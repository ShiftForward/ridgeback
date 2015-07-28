package core

import akka.actor._
import api.AbstractAPISpec
import org.specs2.time.NoTimeConversions
import persistence.entities.{ PullRequestPayload, Job, Project }
import spray.http.HttpResponse
import utils.Configuration

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

class TestCommentWriter extends CommentWriter {
  var called = false

  def apply(prSource: PullRequestPayload, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory, ec: ExecutionContext): Future[HttpResponse] = {
    called = true
    Future(new HttpResponse())
  }
}

class CommentWriterSpec extends AbstractAPISpec with NoTimeConversions {
  sequential

  def actorRefFactory = system

  val modules = new Modules {}

  "A CommentWriterActor" should {
    "write comments" in new AkkaTestkitSpecs2Support {

      val proj = Project(Some(1), "name", "repo")
      val testId = 1
      val prSource = PullRequestPayload("comment", "tests", "repo", "commit", "branch", 1)
      val commentWriter = new TestCommentWriter

      modules.jobsDal.getJobsByTestId(testId) returns Future(Seq(Job(Some(1), proj.id, Some(testId), "job", "source", 1.seconds)))

      val actor = system.actorOf(Props(new CommentWriterActor(modules, commentWriter)))
      actor ! SendComment(proj, testId, prSource)

      commentWriter.called must be_==(true).eventually
    }
  }
}
