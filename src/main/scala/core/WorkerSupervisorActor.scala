package core

import java.io.File
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

case object CloneRepository
case class Start(yamlStr: String, dir: Option[String] = None)

class WorkerSupervisorActor(modules: Configuration with PersistenceModule with EventPublisherModule,
                            project: Project, prSource: Option[PullRequestPayload]) extends Actor {
  import context._

  var tempDir: Option[File] = None

  def receive: Receive = {

    case CloneRepository =>
      val dir = Files.createTempDirectory("repos")
      val dirFile = dir.toFile
      tempDir = Some(dirFile)

      Process(Seq("git", "clone", project.gitRepo, dir.toString)).!
      Process(Seq("git", "checkout", "-qf", prSource.fold("HEAD")(_.commit)), dirFile).!
      val ymlFile = Process(Seq("cat", ".perftests.yml"), dirFile).!!

      self.tell(Start(ymlFile, Some(dir.toString)), sender())

    case Start(yamlStr, dir) =>
      val replyTo = sender()

      val commit = prSource.fold("HEAD")(_.commit)
      val branch = prSource.map(_.branch)
      val prId = prSource.map(_.pullRequestId)
      modules.testsDal.save(Test(None, project.id, commit, branch, prId, dir, Some(ZonedDateTime.now()), None)) onComplete {
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
    case TestError(ex) => modules.publish(project, testId, EventType.TestError, ex.getMessage)
    case CommandExecuted(cmd) => modules.publish(project, testId, EventType.CmdExecuted, cmd)
    case CommandFailed(cmd, exitCode, jobName) => modules.publish(project, testId, EventType.CmdFailed, s"$cmd ($exitCode)")
    case CommandStdout(str) => modules.publish(project, testId, EventType.Stdout, str)
    case CommandStderr(str) => modules.publish(project, testId, EventType.Stderr, str)
    case MetricOutput(durations, jobName, source, threshold) =>
      modules.publish(project, testId, EventType.Metric, durations.map(d => d.toMillis).mkString(","))
      Await.result(modules.jobsDal.save(Job(None, project.id, Some(testId), jobName, source, threshold, durations)), 5.seconds)
    case InvalidOutput(cmd, jobName) => modules.publish(project, testId, EventType.InvalidOutput, cmd)
    case Finished(test) =>
      modules.testsDal.setTestEndDate(testId, ZonedDateTime.now())
      tempDir.foreach(d => FileUtils.forceDelete(d))
      modules.publish(project, testId, EventType.Finished, "")
      prSource.foreach(pr => {
        val actor = system.actorOf(Props(new CommentWriterActor(modules, BitbucketCommentWriter, test)))
        actor ! SendComment(project, testId, pr)
      })
      context.stop(self)
    case BadConfiguration(errs) => modules.publish(project, testId, EventType.BadConfiguration, errs.mkString("\n"))
    case _ => println("Unknown")
  }
}
