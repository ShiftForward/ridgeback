package core

import akka.actor.{ Actor, ActorRefFactory }
import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ Project, PullRequestPayload }
import spray.client.pipelining._
import spray.http.{ BasicHttpCredentials, HttpRequest, _ }
import utils.{ Configuration, PersistenceModule }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

case class SendComment(proj: Project, testId: Int, prSource: PullRequestPayload)

trait CommentWriter {
  def apply(prSource: PullRequestPayload, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory,
                                                                               ec: ExecutionContext): Future[HttpResponse]
}

class CommentWriterActor(modules: Configuration with PersistenceModule, commentWriter: CommentWriter) extends Actor with LazyLogging {
  import context.dispatcher

  def receive: Receive = {
    case SendComment(proj, testId, prSource) =>
      modules.jobsDal.getJobsByTestId(testId) onComplete {
        case Success(jobs) =>
          val strBuilder = new StringBuilder
          jobs.foreach { job =>
            job.durations match {
              case ds if ds.length == 1 => strBuilder.append(s"- Job ${job.jobName} (${job.id.get}) took ${job.durations.head}\n\n")
              case ds if ds.isEmpty => strBuilder.append(s"- Job ${job.jobName} (${job.id.get}) had no output\n\n")
              case ds =>
                val avg = Duration((job.durations.map(_.toMillis).sum / job.durations.length), MILLISECONDS).toCoarsest
                strBuilder.append(s"- Job ${job.jobName} (${job.id.get}) took in average $avg (min: ${ds.min}, max: ${ds.max})\n\n")
            }
          }

          val response = commentWriter(prSource, strBuilder.toString(), modules)

          response onComplete {
            case Success(res: HttpResponse) =>
              if (res.status.isSuccess)
                logger.info(s"Write comment on ${prSource.repoFullName}#${prSource.pullRequestId} status ${res.status}")
              else
                logger.error(s"Write comment on ${prSource.repoFullName}#${prSource.pullRequestId} status ${res.status}")
              context.stop(self)
            case Failure(error) =>
              logger.error(s"Write comment on ${prSource.repoFullName}#${prSource.pullRequestId} failed", error)
              context.stop(self)
          }

        case Failure(error) =>
          logger.error(s"Failed to get jobs of ${prSource.repoFullName}#${prSource.pullRequestId}", error)
          context.stop(self)
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
}
