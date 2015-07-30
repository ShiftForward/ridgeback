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
  var message = ""

  def apply(prSource: PullRequestPayload, msg: String, modules: Configuration)(implicit refFactory: ActorRefFactory, ec: ExecutionContext): Future[HttpResponse] = {
    message = msg
    Future(new HttpResponse())
  }

  val actionNew = "[new]"
  val actionBetter = "[better]"
  val actionWorse = "[worse]"
  val actionEqual = "[equal]"
  val actionUnknown = "[unknown]"
}

class CommentWriterSpec extends AbstractAPISpec with NoTimeConversions {

  def actorRefFactory = system

  "A CommentWriterActor" should {
    "write comments" in {
      "new jobs" in {
        val proj = Project(Some(1), "name", "repo")
        val testId = 1
        val jobId = 1
        val prSource = PullRequestPayload("comment", "tests", "repo", "commit", "branch", 1)
        val job1 = Job(Some(jobId), proj.id, Some(testId), "job", "source", List(1.seconds))

        val modules = new Modules {}
        modules.jobsDal.getJobsByTestId(testId) returns Future(Seq(job1))
        modules.jobsDal.getPastJobs(job1) returns Future(Seq())

        val commentWriter = new TestCommentWriter
        val actor = system.actorOf(Props(new CommentWriterActor(modules, commentWriter)))
        actor ! SendComment(proj, testId, prSource)

        commentWriter.message must contain(commentWriter.actionNew).eventually
      }

      "jobs with worse past job" in {
        val proj = Project(Some(2), "name", "repo")
        val testId = 2
        val jobId = 2
        val prSource = PullRequestPayload("comment", "tests", "repo", "commit", "branch", 1)
        val job1 = Job(Some(jobId + 0), proj.id, Some(testId + 0), "job", "source", List(3.seconds))
        val job2 = Job(Some(jobId + 1), proj.id, Some(testId + 1), "job", "source", List(2.seconds))
        val job3 = Job(Some(jobId + 2), proj.id, Some(testId + 2), "job", "source", List(4.seconds))

        val modules = new Modules {}
        modules.jobsDal.getJobsByTestId(testId) returns Future(Seq(job1))
        modules.jobsDal.getPastJobs(job1) returns Future(Seq(job3, job2))

        val commentWriter = new TestCommentWriter
        val actor = system.actorOf(Props(new CommentWriterActor(modules, commentWriter)))
        actor ! SendComment(proj, testId, prSource)

        commentWriter.message must contain(commentWriter.actionBetter).eventually
      }

      "jobs with best past job" in {
        val proj = Project(Some(3), "name", "repo")
        val testId = 5
        val jobId = 5
        val prSource = PullRequestPayload("comment", "tests", "repo", "commit", "branch", 1)

        val job1 = Job(Some(jobId + 0), proj.id, Some(testId + 0), "job", "source", List(3.seconds))
        val job2 = Job(Some(jobId + 1), proj.id, Some(testId + 1), "job", "source", List(4.seconds))
        val job3 = Job(Some(jobId + 2), proj.id, Some(testId + 2), "job", "source", List(2.seconds))

        val modules = new Modules {}
        modules.jobsDal.getJobsByTestId(testId) returns Future(Seq(job1))
        modules.jobsDal.getPastJobs(job1) returns Future(Seq(job3, job2))

        val commentWriter = new TestCommentWriter
        val actor = system.actorOf(Props(new CommentWriterActor(modules, commentWriter)))
        actor ! SendComment(proj, testId, prSource)

        commentWriter.message must contain(commentWriter.actionWorse).eventually
      }
    }
  }
}
