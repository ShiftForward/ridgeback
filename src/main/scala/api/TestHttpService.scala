package api

import akka.util.Timeout
import com.wordnik.swagger.annotations._
import persistence.entities.{JsonProtocol, _}
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.routing._
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Api(value = "/test", description = "Operations about tests")
abstract class TestHttpService(modules: Configuration with PersistenceModule) extends HttpService {

  import JsonProtocol._
  import SprayJsonSupport._

  implicit val timeout = Timeout(5.seconds)

  @ApiOperation(httpMethod = "GET", response = classOf[Test], value = "Returns a test based on ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "testId", required = true, dataType = "integer", paramType = "path", value = "ID of test that needs to be fetched")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 404, message = "Not Found")
  ))
  def TestGetRoute = path("test" / IntNumber) { (testId) =>
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
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "query", value = "ID of project that needs tests to be fetched")
  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok")))
  def TestsGetRoute = path("test") {
    parameters('projId.?) { (projIdStr: Option[String]) => {
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

  @ApiOperation(value = "Add Test", nickname = "addTest", httpMethod = "POST", consumes = "application/json", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Test Object", dataType = "persistence.entities.SimpleTest", required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created")
  ))
  def TestPostRoute = path("test") {
    post {
      entity(as[SimpleTest]) {
        testToInsert => onComplete(modules.testsDal.save(Test(None, testToInsert.projId, testToInsert.commit, None /* TODO perhaps define start date */, None))) {
          // ignoring the number of insertedEntities because in this case it should always be one, you might check this in other cases
          case Success(insertedEntities) => complete(StatusCodes.Created)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }
}
