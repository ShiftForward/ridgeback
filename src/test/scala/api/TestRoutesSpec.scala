package api

import persistence.entities.JsonProtocol._
import persistence.entities.Test
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.Future

class TestRoutesSpec extends AbstractAPISpec {
  sequential

  def actorRefFactory = system

  val modules = new Modules {}

  val tests = new TestHttpService(modules) {
    override def actorRefFactory = system
  }

  "Test Routes" should {

    "return 404" in {
      modules.testsDal.getTestById(1) returns Future(None)

      Get("/tests/1") ~> tests.TestGetRoute ~> check {
        handled must beTrue
        status mustEqual NotFound
      }
    }

    "return 1 test" in {
      modules.testsDal.getTestById(1) returns Future(Some(Test(Some(1), Some(1), "commit", Some("branch"), None, Some("dir"), None, None)))
      Get("/tests/1") ~> tests.TestGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Option[Test]].isDefined
      }
    }

    "return an array with 2 tests" in {
      modules.testsDal.getTests() returns Future(Seq(
        Test(Some(1), Some(1), "commit", Some("branch"), None, Some("dir"), None, None),
        Test(Some(2), Some(2), "commit", Some("branch"), None, Some("dir"), None, None)))
      Get("/tests") ~> tests.TestsGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Test]].length == 2
      }
    }

    "return a filtered array with 1 test" in {
      modules.testsDal.getTestsByProjId(2) returns Future(Seq(Test(Some(3), Some(2), "commit", Some("branch"), None, Some("dir"), None, None)))
      Get("/tests?projId=2") ~> tests.TestsGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Test]].length == 1
      }
    }

  }
}
