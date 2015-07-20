package core

import java.nio.file.Files
import java.time.ZonedDateTime

import akka.actor.{ Actor, Props }
import persistence.entities.{ Job, Project, PullRequestSource, Test }
import utils.{ Configuration, PersistenceModule }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.sys.process._
import scala.util.{ Failure, Success }

case class CloneRepository(commit: String)
case class Start(yamlStr: String)

class WorkerSupervisorActor(modules: Configuration with PersistenceModule, project: Project, prSource: Option[PullRequestSource]) extends Actor {
  import context._

  var testId: Option[Int] = None

  def receive: Receive = {

    case CloneRepository(commit) =>

      val dir = Files.createTempDirectory("repos")
      val dirFile = dir.toFile

      Seq("git", "clone", "--quiet", "--depth=1", project.gitRepo, dir.toString).!
      Process(Seq("git", "checkout", "-qf", commit), dirFile).!
      val ymlFile = Process(Seq("cat", ".perftests.yml"), dirFile).!!

      self ! Start(ymlFile)

    case Start(yamlStr) =>
      val replyTo = sender()
      modules.testsDal.save(Test(None, project.id, "commit", Some(ZonedDateTime.now()), None)) onComplete {
        case Success(test: Int) =>
          testId = Some(test)
          context.actorOf(Props(new TestRunnerActor)) ! Run(yamlStr, testId.get)
          replyTo ! testId
        case Failure(t) =>
          replyTo ! t
          context.stop(self)
      }

    case TestError(ex) => println("TestError: " + ex)
    case CommandExecuted(cmd) => println("CommandExecuted: " + cmd)
    case CommandFailed(cmd, exitCode, jobName) => println("CommandFailed: " + cmd + " - " + exitCode + " - " + jobName)
    case CommandStdout(str) => println("CommandStdout: " + str)
    case CommandStderr(str) => println("CommandStderr: " + str)
    case MetricOutput(duration, jobName, source) =>
      println("MetricOutput: " + duration + " - " + jobName)
      Await.result(modules.jobsDal.save(Job(None, project.id, testId, jobName, source, duration)), 5.seconds)
    case InvalidOutput(cmd, jobName) => println("InvalidOutput: " + cmd + " - " + jobName)
    case Finished =>
      println(s"Finished $testId")
      modules.testsDal.setTestEndDate(testId.get, ZonedDateTime.now())
      context.stop(self)
    case BadConfiguration(errs) => println("BadConfiguration: " + errs)
    case _ => println("Unknown")
  }
}
