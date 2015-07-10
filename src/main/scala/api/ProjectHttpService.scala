package api

import javax.ws.rs.Path

import akka.actor.Props
import akka.util.Timeout
import com.wordnik.swagger.annotations._
import core.{ WorkerSupervisorActor, Run, TestRunnerActor }
import persistence.entities.{ JsonProtocol, _ }
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.routing._
import utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

@Api(value = "/project", description = "Operations about projects")
abstract class ProjectHttpService(modules: Configuration with PersistenceModule) extends HttpService {

  import JsonProtocol._
  import SprayJsonSupport._

  implicit val timeout = Timeout(5.seconds)

  @ApiOperation(httpMethod = "GET", response = classOf[Project], value = "Returns a project based on ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "path", value = "ID of project that needs to be fetched")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 404, message = "Not Found")))
  def ProjectGetRoute = path("project" / IntNumber) { (projId) =>
    get {
      respondWithMediaType(`application/json`) {
        onComplete(modules.projectsDal.getProjectById(projId)) {
          case Success(project) => complete(project)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  @ApiOperation(httpMethod = "GET", response = classOf[Seq[Project]], value = "Returns all projects")
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok")))
  def ProjectsGetRoute = path("project") {
    get {
      respondWithMediaType(`application/json`) {
        onComplete(modules.projectsDal.getProjects()) {
          case Success(projects) => complete(projects)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  @ApiOperation(value = "Add Project", nickname = "addProject", httpMethod = "POST", consumes = "application/json", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Project Object", dataType = "persistence.entities.SimpleProject", required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created")))
  def ProjectPostRoute = path("project") {
    post {
      entity(as[SimpleProject]) {
        projectToInsert =>
          onComplete(modules.projectsDal.save(Project(None, projectToInsert.name, projectToInsert.gitRepo))) {
            // ignoring the number of insertedEntities because in this case it should always be one, you might check this in other cases
            case Success(insertedEntities) => complete(StatusCodes.Created)
            case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
      }
    }
  }

  @Path("/{projId}/trigger")
  @ApiOperation(value = "Trigger Project Build", nickname = "triggerProject", httpMethod = "POST", consumes = "text/plain; charset=UTF-8", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "path", value = "ID of project that needs to be fetched"),
    new ApiImplicitParam(name = "body", value = "YAML Definition", dataType = "String", required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 404, message = "Not Found")))
  def ProjectTriggerRoute = path("project" / IntNumber / "trigger") { (projId) =>
    post {
      entity(as[String]) {
        yamlStr =>
          actorRefFactory.actorOf(Props(new WorkerSupervisorActor(modules))) ! Run(yamlStr)
          complete(StatusCodes.Created) // TODO: return test id
      }
    }
  }
}
