package api

import akka.util.Timeout
import com.wordnik.swagger.annotations._
import persistence.entities.{ JsonProtocol, _ }
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.routing._
import utils.{ Configuration, PersistenceModule }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

@Api(value = "/jobs", description = "Operations about jobs")
abstract class JobHttpService(modules: Configuration with PersistenceModule) extends HttpService {

  import JsonProtocol._
  import SprayJsonSupport._

  implicit val timeout = Timeout(5.seconds)
  implicit val ec: ExecutionContext = actorRefFactory.dispatcher

  @ApiOperation(httpMethod = "GET", response = classOf[Job], value = "Returns a job based on ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "jobId", required = true, dataType = "integer", paramType = "path", value = "ID of job that needs to be fetched")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 404, message = "Not Found")))
  def JobGetRoute = path("jobs" / IntNumber) { jobId =>
    get {
      onComplete(modules.jobsDal.getJobById(jobId)) {
        case Success(job) => complete(job)
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }

  @ApiOperation(httpMethod = "GET", response = classOf[Seq[Job]], value = "Returns all jobs, optionally by test OR by project")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "testId", required = false, dataType = "integer", paramType = "query", value = "ID of test that needs jobs to be fetched"),
    new ApiImplicitParam(name = "projId", required = false, dataType = "integer", paramType = "query", value = "ID of project that needs jobs to be fetched")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Ok"),
    new ApiResponse(code = 400, message = "Bad Request")))
  def JobsGetRoute = path("jobs") {
    get {
      parameters('testId.as[Int].?, 'projId.as[Int].?) { (testIdOpt: Option[Int], projIdOpt: Option[Int]) =>
        (testIdOpt, projIdOpt) match {
          case (None, None) =>
            onComplete(modules.jobsDal.getJobs()) {
              case Success(jobs) => complete(jobs)
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          case (Some(testId), None) =>
            onComplete(modules.jobsDal.getJobsByTestId(testId)) {
              case Success(jobs) => complete(jobs)
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          case (None, Some(projId)) =>
            onComplete(modules.jobsDal.getJobsByProjId(projId)) {
              case Success(jobs) => complete(jobs)
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          case (Some(_), Some(_)) => complete(BadRequest)
        }
      }
    }
  }
}
