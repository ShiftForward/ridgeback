package core

import akka.actor.{ PoisonPill, Actor, ActorRefFactory }
import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ Job, Project, PullRequestPayload }
import spray.client.pipelining._
import spray.http.{ BasicHttpCredentials, HttpRequest, _ }
import utils._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

case class SendComment(proj: Project, testId: Int, prSource: PullRequestPayload)

trait CommentWriter {
  def apply(prSource: PullRequestPayload, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory,
                                                                               ec: ExecutionContext): Future[HttpResponse]

  def actionNew: String
  def actionBetter: String
  def actionWorse: String
  def actionEqual: String
  def actionUnknown: String
}

class CommentWriterActor(modules: Configuration with PersistenceModule, commentWriter: CommentWriter) extends Actor with LazyLogging {
  import context.dispatcher

  def receive: Receive = {
    case SendComment(proj, testId, prSource) =>

      val response = for {
        jobs <- modules.jobsDal.getJobsByTestId(testId)
        jobComment <- Future.sequence(jobs.map(buildComment(_, commentWriter))).map(_.mkString("\n\n"))
        response <- commentWriter(prSource, jobComment, modules)
      } yield response

      response onComplete {
        case Success(res) =>
          val name = s"${prSource.repoFullName}#${prSource.pullRequestId}"
          if (res.status.isSuccess)
            logger.info(s"Write comment on $name status ${res.status}")
          else
            logger.error(s"Write comment on $name status ${res.status}")
          self ! PoisonPill
        case Failure(error) =>
          logger.error(s"Failed to send comment for ${prSource.repoFullName}#${prSource.pullRequestId}", error)
          self ! PoisonPill
      }
  }

  def buildComment(job: Job, commentWriter: CommentWriter): Future[String] = {
    import utils.RichDuration._

    val description = s"${job.jobName} (${job.id.get})"
    job.durations match {
      case ds if ds.isEmpty => Future(s"- ${commentWriter.actionUnknown} Job $description had no output")
      case ds =>
        modules.jobsDal.getPastJobs(job).map { pastJobs =>
          val pastMeanOpt = pastJobs.headOption
            .map(j => MeanDurationStat(j.durations)).getOrElse(None)

          val min = MinDurationStat(ds).get
          val max = MaxDurationStat(ds).get
          val mean = MeanDurationStat(ds).get

          pastMeanOpt match {
            case Some(pastMean) =>
              val defaultThreshold = modules.config.getInt("worker.defaultThreshold")
              val action = pastMean.compareThresh(mean, job.threshold.getOrElse(defaultThreshold)) match {
                case c if c < 0 => commentWriter.actionWorse
                case c if c > 0 => commentWriter.actionBetter
                case _ => commentWriter.actionEqual
              }
              s"- $action Job $description took in average ${mean.toShortString} [${min.toShortString}, ${max.toShortString}] (before ${pastMeanOpt.get.toShortString})"
            case None => s"- ${commentWriter.actionNew} Job $description took in average ${mean.toShortString} [${min.toShortString}, ${max.toShortString}]"
          }
        }
    }
  }
}

object BitbucketCommentWriter extends CommentWriter {
  def apply(prSource: PullRequestPayload, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory, ec: ExecutionContext): Future[HttpResponse] = {
    val url = s"https://bitbucket.org/api/1.0/repositories/${prSource.repoFullName}/pullrequests/${prSource.pullRequestId}/comments"

    val pipeline: HttpRequest => Future[HttpResponse] = (
      addCredentials(BasicHttpCredentials(modules.config.getString("bitbucket.user"), modules.config.getString("bitbucket.pass")))
      ~> sendReceive)

    pipeline(Post(url, FormData(Seq("content" -> msg))))
  }

  val actionNew = ":new:"
  val actionBetter = ":green_heart:"
  val actionWorse = ":broken_heart:"
  val actionEqual = ":blue_heart:"
  val actionUnknown = ":confused:"
}
