package core

import akka.actor.Actor
import net.jcazevedo.moultingyaml._
import persistence.entities.TestsConfiguration
import persistence.entities.YamlProtocol._

import scala.util.{ Failure, Success, Try }

trait TestRunnerException extends Exception
case class BadConfiguration(errors: Seq[String]) extends Exception(errors.mkString(";")) with TestRunnerException
case class CommandFailed(cmd: String, exitCode: Int, jobName: Option[String] = None) extends TestRunnerException
case class InvalidOutput(cmd: String, jobName: Option[String] = None) extends TestRunnerException

case class Run(yamlStr: String)
case class TestError(ex: Throwable)
case class CommandExecuted(cmd: String)
case class MetricOutput(m: Any, jobName: String)
object Finished

class TestRunnerActor extends Actor {
  override def receive = {
    case Run(yamlStr) =>

      Try(parseConfig(yamlStr).map(processConfig)) match {
        case Failure(ex) =>
          println("exxx: " + ex); sender ! TestError(ex)
        case Success(_) => ;
      }

      sender ! Finished
  }

  private val sourceFormats = Map(
    "ignore" -> List(),
    "time" -> List(),
    "output" -> List("days", "hours", "microseconds", "milliseconds", "minute", "nanoseconds", "seconds"))

  private def parseConfig(yamlStr: String): Try[TestsConfiguration] = {
    val config = Try(yamlStr.parseYaml.convertTo[TestsConfiguration]) match {
      case Failure(e: DeserializationException) => throw BadConfiguration(Seq(e.getMessage))
      case Failure(e: Throwable) => throw e
      case Success(null) => throw BadConfiguration(Seq("Could not parse"))
      case Success(c) if c.jobs.isEmpty => throw BadConfiguration(Seq("Test has no jobs"))
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

    if (errors.nonEmpty) {
      throw BadConfiguration(errors.toList)
    }

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
