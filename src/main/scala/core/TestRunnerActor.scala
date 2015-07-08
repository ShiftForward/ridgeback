package core

import akka.actor.Actor
import net.jcazevedo.moultingyaml._
import persistence.entities.YamlProtocol._
import persistence.entities.{ JobDefinitionUtilities, JobDefinition, TestsConfiguration }

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.sys.process._
import scala.util.{ Failure, Success, Try }

trait TestRunnerException extends Exception
case class BadConfiguration(errors: Seq[String]) extends Exception(errors.mkString(";")) with TestRunnerException
case class CommandFailed(cmd: String, exitCode: Int, jobName: Option[String] = None) extends TestRunnerException
case class InvalidOutput(cmd: String, jobName: Option[String] = None) extends TestRunnerException

case class Run(yamlStr: String)
case class TestError(ex: Throwable)
case class CommandExecuted(cmd: String)
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

  private val validMetrics = "time_days" :: "time_hours" :: "time_microseconds" :: "time_milliseconds" :: "time_minute" ::
    "time_nanoseconds" :: "time_seconds" :: "ignore" :: Nil // :: "perf_cpu" :: "perf_ram" ...

  private def parseConfig(yamlStr: String): Try[TestsConfiguration] = {
    val config = (Try(yamlStr.parseYaml.convertTo[TestsConfiguration]) recover {
      case e: DeserializationException => throw BadConfiguration(Seq(e.getMessage))
    }) get

    if (config == null) {
      throw BadConfiguration(Seq("Could not parse"))
    }

    if (config.jobs.isEmpty) {
      throw BadConfiguration(Seq("Test has no jobs"))
    }

    val errors = config.jobs.flatMap(job => {
      if (!validMetrics.contains(job.metric)) {
        Some(s"Unknown metric ${job.metric} in ${job.name}")
      } else if (job.script.isEmpty) {
        Some(s"${job.name} does not have any command in script")
      } else {
        None
      }
    })

    if (errors.nonEmpty) {
      throw BadConfiguration(errors.toList)
    }

    Success(config)
  }

  private def processConfig(test: TestsConfiguration): Unit = {

    test.before_jobs.foreach(cmds => executeCommands(cmds))

    test.jobs.foreach((job: JobDefinition) => {
      job.before_script.foreach(cmds => executeCommands(cmds, Some(job.name)))

      val lastOutput = executeCommandsOutput(job.script)

      if (job.metric != "ignore") {
        val duration = Try(Duration(lastOutput.toDouble, JobDefinitionUtilities.timeMetricToTimeUnit(job.metric)))

        duration match {
          case Success(d) => println(s"OUT: ${job.name} took $d (${job.metric})")
          case Failure(e) => throw InvalidOutput(job.script.last, Some(job.name))
        }
      }

      job.after_script.foreach(cmds => executeCommands(cmds, Some(job.name)))
    })

    test.after_jobs.foreach(cmds => executeCommands(cmds))
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
