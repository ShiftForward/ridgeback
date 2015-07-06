import javax.management.openmbean.KeyAlreadyExistsException

import akka.actor.Actor
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import persistence.entities.{ JobDefinition, TestsConfiguration }
import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import sys.process._

case class BadConfiguration(error: String) extends Exception {}
case class CommandFailed(cmd: String, exitCode: Int, jobName: Option[String] = None) extends Exception {}
case class InvalidOutput(cmd: String, jobName: Option[String] = None) extends Exception {}

case class Run(yamlStr: String)
case class TestError(ex: Throwable)
object Finished

class TestRunnerActor extends Actor {
  override def receive = {
    case Run(yamlStr) =>
      try {
        val config = validateYaml(yamlStr)
        processConfig(config)
      } catch {
        case ex: Throwable => sender ! TestError(ex)
      } finally {
        sender ! Finished
      }
  }

  private val validMetrics = "time_days" :: "time_hours" :: "time_microseconds" :: "time_milliseconds" :: "time_minute" ::
    "time_nanoseconds" :: "time_seconds" :: Nil // :: "perf_cpu" :: "perf_ram" ...

  private def validateYaml(yamlStr: String): TestsConfiguration = {
    try {
      val yaml = new Yaml(new Constructor(classOf[TestsConfiguration]))
      val config = yaml.load(yamlStr).asInstanceOf[TestsConfiguration]

      config.jobs.foreach(job => {
        val jobName = job.getName

        if (!validMetrics.contains(job.getMetric)) {
          val metric = job.getMetric
          throw BadConfiguration(s"Unknown metric $metric in $jobName")
        }

        if (job.script.isEmpty) {
          throw BadConfiguration(s"$jobName does not have any command in script")
        }
      })

      config
    } catch {
      case e: Exception => throw BadConfiguration(e.getMessage)
    }
  }

  private def processConfig(test: TestsConfiguration): Unit = {

    test.before_jobs.foreach(cmd => {
      val exitCode = cmd.!(new ConsoleProcessLogger)
      if (exitCode != 0) throw CommandFailed(cmd, exitCode)
    })

    test.jobs.foreach(job => {

      val jobName = job.name
      val metric = job.metric

      job.before_script.foreach(cmd => {
        val exitCode = cmd.!(new ConsoleProcessLogger)
        if (exitCode != 0) throw CommandFailed(cmd, exitCode, Some(jobName))
      })

      var lastOutput = ""

      job.script.foreach(cmd => {
        try {
          lastOutput = cmd.!!(new ConsoleProcessLogger)
        } catch {
          case ex: KeyAlreadyExistsException => throw CommandFailed(cmd, ex.getMessage.split(' ').last.toInt, Some(jobName)) // "Nonzero exit value: XX"
        }
      })

      try {
        val duration = Duration(lastOutput.toDouble, JobDefinition.timeMetricToTimeUnit(job.metric))
        println(s"OUT: $jobName took $duration ($metric)")
      } catch {
        case ex: NumberFormatException => throw InvalidOutput(job.script.last, Some(jobName))
      }

      job.after_script.foreach(cmd => {
        val exitCode = cmd.!(new ConsoleProcessLogger)
        if (exitCode != 0) throw CommandFailed(cmd, exitCode, Some(jobName))
      })

    })

    test.after_jobs.foreach(cmd => {
      val exitCode = cmd.!(new ConsoleProcessLogger)
      if (exitCode != 0) throw CommandFailed(cmd, exitCode)
    })
  }
}

// TODO: eventually change this class to "write" to WebSockets or pusher.com
class ConsoleProcessLogger() extends ProcessLogger {
  def out(s: => String): Unit = println("Out: " + s)
  def err(s: => String): Unit = println("Err: " + s)
  def buffer[T](f: => T): T = f
}
