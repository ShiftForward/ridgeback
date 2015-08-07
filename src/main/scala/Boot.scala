import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import api.RoutesActor
import com.typesafe.config.ConfigFactory
import core.PusherEventPublisher
import spray.can.Http
import utils._

import scala.concurrent.duration._

object Boot extends App {

  // configuring modules for application, cake pattern for DI
  val modules = new ConfigurationModuleImpl with ActorModuleImpl with PersistenceModuleImpl with PusherEventPublisher

  // create and start our service actor
  val service = modules.system.actorOf(Props(classOf[RoutesActor], modules), "routesActor")

  val config = ConfigFactory.load()

  implicit val system = modules.system
  implicit val timeout = Timeout(5.seconds)

  // start a new HTTP server on the configured port with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = config.getString("app.interface"), port = config.getInt("app.port"))
}
