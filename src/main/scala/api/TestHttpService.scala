package api

import javax.ws.rs.Path

import akka.util.Timeout
import com.wordnik.swagger.annotations._
import core.EventPublisherModule
import persistence.entities.{ JsonProtocol, _ }
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.routing._
import utils.{ Configuration, PersistenceModule }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

@Api(value = "/tests", description = "Operations about tests")
abstract class TestHttpService(modules: Configuration with PersistenceModule with EventPublisherModule) extends HttpService {

  import JsonProtocol._
  import SprayJsonSupport._

  implicit val timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = actorRefFactory.dispatcher

  @ApiOperation(httpMethod = "GET", response = classOf[Test], value = "Returns a test based on ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "testId", required = true, dataType = "integer", paramType = "path", value = "ID of test that needs to be fetched")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 404, message = "Not Found")))
  def TestGetRoute = path("tests" / IntNumber) { testId =>
    get {
      respondWithMediaType(`application/json`) {
        onComplete(modules.testsDal.getTestById(testId)) {
          case Success(test) => complete(test)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  @ApiOperation(httpMethod = "GET", response = classOf[Seq[Test]], value = "Returns all tests, optionally by project")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = false, dataType = "integer", paramType = "query", value = "ID of project that needs tests to be fetched")))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok")))
  def TestsGetRoute = path("tests") {
    parameters('projId.?) { (projIdStr: Option[String]) =>
      {
        get {
          respondWithMediaType(`application/json`) {
            if (projIdStr.isEmpty) {
              onComplete(modules.testsDal.getTests()) {
                case Success(tests) => complete(tests)
                case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
            } else {
              val projId = projIdStr.get.toInt
              onComplete(modules.testsDal.getTestsByProjId(projId)) {
                case Success(tests) => complete(tests)
                case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
            }
          }
        }
      }
    }
  }

  @Path("/{testId}/events")
  @ApiOperation(httpMethod = "GET", response = classOf[Seq[(String, String)]], value = "Returns events of a test")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "testId", required = true, dataType = "integer", paramType = "path", value = "ID of test that needs to be fetched")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 404, message = "Not Found")))
  def TestEventsGetRoute = path("tests" / IntNumber / "events") { testId =>
    get {
      respondWithMediaType(`application/json`) {
        onComplete(modules.testsDal.getTestById(testId)) {
          case Success(Some(test)) =>
            onComplete(modules.projectsDal.getProjectById(test.projId.getOrElse(0))) {
              case Success(Some(proj)) => complete(modules.getEvents(proj, testId))
              case Success(None) => complete(NotFound, test.projId.getOrElse(0).toString)
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          case Success(None) => complete(NotFound, testId.toString)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }
}
