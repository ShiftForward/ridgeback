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
import scala.concurrent.duration._

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
      } else if (job.metric == null || job.metric.isEmpty) {
        Some(s"${job.name} is missing its metric")
      } else if (!validMetrics.contains(job.getMetric)) {
        Some(s"Unknown metric ${job.metric} in ${job.name}")
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

    executeCommands(test.before_jobs.toList)

    test.jobs.foreach(job => {
      executeCommands(job.before_script.toList, Some(job.name))

      job.source match {
        case "time" =>
          val d = executeCommandsTime(job.script.toList, Some(job.name))
          sender ! MetricOutput(d, job.name)
        case "output" =>
          val lastOutput = executeCommandsOutput(job.script.toList, Some(job.name))
          val duration = Try(Duration(lastOutput.toDouble, JobDefinition.timeFormatToTimeUnit(job.format)))

          duration match {
            case Success(d) => sender ! MetricOutput(d, job.name)
            case Failure(e) => throw InvalidOutput(job.script.last, Some(job.name))
          }
        case _ => executeCommands(job.script.toList, Some(job.name));
      }

      executeCommands(job.after_script.toList, Some(job.name))
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

  // wraps all commands in a call to /usr/bin/time and gets the real time of the /usr/bin/time output
  private def executeCommandsTime(cmds: List[String], jobName: Option[String] = None): Duration = {
    val cmdSeq = Seq("/usr/bin/time", "-p", "sh", "-c", cmds.mkString("; "))
    val cmd = cmdSeq.mkString(" ")
    println("cmd: " + cmd)
    sender ! CommandExecuted(cmd)

    // the time command prints its output to stderr thus we need
    // a way to capture that
    val logger = new BufferProcessLogger

    Try(cmdSeq !! logger) match {
      case Success(o) =>
        val timeOutput = """real (\d+.\d*)[\r\n\s]+user (\d+.\d*)[\r\n\s]sys (\d+.\d*)""".r.unanchored
        logger.err.toString() match {
          case timeOutput(real, user, sys) => Duration(real.toDouble, SECONDS)
        }
      case Failure(ex: RuntimeException) => throw CommandFailed(cmd, ex.getMessage.split(' ').last.toInt, jobName) // "Nonzero exit value: XX"
      case Failure(ex) => throw CommandFailed(cmd, -1, jobName)
    }
  }
}

class BufferProcessLogger extends ProcessLogger {
  val out = new StringBuilder
  val err = new StringBuilder

  override def out(s: => String): Unit = out.append(s + "\n")
  override def err(s: => String): Unit = err.append(s + "\n")
  override def buffer[T](f: => T): T = f
}

// TODO: eventually change this class to "write" to WebSockets or pusher.com
object ConsoleProcessLogger extends ProcessLogger {
  def out(s: => String): Unit = println("Out: " + s)
  def err(s: => String): Unit = println("Err: " + s)
  def buffer[T](f: => T): T = f
}
