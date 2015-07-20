package core

import akka.actor.{ ActorRefFactory, PoisonPill, Actor }
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ Project, PullRequestSource }
import spray.client.pipelining._
import spray.http.{ BasicHttpCredentials, HttpRequest, _ }
import utils.{ Configuration, PersistenceModule }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

case class SendComment(proj: Project, testId: Int, prSource: PullRequestSource)

trait CommentWriter {
  def apply(prSource: PullRequestSource, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory, ec: ExecutionContext): Future[HttpResponse]
}

class CommentWriterActor(modules: Configuration with PersistenceModule, commentWriter: CommentWriter) extends Actor with LazyLogging {
  import context.dispatcher

  def receive: Receive = {
    case SendComment(proj, testId, prSource) =>
      modules.jobsDal.getJobsByTestId(testId) onComplete {
        case Success(jobs) =>
          val strBuilder = new StringBuilder
          jobs.foreach { job =>
            strBuilder.append(s"- Job ${job.jobName} (${job.id.get}) took ${job.duration}\n\n")
          }

          writeComment(prSource, strBuilder.toString())
          self ! PoisonPill

        case Failure(error) =>
          logger.error(s"Failed to get jobs of ${prSource.repoFullName}#${prSource.pullRequestId}", error)
          self ! PoisonPill
      }
  }

  def writeComment(prSource: PullRequestSource, msg: String): Unit = {

    val response = commentWriter(prSource, msg, modules)

    response onComplete {
      case Success(res: HttpResponse) =>
        if (res.status.isSuccess)
          logger.info(s"Write comment on ${prSource.repoFullName}#${prSource.pullRequestId} status ${res.status}")
        else
          logger.error(s"Write comment on ${prSource.repoFullName}#${prSource.pullRequestId} status ${res.status}")
      case Failure(error) =>
        logger.error(s"Write comment on ${prSource.repoFullName}#${prSource.pullRequestId} failed", error)
    }
  }
}

object BitbucketCommentWriter extends CommentWriter {
  def apply(prSource: PullRequestSource, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory, ec: ExecutionContext): Future[HttpResponse] = {
    val url = s"https://bitbucket.org/api/1.0/repositories/${prSource.repoFullName}/pullrequests/${prSource.pullRequestId}/comments"

    val pipeline: HttpRequest => Future[HttpResponse] = (
      addCredentials(BasicHttpCredentials(modules.config.getString("bitbucket.user"), modules.config.getString("bitbucket.pass")))
      ~> sendReceive)

    pipeline(Post(url, FormData(Seq("content" -> msg))))
  }
}
