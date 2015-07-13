package core

import akka.actor.ActorRef
import persistence.entities.JobDefinition

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import scala.collection.JavaConversions._

trait JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput]
}

object TimeJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput] = {
    val d = Shell.executeCommandsTime(job.script.toList, Some(job.name), sender)
    Some(MetricOutput(d, job.name))
  }
}

object OutputJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput] = {
    val lastOutput = Shell.executeCommandsOutput(job.script.toList, Some(job.name), sender)
    val duration = Try(Duration(lastOutput.toDouble, JobDefinition.timeFormatToTimeUnit(job.format)))

    duration match {
      case Success(d) => Some(MetricOutput(d, job.name))
      case Failure(e) => throw InvalidOutput(job.script.last, Some(job.name))
    }
  }
}

object IgnoreJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput] = {
    Shell.executeCommands(job.script.toList, Some(job.name), sender)
    None
  }
}
