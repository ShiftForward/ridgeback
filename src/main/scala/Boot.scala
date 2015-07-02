import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import api.RoutesActor
import spray.can.Http
import utils.{ActorModuleImpl, ConfigurationModuleImpl, PersistenceModuleImpl}

import scala.concurrent.duration._

object Boot extends App   {

  // configuring modules for application, cake pattern for DI
  val modules = new ConfigurationModuleImpl  with ActorModuleImpl with PersistenceModuleImpl

  // create and start our service actor
  val service = modules.system.actorOf(Props(classOf[RoutesActor], modules), "routesActor")

  implicit val system = modules.system
  implicit val timeout = Timeout(5.seconds)

  // start a new HTTP server on the configured port with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}
