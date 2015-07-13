package persistence.entities

import scala.concurrent.duration._

case class TestsConfiguration(
  before_jobs: Option[List[String]],
  jobs: List[JobDefinition],
  after_jobs: Option[List[String]])

object JobDefinitionUtilities {
  def timeMetricToTimeUnit(metric: String): TimeUnit = metric match {
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

case class JobDefinition(
  name: String,
  metric: String,
  before_script: Option[List[String]],
  script: List[String],
  after_script: Option[List[String]])
