package core

import akka.actor.Actor
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.error.YAMLException
import persistence.entities.{ JobDefinition, TestsConfiguration }

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.sys.process._
import scala.util.{ Failure, Success, Try }

trait TestRunnerException extends Exception
case class BadConfiguration(error: String) extends Exception(error) with TestRunnerException
case class CommandFailed(cmd: String, exitCode: Int, jobName: Option[String] = None) extends TestRunnerException
case class InvalidOutput(cmd: String, jobName: Option[String] = None) extends TestRunnerException

case class ParsingError(error: String)

case class Run(yamlStr: String)
case class TestError(ex: Throwable)
case class CommandExecuted(cmd: String)
object Finished

class TestRunnerActor extends Actor {
  override def receive = {
    case Run(yamlStr) =>
      try {
        parseConfig(yamlStr).fold(
          error => sender ! TestError(BadConfiguration(error.error)),
          config => processConfig(config))
      } catch {
        case ex: TestRunnerException => sender ! TestError(ex)
      } finally {
        sender ! Finished
      }
  }

  private val validMetrics = "time_days" :: "time_hours" :: "time_microseconds" :: "time_milliseconds" :: "time_minute" ::
    "time_nanoseconds" :: "time_seconds" :: "ignore" :: Nil // :: "perf_cpu" :: "perf_ram" ...

  private def parseConfig(yamlStr: String): Either[ParsingError, TestsConfiguration] = {
    val yaml = new Yaml(new Constructor(classOf[TestsConfiguration]))
    val config = (Try(yaml.load(yamlStr).asInstanceOf[TestsConfiguration]) recover {
      case e: YAMLException => return Left(ParsingError(e.getMessage))
    }) get

    if (config == null)
      return Left(ParsingError("Could not parse"))

    if (config.jobs.isEmpty) {
      return Left(ParsingError("Test has no jobs"))
    }

    config.jobs.foreach(job => {
      val jobName = job.getName

      if (job.name == null || job.name.isEmpty) {
        return Left(ParsingError("A job is missing its name"))
      }

      if (job.metric == null || job.metric.isEmpty) {
        return Left(ParsingError(s"$jobName is missing its metric"))
      }

      if (job.script.isEmpty) {
        return Left(ParsingError(s"$jobName does not have any command in script"))
      }

      if (!validMetrics.contains(job.getMetric)) {
        val metric = job.getMetric
        return Left(ParsingError(s"Unknown metric $metric in $jobName"))
      }

      if (job.script.isEmpty) {
        return Left(ParsingError(s"$jobName does not have any command in script"))
      }
    })

    Right(config)
  }

  private def processConfig(test: TestsConfiguration): Unit = {

    executeCommands(test.before_jobs.toList)

    test.jobs.foreach(job => {

      val jobName = job.name
      val metric = job.metric

      executeCommands(job.before_script.toList, Some(jobName))

      val lastOutput = executeCommandsOutput(job.script.toList)

      if (metric != "ignore") {
        val duration = Try(Duration(lastOutput.toDouble, JobDefinition.timeMetricToTimeUnit(job.metric)))

        duration match {
          case Success(d) => println(s"OUT: $jobName took $d ($metric)")
          case Failure(e) => throw InvalidOutput(job.script.last, Some(jobName))
        }
      }

      executeCommands(job.after_script.toList, Some(jobName))
    })

    executeCommands(test.after_jobs.toList)
  }

  // executes commands in a shell and throws if any command fails
  private def executeCommands(cmds: List[String], jobName: Option[String] = None): Unit = {
    cmds.foreach(cmd => {
      println("cmd: " + cmd)
      sender ! CommandExecuted(cmd)

      val exitCode = cmd ! ConsoleProcessLogger
      if (exitCode != 0) throw CommandFailed(cmd, exitCode, jobName)
    })
  }

  // similar to executeCommands however it returns the output of the last command
  private def executeCommandsOutput(cmds: List[String], jobName: Option[String] = None): String = {
    var lastOutput = ""

    cmds.foreach(cmd => {
      println("cmd: " + cmd)
      sender ! CommandExecuted(cmd)

      Try(cmd !! ConsoleProcessLogger) match {
        case Success(o) => lastOutput = o
        case Failure(ex: RuntimeException) => throw CommandFailed(cmd, ex.getMessage.split(' ').last.toInt, jobName) // "Nonzero exit value: XX"
        case Failure(ex) => throw CommandFailed(cmd, -1, jobName)
      }
    })

    lastOutput
  }
}

// TODO: eventually change this class to "write" to WebSockets or pusher.com
object ConsoleProcessLogger extends ProcessLogger {
  def out(s: => String): Unit = println("Out: " + s)
  def err(s: => String): Unit = println("Err: " + s)
  def buffer[T](f: => T): T = f
}
