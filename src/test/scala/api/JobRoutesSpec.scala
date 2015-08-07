package api

import persistence.entities.JsonProtocol._
import persistence.entities.{ Job, Test }
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.Future

class JobRoutesSpec extends AbstractAPISpec {
  sequential

  def actorRefFactory = system

  val modules = new Modules {}

  val jobs = new JobHttpService(modules) {
    override def actorRefFactory = system
  }

  "Job Routes" should {

    "return 404" in {
      modules.jobsDal.getJobById(1) returns Future(None)

      Get("/jobs/1") ~> jobs.JobGetRoute ~> check {
        handled must beTrue
        status mustEqual NotFound
      }
    }

    "return 1 job" in {
      modules.jobsDal.getJobById(1) returns Future(Some(Job(Some(1), Some(1), Some(1), "name", "source", None, List())))
      Get("/jobs/1") ~> jobs.JobGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Option[Job]].isDefined
      }
    }

    "return an array with 2 jobs" in {
      modules.jobsDal.getJobs() returns Future(Seq(
        Job(Some(1), Some(1), Some(1), "name", "source", None, List()),
        Job(Some(2), Some(2), Some(2), "name", "source", None, List())))
      Get("/jobs") ~> jobs.JobsGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Job]].length == 2
      }
    }

    "return a filtered array by test with 1 job" in {
      modules.jobsDal.getJobsByTestId(3) returns Future(Seq(Job(Some(3), Some(3), Some(3), "name", "source", None, List())))
      Get("/jobs?testId=3") ~> jobs.JobsGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Job]].length == 1
      }
    }

    "return a filtered array by test with 1 job" in {
      modules.jobsDal.getJobsByProjId(4) returns Future(Seq(Job(Some(4), Some(4), Some(4), "name", "source", None, List())))
      Get("/jobs?projId=4") ~> jobs.JobsGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Job]].length == 1
      }
    }
  }
}
