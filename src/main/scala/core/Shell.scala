package core

import akka.actor.ActorRef

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import sys.process._

object Shell {

  // executes commands in a shell, throws if any command fails and returns the last command stdout
  def executeCommands(cmds: List[String], jobName: Option[String] = None, sender: Option[ActorRef] = None): String = {
    var lastOutput = ""

    cmds.foreach(cmd => {
      sender.foreach(s => s ! CommandExecuted(cmd))

      Try(cmd !! new ActorProcessLogger(sender)) match {
        case Success(o) => lastOutput = o
        case Failure(ex: RuntimeException) => throw CommandFailed(cmd, ex.getMessage.split(' ').last.toInt, jobName) // "Nonzero exit value: XX"
        case Failure(ex) => throw CommandFailed(cmd, -1, jobName)
      }
    })

    lastOutput
  }

  // wraps all commands in a call to /usr/bin/time and gets the real time of the /usr/bin/time output
  def executeCommandsTime(cmds: List[String], jobName: Option[String] = None, sender: Option[ActorRef] = None): Duration = {
    val cmdSeq = Seq("/usr/bin/time", "-p", "sh", "-c", cmds.mkString(" && "))
    val cmd = cmdSeq.mkString(" ")
    sender.foreach(s => s ! CommandExecuted(cmd))

    // the time command prints its output to stderr thus we need
    // a way to capture that
    val logger = new BufferActorProcessLogger(sender)

    Try(cmdSeq !! logger) match {
      case Success(o) =>
        val timeOutput = """real[\s]+(\d+.\d*)[\r\n\s]+user[\s]+(\d+.\d*)[\r\n\s]sys[\s]+(\d+.\d*)""".r.unanchored
        logger.err.toString() match {
          case timeOutput(real, user, sys) => real.toDouble.seconds
          case _ => throw CommandFailed(cmd, -1, jobName)
        }
      case Failure(ex: RuntimeException) => throw CommandFailed(cmd, ex.getMessage.split(' ').last.toInt, jobName) // "Nonzero exit value: XX"
      case Failure(ex) => throw CommandFailed(cmd, -1, jobName)
    }
  }
}

// forward process output to an actor (CommandStdout and CommandStderr) and store it in out and err
class BufferActorProcessLogger(actor: Option[ActorRef]) extends ProcessLogger {
  val out = new StringBuilder
  val err = new StringBuilder

  override def out(s: => String): Unit = {
    out.append(s + "\n")
    actor.foreach(a => a ! CommandStdout(s))
  }

  override def err(s: => String): Unit = {
    err.append(s + "\n")
    actor.foreach(a => a ! CommandStderr(s))
  }

  override def buffer[T](f: => T): T = f
}

// forward process output to an actor (CommandStdout and CommandStderr)
class ActorProcessLogger(actor: Option[ActorRef]) extends ProcessLogger {
  def out(s: => String): Unit = actor.foreach(a => a ! CommandStdout(s))
  def err(s: => String): Unit = actor.foreach(a => a ! CommandStderr(s))
  def buffer[T](f: => T): T = f
}
