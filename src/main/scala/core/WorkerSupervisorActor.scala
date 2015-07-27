package core

import java.nio.file.Files
import java.time.ZonedDateTime

import akka.actor.{ Actor, Props }
import org.apache.commons.io.FileUtils
import persistence.entities._
import utils.{ Configuration, PersistenceModule }

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.sys.process._
import scala.util.{ Failure, Success }

case class CloneRepository(pr: PullRequestPayload)
case class Start(yamlStr: String)

class WorkerSupervisorActor(modules: Configuration with PersistenceModule, project: Project, prSource: Option[PullRequestPayload]) extends Actor {
  import context._

  def receive: Receive = {
    case CloneRepository(pr) =>

      val dir = Files.createTempDirectory("repos")
      val dirFile = dir.toFile
      FileUtils.forceDeleteOnExit(dirFile)

      Process(Seq("git", "clone", project.gitRepo, dir.toString)).!
      Process(Seq("git", "checkout", "-qf", pr.commit), dirFile).!
      val ymlFile = Process(Seq("cat", ".perftests.yml"), dirFile).!!

      self.tell(Start(ymlFile), sender())

    case Start(yamlStr) =>
      val replyTo = sender()
      modules.testsDal.save(Test(None, project.id, "commit", Some(ZonedDateTime.now()), None)) onComplete {
        case Success(testId) =>
          context.actorOf(Props(new TestRunnerActor)) ! Run(yamlStr, testId)
          become(running(testId))
          replyTo ! testId
        case Failure(t) =>
          replyTo ! t
          context.stop(self)
      }
  }

  def running(testId: Int): Receive = {
    case TestError(ex) => println("TestError: " + ex)
    case CommandExecuted(cmd) => println("CommandExecuted: " + cmd)
    case CommandFailed(cmd, exitCode, jobName) => println("CommandFailed: " + cmd + " - " + exitCode + " - " + jobName)
    case CommandStdout(str) => println("CommandStdout: " + str)
    case CommandStderr(str) => println("CommandStderr: " + str)
    case MetricOutput(duration, jobName, source) =>
      println("MetricOutput: " + duration + " - " + jobName)
      Await.result(modules.jobsDal.save(Job(None, project.id, Some(testId), jobName, source, duration)), 5.seconds)
    case InvalidOutput(cmd, jobName) => println("InvalidOutput: " + cmd + " - " + jobName)
    case Finished =>
      println(s"Finished $testId")
      modules.testsDal.setTestEndDate(testId, ZonedDateTime.now())
      prSource.foreach(pr => {
        val actor = system.actorOf(Props(new CommentWriterActor(modules, BitbucketCommentWriter)))
        actor ! SendComment(project, testId, pr)
      })

      context.stop(self)
    case BadConfiguration(errs) => println("BadConfiguration: " + errs)
    case _ => println("Unknown")
  }
}
