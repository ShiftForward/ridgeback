package api

import entities.JsonProtocol
import persistence.entities.{SimpleProject, Project}
import spray.httpx.SprayJsonSupport
import spray.http._
import StatusCodes._
import scala.concurrent.Future
import JsonProtocol._
import SprayJsonSupport._

class RoutesSpec  extends AbstractAPITest {
  sequential

  def actorRefFactory = system

  val modules = new Modules {}

  val projects = new ProjectHttpService(modules){
    override def actorRefFactory = system
  }

  "Project Routes" should {

    "return an empty array of project" in {
      modules.projectsDal.getProjectById(1) returns Future(Vector())

      Get("/project/1") ~> projects.ProjectGetRoute ~> check { // TODO: change to 404
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Project]].isEmpty
      }
    }

    "return an array with 2 projects" in {
      modules.projectsDal.getProjectById(1) returns Future(Vector(Project(Some(1), "name 1", "url 1"), Project(Some(2), "name 2", "url 2")))
      Get("/project/1") ~> projects.ProjectGetRoute ~> check {
        handled must beTrue
        status mustEqual OK
        responseAs[Seq[Project]].length == 2
      }
    }

    "return an array with 2 projects (2)" in {
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
      Post("/project","{\"name\":\"1\"}") ~> projects.ProjectPostRoute ~> check {
        handled must beFalse
      }
    }

    "not handle an empty post" in {
      Post("/project") ~> projects.ProjectPostRoute ~> check {
        handled must beFalse
      }
    }
  }
}
