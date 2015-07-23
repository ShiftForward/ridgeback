package api

import akka.actor.Actor
import akka.util.Timeout
import com.gettyimages.spray.swagger._
import com.typesafe.scalalogging.LazyLogging
import com.wordnik.swagger.model.ApiInfo
import core.EventPublisherModule
import slick.jdbc.meta.MTable
import spray.routing._
import utils.{ DbModule, Configuration, PersistenceModule }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.reflect.runtime.universe._

class RoutesActor(modules: Configuration with PersistenceModule with DbModule with EventPublisherModule) extends Actor with HttpService with LazyLogging {

  def actorRefFactory = context

  implicit val timeout = Timeout(5.seconds)

  // create tables  if they don't exist
  if (Await.result(modules.db.run(MTable.getTables), 5.seconds).isEmpty) {
    modules.projectsDal.createTables()
    modules.testsDal.createTables()
    modules.jobsDal.createTables()
    println("Create tables for projects, tests and jobs")
  }

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
    projects.ProjectTriggerRoute ~ projects.ProjectTriggerRouteBB ~
    tests.TestGetRoute ~ tests.TestsGetRoute ~
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
