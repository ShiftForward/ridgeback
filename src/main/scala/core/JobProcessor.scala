package core

import akka.actor.ActorRef
import persistence.entities.{ JobDefinition, JobDefinitionUtilities }

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

trait JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput]
}

object TimeJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput] = {
    val d = Shell.executeCommandsTime(job.script, Some(job.name), sender)
    Some(MetricOutput(d, job.name))
  }
}

object OutputJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput] = {
    val lastOutput = Shell.executeCommands(job.script, Some(job.name), sender)
    val duration = Try(Duration(lastOutput.toDouble, JobDefinitionUtilities.timeFormatToTimeUnit(job.format getOrElse "seconds")))

    duration match {
      case Success(d) => Some(MetricOutput(d, job.name))
      case Failure(e) => throw InvalidOutput(job.script.last, Some(job.name))
    }
  }
}

object IgnoreJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Option[MetricOutput] = {
    Shell.executeCommands(job.script, Some(job.name), sender)
    None
  }
}
