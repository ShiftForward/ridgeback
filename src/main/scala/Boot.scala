import akka.actor.{ ActorRef, Inbox, ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import api.RoutesActor
import com.typesafe.config.ConfigFactory
import spray.can.Http
import utils.{ ActorModuleImpl, ConfigurationModuleImpl, PersistenceModuleImpl }

import scala.concurrent.duration._

object Boot extends App {

  // configuring modules for application, cake pattern for DI
  val modules = new ConfigurationModuleImpl with ActorModuleImpl with PersistenceModuleImpl

  // create and start our service actor
  val service = modules.system.actorOf(Props(classOf[RoutesActor], modules), "routesActor")

  val config = ConfigFactory.load()

  implicit val system = modules.system
  implicit val timeout = Timeout(5.seconds)

  // start a new HTTP server on the configured port with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = config.getString("app.interface"), port = config.getInt("app.port"))
}

object TestRunnerBoot extends App {
  val text =
    """
  before_jobs:
    - mkdir test
    - touch test/hi.txt

  jobs:
    - name: job1_google
      metric: time_seconds
      script:
        - curl -w %{time_total} -o /dev/null -s http://google.com/
    - name: job2_bing
      metric: time_seconds
      before_script:
        - touch executingjob2.txt
      script:
        - curl -w %{time_total} -o /dev/null -s http://bing.com/
      after_script:
        - rm executingjob2.txt
    - name: job3_pi
      metric: time_seconds
      script:
        - curl -w %{time_total} -o /dev/null -s http://jpdias.noip.me:8080/

  after_jobs:
    - rm test/hi.txt
    - rmdir test
    """.stripMargin

  val system = ActorSystem("helloakka")
  val testRunner = system.actorOf(Props[TestRunnerActor], "TestRunner")
  val inbox = Inbox.create(system)

  inbox.send(testRunner, Run(text))

  val TestError(ex) = inbox.receive(50.seconds)
  println(s"Ex: $ex")
}
