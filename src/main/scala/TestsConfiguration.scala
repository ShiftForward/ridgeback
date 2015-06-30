import java.util

import scala.beans.BeanProperty
import scala.concurrent.duration._
import scala.collection.JavaConversions._

class TestsConfiguration {
  @BeanProperty var before_jobs = new util.ArrayList[String]()
  @BeanProperty var jobs = new util.ArrayList[JobDefinition]()
  @BeanProperty var after_jobs = new util.ArrayList[String]()

  override def toString: String = {
    "before: %s, jobs: %s, after: %s".format(before_jobs.toString, jobs.toString, after_jobs.toString)
  }

  def process: Boolean = {
    CommandExecutor.executeMultipleCommands(before_jobs.toList)
    jobs.foreach(job => job.runJob)
    CommandExecutor.executeMultipleCommands(after_jobs.toList)
  }
}

object JobDefinition {
  def timeMetricToTimeUnit(metric : String) : TimeUnit = metric match {
    case "time_days" => DAYS
    case "time_hours" => HOURS
    case "time_microseconds" => MICROSECONDS
    case "time_milliseconds" => MILLISECONDS
    case "time_minute" => MINUTES
    case "time_nanoseconds" => NANOSECONDS
    case "time_seconds" => SECONDS
    case _ => SECONDS
  }
}

class JobDefinition {

  val valid_metrics = "time_days" :: "time_hours" :: "time_microseconds" :: "time_milliseconds" :: "time_minute" ::
    "time_nanoseconds" :: "time_seconds" :: Nil // :: "perf_cpu" :: "perf_ram" ...

  @BeanProperty var name: String = null
  @BeanProperty var metric: String = null
  @BeanProperty var before_script = new util.ArrayList[String]()
  @BeanProperty var script = new util.ArrayList[String]()
  @BeanProperty var after_script = new util.ArrayList[String]()

  def runJob : Boolean = {
    // println("running job " + name)
    CommandExecutor.executeMultipleCommands(before_script.toList)
    val times = CommandExecutor.executeMultipleCommandsOutput(script.toList)
    times.foreach(time => {
      val d = Duration(time.toDouble, JobDefinition.timeMetricToTimeUnit(metric))
      println("Job %s took %s".format(name, d))
    })
    CommandExecutor.executeMultipleCommands(after_script.toList)
  }

  override def toString: String = {
    "name: %s, metric: %s, before: %s, script: %s after: %s".format(name, metric, before_script.toString,
      script.toString, after_script.toString)
  }
}
