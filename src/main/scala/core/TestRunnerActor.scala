package core

import akka.actor.Actor
import net.jcazevedo.moultingyaml._
import persistence.entities.TestsConfiguration
import persistence.entities.YamlProtocol._

import scala.util.{ Failure, Success, Try }
import scala.concurrent.duration._

trait TestRunnerException extends Exception
case class BadConfiguration(errors: Seq[String]) extends Exception(errors.mkString(";")) with TestRunnerException
case class CommandFailed(cmd: String, exitCode: Int, jobName: Option[String] = None) extends Exception(s"$cmd - $exitCode - $jobName") with TestRunnerException
case class InvalidOutput(cmd: String, jobName: Option[String] = None) extends TestRunnerException

case class Run(yamlStr: String, testId: Int)
case class TestError(ex: Throwable)
case class CommandExecuted(cmd: String)
case class CommandStdout(str: String)
case class CommandStderr(str: String)
case class MetricOutput(duration: Duration, jobName: String, source: String)
case object Finished

class TestRunnerActor extends Actor {
  def receive = {
    case Run(yamlStr, testId) =>

      parseConfig(yamlStr) match {
        case Failure(ex: TestRunnerException) => sender ! ex
        case Failure(ex) => sender ! TestError(ex)
        case Success(config) =>
          Try(processConfig(config)) match {
            case Failure(ex: TestRunnerException) => sender ! ex
            case Failure(ex) => sender ! TestError(ex)
            case Success(_) => ;
          }
      }

      sender ! Finished
      context.stop(self)
  }

  private val sourceFormats = Map(
    "ignore" -> List(),
    "time" -> List(),
    "output" -> List("days", "hours", "microseconds", "milliseconds", "minute", "nanoseconds", "seconds"))

  private def parseConfig(yamlStr: String): Try[TestsConfiguration] = {
    val config = Try(yamlStr.parseYaml.convertTo[TestsConfiguration]) match {
      case Failure(e: DeserializationException) => return Failure(BadConfiguration(Seq(e.getMessage)))
      case Failure(e: Throwable) => return Failure(e)
      case Success(null) => return Failure(BadConfiguration(Seq("Could not parse")))
      case Success(c) if c.jobs.isEmpty => return Failure(BadConfiguration(Seq("Test has no jobs")))
      case Success(c) => c
    }

    val errors = config.jobs.flatMap(job => {
      if (job.script.isEmpty) {
        Some(s"${job.name} does not have any command in script")
      } else {
        sourceFormats.get(job.source) match {
          case Some(formats) =>
            if (formats.nonEmpty && !formats.contains(job.format.getOrElse(""))) {
              Some(s"${job.name} format ${job.format.getOrElse("\"\"")} doesn't match source ${job.source}")
            } else {
              None
            }
          case None =>
            Some(s"${job.name} has unknown source ${job.source}")
        }
      }
    })

    if (errors.nonEmpty)
      Failure(BadConfiguration(errors.toList))
    else
      Success(config)
  }

  private def processConfig(test: TestsConfiguration): Unit = {
    test.before_jobs.foreach(Shell.executeCommands(_, None, Some(sender())))

    test.jobs.foreach(job => {
      job.before_script.foreach(Shell.executeCommands(_, Some(job.name), Some(sender())))

      val metric: Option[MetricOutput] = job.source match {
        case "time" => TimeJobProcessor(job, Some(sender()))
        case "output" => OutputJobProcessor(job, Some(sender()))
        case _ => IgnoreJobProcessor(job, Some(sender()))
      }

      metric.foreach(d => sender ! d)

      job.after_script.foreach(Shell.executeCommands(_, Some(job.name), Some(sender())))
    })

    test.after_jobs.foreach(Shell.executeCommands(_, None, Some(sender())))
  }
}
