package persistence.entities

import java.util

import scala.beans.BeanProperty
import scala.collection.JavaConversions._
import scala.concurrent.duration._

class TestsConfiguration {
  @BeanProperty var before_jobs = new util.ArrayList[String]()
  @BeanProperty var jobs = new util.ArrayList[JobDefinition]()
  @BeanProperty var after_jobs = new util.ArrayList[String]()

  override def toString: String = {
    "before: %s, jobs: %s, after: %s".format(before_jobs.toString, jobs.toString, after_jobs.toString)
  }
}

object JobDefinition {
  def timeFormatToTimeUnit(metric: String): TimeUnit = metric match {
    case "days" => DAYS
    case "hours" => HOURS
    case "microseconds" => MICROSECONDS
    case "milliseconds" => MILLISECONDS
    case "minute" => MINUTES
    case "nanoseconds" => NANOSECONDS
    case "seconds" => SECONDS
    case _ => SECONDS
  }
}

class JobDefinition {
  @BeanProperty var name: String = null
  @BeanProperty var source: String = null
  @BeanProperty var format: String = null
  @BeanProperty var before_script = new util.ArrayList[String]()
  @BeanProperty var script = new util.ArrayList[String]()
  @BeanProperty var after_script = new util.ArrayList[String]()

  override def toString: String = {
    "name: %s, source: %s, format: %s, before: %s, script: %s after: %s".format(name, source, format, before_script.toString,
      script.toString, after_script.toString)
  }
}
