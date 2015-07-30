package persistence.entities

import scala.concurrent.duration._

case class TestsConfiguration(
  before_jobs: Option[List[String]],
  jobs: List[JobDefinition],
  after_jobs: Option[List[String]])

object JobDefinitionUtilities {
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

case class JobDefinition(
  name: String,
  source: String,
  format: Option[String],
  repeat: Option[Int],
  burnin: Option[Int],
  threshold: Option[Int],
  before_script: Option[List[String]],
  script: List[String],
  after_script: Option[List[String]])
