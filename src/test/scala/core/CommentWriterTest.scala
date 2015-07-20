package core

import akka.actor._
import api.AbstractAPITest
import org.specs2.time.NoTimeConversions
import persistence.entities.{ Job, Project, PullRequestSource }
import spray.http.HttpResponse
import utils.Configuration

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

object TestCommentWriter extends CommentWriter {
  var called = false

  def apply(prSource: PullRequestSource, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory, ec: ExecutionContext): Future[HttpResponse] = {
    called = true
    Future(new HttpResponse())
  }
}

class CommentWriterTest extends AbstractAPITest with NoTimeConversions {
  sequential

  def actorRefFactory = system

  val modules = new Modules {}

  "A CommentWriterActor" should {
    "write comments" in new AkkaTestkitSpecs2Support {

      val proj = Project(Some(1), "name", "repo")
      val testId = 1
      val prSource = PullRequestSource("tests", "repo", "commit", 1)

      modules.jobsDal.getJobsByTestId(testId) returns Future(Seq(Job(Some(1), proj.id, Some(testId), "job", "source", 1.seconds)))

      val actor = system.actorOf(Props(new CommentWriterActor(modules, TestCommentWriter)))
      actor ! SendComment(proj, testId, prSource)

      TestCommentWriter.called must be_==(true).eventually
    }
  }
}
