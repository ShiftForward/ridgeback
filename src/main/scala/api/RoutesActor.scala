package api

import akka.actor.Actor
import akka.util.Timeout
import com.gettyimages.spray.swagger._
import com.typesafe.scalalogging.LazyLogging
import com.wordnik.swagger.annotations._
import com.wordnik.swagger.model.ApiInfo
import entities.JsonProtocol
import persistence.entities._
import spray.http.MediaTypes._
import spray.http.StatusCodes._
import spray.http._
import spray.httpx.SprayJsonSupport
import spray.routing._
import utils.{Configuration, PersistenceModule}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.reflect.runtime.universe._
import scala.util.{Failure, Success}

class RoutesActor(modules: Configuration with PersistenceModule) extends Actor with HttpService with LazyLogging {

  def actorRefFactory = context

  implicit val timeout = Timeout(5.seconds)

  // create table for projects if the table didn't exist (should be removed, when the database wasn't h2)
  modules.projectsDal.createTables()

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[ProjectHttpService])
    override def apiVersion = "2.0"
    override def baseUrl = "/"
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    override def apiInfo = Some(new ApiInfo("Ridgeback", " API Documentation", "No TOC", "dnpd.dd@gmail.com", "MIT", "http://opensource.org/licenses/MIT"))
  }

  val projects = new ProjectHttpService(modules){
    def actorRefFactory = context
  }

  def receive = runRoute(projects.ProjectPostRoute ~ projects.ProjectGetRoute ~ projects.ProjectsGetRoute ~ swaggerService.routes ~
    get {
      pathPrefix("") { pathEndOrSingleSlash {
        getFromResource("swagger-ui/index.html")
      }
      } ~
        getFromResourceDirectory("swagger-ui")
    })
}

@Api(value = "/project", description = "Operations about projects")
abstract class ProjectHttpService(modules: Configuration with PersistenceModule) extends HttpService {

  import JsonProtocol._
  import SprayJsonSupport._

  implicit val timeout = Timeout(5.seconds)

  @ApiOperation(httpMethod = "GET", response = classOf[Project], value = "Returns a project based on ID")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "projId", required = true, dataType = "integer", paramType = "path", value = "ID of project that needs to be fetched")
  ))
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok")))
  def ProjectGetRoute = path("project" / IntNumber) { (projId) =>
    get {
      respondWithMediaType(`application/json`) {
        onComplete(modules.projectsDal.getProjectById(projId).mapTo[Vector[Project]]) {
          case Success(projects) => complete(projects)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }}

  @ApiOperation(httpMethod = "GET", response = classOf[Project], value = "Returns all projects")
  @ApiResponses(Array(new ApiResponse(code = 200, message = "Ok")))
  def ProjectsGetRoute = path("project") {
    get {
      respondWithMediaType(`application/json`) {
        onComplete(modules.projectsDal.getProjects().mapTo[Vector[Project]]) {
          case Success(projects) => complete(projects)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }}

  @ApiOperation(value = "Add Project", nickname = "addProject", httpMethod = "POST", consumes = "application/json", produces = "text/plain; charset=UTF-8")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", value = "Project Object", dataType = "persistence.entities.SimpleProject", required = true, paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 201, message = "Entity Created")
  ))
  def ProjectPostRoute = path("project"){
    post {
      entity(as[SimpleProject]){ projectToInsert =>  onComplete(modules.projectsDal.save(Project(None, projectToInsert.name, projectToInsert.gitRepo))) {
        // ignoring the number of insertedEntities because in this case it should always be one, you might check this in other cases
        case Success(insertedEntities) => complete(StatusCodes.Created)
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
      }
    }
  }
}
