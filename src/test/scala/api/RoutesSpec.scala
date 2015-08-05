package api

import java.net.URLDecoder
import java.time.ZonedDateTime

import persistence.entities.JsonProtocol._
import persistence.entities.{ SimpleProject, Project, Test }
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

import scala.concurrent.Future
import scala.io.Source

class RoutesSpec extends AbstractAPISpec {
  sequential

  def actorRefFactory = system

  val modules = new Modules {}

  val projects = new ProjectHttpService(modules) {
    override def actorRefFactory = system
  }

  "Project Routes" should {

    "return 404" in {
      modules.projectsDal.getProjectById(1) returns Future(None)

      Get("/projects/1") ~> projects.ProjectGetRoute ~> check {
        handled must beTrue
        status mustEqual NotFound
      }
    }

    "return 1 project" in {
      modules.projectsDal.getProjectById(1) returns Future(Some(Project(Some(1), "name 1", "url 1")))
      Get("/projects/1") ~> projects.ProjectGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Option[Project]].isDefined
      }
    }

    "return an array with 2 projects" in {
      modules.projectsDal.getProjects() returns Future(Vector(Project(Some(1), "name 1", "url 1"), Project(Some(2), "name 2", "url 2")))
      Get("/projects") ~> projects.ProjectsGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Project]].length == 2
      }
    }

    "create a project with the json in post" in {
      modules.projectsDal.save(Project(None, "name 1", "url 1")) returns Future(2)
      Post("/projects", SimpleProject("name 1", "url 1")) ~> projects.ProjectPostRoute ~> check {
        handled must beTrue
        status mustEqual Created
        responseAs[String] == "2"
      }
    }

    "not handle the invalid json" in {
      Post("/projects", "{\"name\":\"1\"}") ~> projects.ProjectPostRoute ~> check {
        handled must beFalse
      }
    }

    "not handle an empty post" in {
      Post("/projects") ~> projects.ProjectPostRoute ~> check {
        handled must beFalse
      }
    }

    "trigger route returns 404" in {
      modules.projectsDal.getProjectById(99) returns Future(None)
      Post("/projects/99/trigger") ~> projects.ProjectTriggerRoute ~> check {
        handled must beTrue
        status mustEqual NotFound
      }
    }

    "trigger route runs successfully" in {
      modules.projectsDal.getProjectById(2) returns Future(Some(Project(Some(2), "name 1", "url 1")))
      modules.testsDal.save(Test(None, Some(2), "commit", None, None, None, Some(any[ZonedDateTime]), None)) returns Future(3)

      Post("/projects/2/trigger") ~> projects.ProjectTriggerRoute ~> check {
        handled must beTrue
        status mustEqual Accepted
        responseAs[String] mustEqual "3"
      }
    }

    "bitbucket trigger route runs successfully with PRs" in {
      modules.projectsDal.getProjectById(1) returns Future(Some(Project(Some(1), "ridgeback", "git@bitbucket.org:shiftforward/ridgeback.git")))

      import spray.json._
      import spray.json.DefaultJsonProtocol._

      val file = getResourceURL("/bitbucket_pr_comment.json")
      val lines = Source.fromFile(file).mkString.replace("REPLACEME", modules.config.getString("worker.keyword"))

      Post("/projects/1/trigger/bb", lines.parseJson.asJsObject) ~> projects.ProjectTriggerRouteBB ~> check {
        handled must beTrue
        status mustEqual Accepted
      }
    }

    "bitbucket trigger route doesn't do anything if the comment doesn't matter" in {
      modules.projectsDal.getProjectById(1) returns Future(Some(Project(Some(1), "ridgeback", "git@bitbucket.org:shiftforward/ridgeback.git")))

      import spray.json._
      import spray.json.DefaultJsonProtocol._

      val file = getResourceURL("/bitbucket_pr_comment.json")
      val lines = Source.fromFile(file).mkString.replace("REPLACEME", "some random comment")

      Post("/projects/1/trigger/bb", lines.parseJson.asJsObject) ~> projects.ProjectTriggerRouteBB ~> check {
        handled must beTrue
        status mustEqual NoContent
      }
    }

    "bitbucket trigger route doesn't run yet with commits" in {
      modules.projectsDal.getProjectById(1) returns Future(Some(Project(Some(1), "ridgeback", "git@bitbucket.org:shiftforward/ridgeback.git")))

      import spray.json._
      import spray.json.DefaultJsonProtocol._

      val file = getResourceURL("/bitbucket_commit_comment.json")
      val lines = Source.fromFile(file).mkString.replace("REPLACEME", modules.config.getString("worker.keyword"))

      Post("/projects/1/trigger/bb", lines.parseJson.asJsObject) ~> projects.ProjectTriggerRouteBB ~> check {
        handled must beTrue
        status mustEqual NotImplemented
      }
    }
  }

  def getResourceURL(resource: String): String =
    URLDecoder.decode(getClass.getResource(resource).getFile, "UTF-8")
}
