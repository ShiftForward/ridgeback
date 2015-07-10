package core

import akka.actor.Actor
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.error.YAMLException
import persistence.entities.TestsConfiguration

import scala.collection.JavaConversions._
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
        case Failure(ex) => sender ! TestError(ex)
        case Success(_) => ;
      }

      sender ! Finished
  }

  private val sourceFormats = Map(
    "ignore" -> List(),
    "time" -> List(),
    "output" -> List("days", "hours", "microseconds", "milliseconds", "minute", "nanoseconds", "seconds"))

  private def parseConfig(yamlStr: String): Try[TestsConfiguration] = {
    val yaml = new Yaml(new Constructor(classOf[TestsConfiguration]))
    val config = (Try(yaml.load(yamlStr).asInstanceOf[TestsConfiguration]) recover {
      case e: YAMLException => throw BadConfiguration(Seq(e.getMessage))
    }) get

    if (config == null) {
      throw BadConfiguration(Seq("Could not parse"))
    }

    if (config.jobs.isEmpty) {
      throw BadConfiguration(Seq("Test has no jobs"))
    }

    val errors = config.jobs.flatMap(job => {
      if (job.name == null || job.name.isEmpty) {
        Some("A job is missing its name")
      } else if (job.source == null || job.source.isEmpty) {
        Some(s"${job.name} is missing its source")
      } else if (job.script.isEmpty) {
        Some(s"${job.name} does not have any command in script")
      } else {
        sourceFormats.get(job.source) match {
          case Some(formats) =>
            if (formats.nonEmpty && !formats.contains(job.format)) {
              Some(s"${job.name} format ${job.format} doesn't match source ${job.source}")
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

    Shell.executeCommands(test.before_jobs.toList, None, Some(sender()))

    test.jobs.foreach(job => {
      Shell.executeCommands(job.before_script.toList, Some(job.name), Some(sender()))

      val metric: Option[MetricOutput] = job.source match {
        case "time" => TimeJobProcessor(job, Some(sender()))
        case "output" => OutputJobProcessor(job, Some(sender()))
        case _ => IgnoreJobProcessor(job, Some(sender()))
      }

      metric.foreach(d => sender ! d)

      Shell.executeCommands(job.after_script.toList, Some(job.name), Some(sender()))
    })

    Shell.executeCommands(test.after_jobs.toList, None, Some(sender()))
  }
}
