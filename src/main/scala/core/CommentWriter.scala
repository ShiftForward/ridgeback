package core

import akka.actor.{ Actor, ActorRefFactory, PoisonPill }
import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ TestsConfiguration, Project, PullRequestPayload }
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

class CommentWriterActor(modules: Configuration with PersistenceModule,
                         commentWriter: CommentWriter,
                         testConfig: Option[TestsConfiguration]) extends Actor with LazyLogging {
  import context.dispatcher

  def receive: Receive = {
    case SendComment(proj, testId, prSource) =>

      val response = for {
        jobs <- modules.jobsDal.getJobsByTestId(testId)
        jobComment <- Future.sequence(jobs.map(CommentBuilder.buildComment(_, commentWriter, modules, testConfig.flatMap(_.commentTemplate)))).map(_.mkString("\n\n"))
        response <- commentWriter(prSource, jobComment, modules)
      } yield response

      val name = s"${prSource.repoFullName}#${prSource.pullRequestId}"
      response onComplete {
        case Success(res) =>
          if (res.status.isSuccess)
            logger.info(s"Write comment on $name status ${res.status}")
          else
            logger.error(s"Write comment on $name status ${res.status}")
          self ! PoisonPill
        case Failure(error) =>
          logger.error(s"Failed to send comment for $name", error)
          self ! PoisonPill
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
  val actionUnknown = ":grey_question:"
}
