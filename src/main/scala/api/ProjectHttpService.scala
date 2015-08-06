package api

import javax.ws.rs.Path

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import com.wordnik.swagger.annotations._
import core.{ EventPublisherModule, CloneRepository, Start, WorkerSupervisorActor }
import persistence.entities._
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.routing._
import utils._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

@Api(value = "/projects", description = "Operations about projects")
abstract class ProjectHttpService(modules: Configuration with PersistenceModule with EventPublisherModule) extends HttpService with LazyLogging {

  import JsonProtocol._
  import SprayJsonSupport._

  implicit val timeout = Timeout(15.seconds)
  implicit val ec: ExecutionContext = actorRefFactory.dispatcher

  @ApiOperation(httpMethod = "GET", response = classOf[Project], value = "Returns a project based on ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "path",
      value = "ID of project that needs to be fetched")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 404, message = "Not Found")))
  def ProjectGetRoute = path("projects" / IntNumber) { (projId) =>
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
  def ProjectsGetRoute = path("projects") {
    get {
      respondWithMediaType(`application/json`) {
        onComplete(modules.projectsDal.getProjects()) {
          case Success(projects) => complete(projects)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  @ApiOperation(value = "Add Project", nickname = "addProject", httpMethod = "POST", consumes = "application/json",
    produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Project Object", dataType = "persistence.entities.SimpleProject",
      required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created")))
  def ProjectPostRoute = path("projects") {
    post {
      entity(as[SimpleProject]) {
        projectToInsert =>
          onComplete(modules.projectsDal.save(Project(None, projectToInsert.name, projectToInsert.gitRepo))) {
            case Success(projId) => complete(Created, projId.toString)
            case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
      }
    }
  }

  @Path("/{projId}/trigger")
  @ApiOperation(value = "Trigger Project Build", nickname = "triggerProject", httpMethod = "POST",
    consumes = "text/plain;charset=UTF-8", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "path",
      value = "ID of project that needs to be built"),
    new ApiImplicitParam(name = "body", value = "YAML Definition", dataType = "String", required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 202, message = "Accepted"),
    new ApiResponse(code = 404, message = "Not Found")))
  def ProjectTriggerRoute = path("projects" / IntNumber / "trigger") { projId =>
    post {
      entity(as[String]) { yamlStr =>
        onComplete(modules.projectsDal.getProjectById(projId)) {
          case Success(Some(proj)) =>
            val actor = actorRefFactory.actorOf(Props(new WorkerSupervisorActor(modules, proj, None)))
            onComplete(actor ? Start(yamlStr)) {
              case Success(testId: Int) => complete(Accepted, testId.toString)
              case Success(ex) => complete(InternalServerError, s"Could not create test: $ex")
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          case Success(None) => complete(NotFound)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  @Path("/{projId}/trigger/bb")
  @ApiOperation(value = "Trigger Project Build from Bitbucket", nickname = "triggerProjectBB", httpMethod = "POST",
    consumes = "application/json", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", value = "ID of project that needs to be built", dataType = "integer",
      required = true, paramType = "path"),
    new ApiImplicitParam(name = "body", value = "JSON Payload", dataType = "String", required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 202, message = "Accepted"),
    new ApiResponse(code = 204, message = "No Content"),
    new ApiResponse(code = 404, message = "Not Found"),
    new ApiResponse(code = 501, message = "Not Implemented")))
  def ProjectTriggerRouteBB = path("projects" / IntNumber / "trigger" / "bb") { projId =>
    import core.PayloadJsonProtocol._
    post {
      entity(as[Payload]) {
        case pr: PullRequestPayload if pr.comment.contains(modules.config.getString("worker.keyword")) =>
          onSuccess(modules.projectsDal.getProjectById(projId)) {
            case Some(proj) =>
              val actor = actorRefFactory.actorOf(Props(new WorkerSupervisorActor(modules, proj, Some(pr))))
              actor ! CloneRepository
              complete(Accepted)
            case None => complete(NotFound)
            case ex => complete(InternalServerError, s"An error occurred: $ex")
          }
        case commit: CommitPayload if commit.comment.contains(modules.config.getString("worker.keyword")) =>
          complete(NotImplemented)
        case payload =>
          logger.debug(s"'${payload.comment}' does not match keyword ${modules.config.getString("worker.keyword")}")
          complete(NoContent)
      }
    }
  }
}
