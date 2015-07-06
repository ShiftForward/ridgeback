package api

import akka.actor.Actor
import akka.util.Timeout
import com.gettyimages.spray.swagger._
import com.typesafe.scalalogging.LazyLogging
import com.wordnik.swagger.model.ApiInfo
import spray.routing._
import utils.{Configuration, PersistenceModule}

import scala.concurrent.duration._
import scala.reflect.runtime.universe._

class RoutesActor(modules: Configuration with PersistenceModule) extends Actor with HttpService with LazyLogging {

  def actorRefFactory = context

  implicit val timeout = Timeout(5.seconds)

  // create table for projects if the table didn't exist (should be removed, when the database wasn't h2)
  modules.projectsDal.createTables()

  val swaggerService = new SwaggerHttpService {
    override def apiTypes = Seq(typeOf[ProjectHttpService], typeOf[TestHttpService])

    override def apiVersion = "2.0"

    override def baseUrl = "/"

    override def docsPath = "api-docs"

    override def actorRefFactory = context

    override def apiInfo = Some(new ApiInfo("Ridgeback", " API Documentation", "No TOC", "dnpd.dd@gmail.com", "MIT", "http://opensource.org/licenses/MIT"))
  }

  val projects = new ProjectHttpService(modules) {
    def actorRefFactory = context
  }

  val tests = new TestHttpService(modules) {
    def actorRefFactory = context
  }

  def receive = runRoute(projects.ProjectPostRoute ~ projects.ProjectGetRoute ~ projects.ProjectsGetRoute ~
                         tests.TestGetRoute ~ tests.TestsGetRoute ~ tests.TestPostRoute ~
                         swaggerService.routes ~
    get {
      pathPrefix("") {
        pathEndOrSingleSlash {
          getFromResource("swagger-ui/index.html")
        }
      } ~
        getFromResourceDirectory("swagger-ui")
    })
}
