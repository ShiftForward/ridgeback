package api

import java.time.ZonedDateTime

import persistence.entities.JsonProtocol._
import persistence.entities.{ JsonProtocol, Project, SimpleProject, Test }
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.httpx.SprayJsonSupport._

import scala.concurrent.Future

class RoutesSpec extends AbstractAPITest {
  sequential

  def actorRefFactory = system

  val modules = new Modules {}

  val projects = new ProjectHttpService(modules) {
    override def actorRefFactory = system
  }

  "Project Routes" should {

    "return 404" in {
      modules.projectsDal.getProjectById(1) returns Future(None)

      Get("/project/1") ~> projects.ProjectGetRoute ~> check {
        handled must beTrue
        status mustEqual NotFound
      }
    }

    "return 1 project" in {
      modules.projectsDal.getProjectById(1) returns Future(Some(Project(Some(1), "name 1", "url 1")))
      Get("/project/1") ~> projects.ProjectGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Option[Project]].isDefined
      }
    }

    "return an array with 2 projects" in {
      modules.projectsDal.getProjects() returns Future(Vector(Project(Some(1), "name 1", "url 1"), Project(Some(2), "name 2", "url 2")))
      Get("/project") ~> projects.ProjectsGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Project]].length == 2
      }
    }

    "create a project with the json in post" in {
      modules.projectsDal.save(Project(None, "name 1", "url 1")) returns Future(1)
      Post("/project", SimpleProject("name 1", "url 1")) ~> projects.ProjectPostRoute ~> check {
        handled must beTrue
        status mustEqual Created
      }
    }

    "not handle the invalid json" in {
      Post("/project", "{\"name\":\"1\"}") ~> projects.ProjectPostRoute ~> check {
        handled must beFalse
      }
    }

    "not handle an empty post" in {
      Post("/project") ~> projects.ProjectPostRoute ~> check {
        handled must beFalse
      }
    }

    "trigger route returns 404" in {
      modules.projectsDal.getProjectById(99) returns Future(None)
      Post("/project/99/trigger") ~> projects.ProjectTriggerRoute ~> check {
        handled must beTrue
        status mustEqual NotFound
      }
    }

    "trigger route runs successful" in {
      modules.projectsDal.getProjectById(2) returns Future(Some(Project(Some(2), "name 1", "url 1")))
      modules.testsDal.save(Test(None, Some(2), "commit", Some(any[ZonedDateTime]), None)) returns Future(3)

      Post("/project/2/trigger") ~> projects.ProjectTriggerRoute ~> check {
        handled must beTrue
        status mustEqual Created
        responseAs[String] mustEqual "3"
      }
    }
  }
}
