package core

import java.time.ZonedDateTime
import java.util.Date

import akka.actor.{ Actor, Props }
import persistence.entities.{ Project, Test }
import utils.{ Configuration, PersistenceModule }

import scala.util.{ Failure, Success }
import scala.util.{ Try, Success, Failure }
import scala.sys.process._

case class CloneRepository(commit: String, proj: Project)
case class Start(yamlStr: String, proj: Project)

class WorkerSupervisorActor(modules: Configuration with PersistenceModule) extends Actor {
  import context._

  def receive: Receive = {

    case CloneRepository(commit, proj) =>

      val dir = Files.createTempDirectory("repos")
      val dirFile = dir.toFile

      Seq("git", "clone", "--quiet", "--depth=1", proj.gitRepo, dir.toString).!
      Process(Seq("git", "checkout", "-qf", commit), dirFile).!
      val ymlFile = Process(Seq("cat", ".perftests.yml"), dirFile).!!

      self ! Start(ymlFile, proj)

    case Start(yamlStr, proj) =>
      val replyTo = sender()
      modules.testsDal.save(Test(None, proj.id, "commit", Some(ZonedDateTime.now()), None)) onComplete {
        case Success(testId: Int) =>
          context.actorOf(Props(new TestRunnerActor)) ! Run(yamlStr, testId)
          replyTo ! Some(testId)
        case Failure(t) =>
          replyTo ! t
          context.stop(self)
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
      context.stop(self)
    case BadConfiguration(errs) => println("BadConfiguration: " + errs)
    case _ => println("Unknown")
  }
}
