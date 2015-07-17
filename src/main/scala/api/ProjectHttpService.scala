package api

import javax.ws.rs.Path

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.wordnik.swagger.annotations._
import core.{ CloneRepository, Start, WorkerSupervisorActor }
import persistence.entities.{ JsonProtocol, _ }
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.json.{ JsString, JsObject }
import spray.routing._
import utils._
import utils.json.Implicits._

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
            case Success(insertedEntities) => complete(Created)
            case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
      }
    }
  }

  @Path("/{projId}/trigger")
  @ApiOperation(value = "Trigger Project Build", nickname = "triggerProject", httpMethod = "POST", consumes = "text/plain; charset=UTF-8", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "path", value = "ID of project that needs to be built"),
    new ApiImplicitParam(name = "body", value = "YAML Definition", dataType = "String", required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 202, message = "Accepted"),
    new ApiResponse(code = 404, message = "Not Found")))
  def ProjectTriggerRoute = path("project" / IntNumber / "trigger") { (projId) =>
    post {
      entity(as[String]) { yamlStr =>
        onComplete(modules.projectsDal.getProjectById(projId)) {
          case Success(Some(proj)) =>
            val actor = actorRefFactory.actorOf(Props(new WorkerSupervisorActor(modules)))
            onComplete(actor ? Start(yamlStr, proj)) {
              case Success(Some(testId)) => complete(Accepted, testId.toString)
              case Success(_) => complete(InternalServerError, "Could not create test")
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          case Success(None) => complete(NotFound)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  @Path("/{projId}/trigger/bb")
  @ApiOperation(value = "Trigger Project Build from Bitbucket", nickname = "triggerProjectBB", httpMethod = "POST", consumes = "application/json", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "path", value = "ID of project that needs to be built"),
    new ApiImplicitParam(name = "body", value = "JSON Payload", dataType = "JsObject", required = true, paramType = "body")))
  @ApiResponses(Array(
    new ApiResponse(code = 202, message = "Accepted"),
    new ApiResponse(code = 404, message = "Not Found")))
  def ProjectTriggerRouteBB = path("project" / IntNumber / "trigger" / "bb") { (projId) =>
    post {
      entity(as[JsObject]) { (json: JsObject) =>
        json.getPath[JsString]("comment.content.raw") match {
          case Some(comment) if comment.value.contains("PERFTESTS") =>
            val commitPrOpt = json.getPath[JsString]("pullrequest.source.commit.hash")
            val branchPrOpt = json.getPath[JsString]("pullrequest.source.branch.name")
            val commitOpt = json.getPath[JsString]("commit.hash")
            val repoNameOpt = json.getPath[JsString]("repository.full_name")

            (commitPrOpt, commitOpt, branchPrOpt, repoNameOpt) match {
              case (Some(commit), None, Some(branch), Some(repoName)) => // PR comment hook

                onSuccess(modules.projectsDal.getProjectById(projId)) {
                  case Some(proj) =>
                    val actor = actorRefFactory.actorOf(Props(new WorkerSupervisorActor(modules)))
                    actor ! CloneRepository(commit.value, proj)
                    complete(Accepted)
                  case None => complete(NotFound, s"Repository $gitUrl not found")
                  case ex => complete(InternalServerError, s"An error occurred: $ex")
                }
              case (None, Some(commit), None, Some(repoName)) => ??? // commit comment hook
              case _ => complete(NoContent)
            }
          case _ => complete(NoContent)
        }
      }
    }
  }
}
