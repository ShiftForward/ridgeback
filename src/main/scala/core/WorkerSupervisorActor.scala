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

class WorkerSupervisorActor(modules: Configuration with PersistenceModule, project: Project,
                            prSource: Option[PullRequestPayload]) extends Actor {
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

  lazy val pusher = new PusherService(modules, project.name)

  def running(testId: Int): Receive = {
    case TestError(ex) => pusher.send(testId, EventType.TestError, ex.getMessage)
    case CommandExecuted(cmd) => pusher.send(testId, EventType.CmdExecuted, cmd)
    case CommandFailed(cmd, exitCode, jobName) => pusher.send(testId, EventType.CmdFailed, s"$cmd ($exitCode)")
    case CommandStdout(str) => pusher.send(testId, EventType.Stdout, str)
    case CommandStderr(str) => pusher.send(testId, EventType.Stderr, str)
    case MetricOutput(duration, jobName, source) =>
      pusher.send(testId, EventType.Metric, duration.toString)
      Await.result(modules.jobsDal.save(Job(None, project.id, Some(testId), jobName, source, duration)), 5.seconds)
    case InvalidOutput(cmd, jobName) => pusher.send(testId, EventType.InvalidOutput, cmd)
    case Finished =>
      pusher.send(testId, EventType.Finished, "")
      modules.testsDal.setTestEndDate(testId, ZonedDateTime.now())
      prSource.foreach(pr => {
        val actor = system.actorOf(Props(new CommentWriterActor(modules, BitbucketCommentWriter)))
        actor ! SendComment(project, testId, pr)
      })
      context.stop(self)
    case BadConfiguration(errs) => pusher.send(testId, EventType.BadConfiguration, errs.mkString("\n"))
    case _ => println("Unknown")
  }
}
