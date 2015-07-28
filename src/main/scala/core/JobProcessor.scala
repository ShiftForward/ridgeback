package core

import akka.actor.ActorRef
import persistence.entities.{ JobDefinition, JobDefinitionUtilities }

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

trait JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Duration
}

object TimeJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Duration = {
    Shell.executeCommandsTime(job.script, Some(job.name), sender)
  }
}

object OutputJobProcessor extends JobProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None): Duration = {
    val lastOutput = Shell.executeCommands(job.script, Some(job.name), sender)
    val duration = Try(Duration(lastOutput.toDouble, JobDefinitionUtilities.timeFormatToTimeUnit(job.format.getOrElse("seconds"))))

    duration match {
      case Success(d) => d
      case Failure(e) => throw InvalidOutput(job.script.last, Some(job.name))
    }
  }
}

object IgnoreProcessor {
  def apply(job: JobDefinition, sender: Option[ActorRef] = None) = {
    Shell.executeCommands(job.script, Some(job.name), sender)
  }
}
