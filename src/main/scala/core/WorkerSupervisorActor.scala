package core

import java.time.ZonedDateTime
import java.util.Date

import akka.actor.{ PoisonPill, Actor, Props }
import persistence.entities.{ Project, Test }
import utils.{ Configuration, PersistenceModule }

import scala.util.{ Failure, Success }

case class Start(yamlStr: String, proj: Project)

class WorkerSupervisorActor(modules: Configuration with PersistenceModule) extends Actor {
  import context._

  def receive: Receive = {

    case Start(yamlStr, proj) =>
      val date = new Date
      val replyTo = sender()
      modules.testsDal.save(Test(None, proj.id, "commit", Some(ZonedDateTime.now()), None)) onComplete {
        case Success(testId: Int) =>
          context.actorOf(Props(new TestRunnerActor)) ! Run(yamlStr, testId)
          replyTo ! Some(testId)
        case Failure(t) => replyTo ! t
      }

    case TestError(ex) => println("TestError: " + ex)
    case CommandExecuted(cmd) => println("CommandExecuted: " + cmd)
    case CommandFailed(cmd, exitCode, jobName) => println("CommandFailed: " + cmd + " - " + exitCode + " - " + jobName)
    case CommandStdout(str) => println("CommandStdout: " + str)
    case CommandStderr(str) => println("CommandStderr: " + str)
    case MetricOutput(m, jobName) => println("MetricOutput: " + m + " - " + jobName)
    case InvalidOutput(cmd, jobName) => println("InvalidOutput: " + cmd + " - " + jobName)
    case Finished(testId) =>
      println(s"Finished $testId")
      modules.testsDal.setTestEndDate(testId, ZonedDateTime.now())
      self ! PoisonPill
    case BadConfiguration(errs) => println("BadConfiguration: " + errs)
    case _ => println("Unknown")
  }
}
