package core

import akka.actor.ActorRef

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }
import sys.process._

object Shell {
  // executes commands in a shell and throws if any command fails
  def executeCommands(cmds: List[String], jobName: Option[String] = None, sender: Option[ActorRef] = None): Unit = {
    cmds.foreach(cmd => {
      println("cmd: " + cmd)
      sender.foreach(s => s ! CommandExecuted(cmd))

      val exitCode = cmd ! ConsoleProcessLogger
      if (exitCode != 0) throw CommandFailed(cmd, exitCode, jobName)
    })
  }

  // similar to executeCommands however it returns the output of the last command
  def executeCommandsOutput(cmds: List[String], jobName: Option[String] = None, sender: Option[ActorRef] = None): String = {
    var lastOutput = ""

    cmds.foreach(cmd => {
      println("cmd: " + cmd)
      sender.foreach(s => s ! CommandExecuted(cmd))

      Try(cmd !! ConsoleProcessLogger) match {
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
    println("cmd: " + cmd)
    sender.foreach(s => s ! CommandExecuted(cmd))

    // the time command prints its output to stderr thus we need
    // a way to capture that
    val logger = new BufferProcessLogger

    Try(cmdSeq !! logger) match {
      case Success(_) =>
        val timeOutput = """real[\s]+(\d+.\d*)[\r\n\s]+user[\s]+(\d+.\d*)[\r\n\s]sys[\s]+(\d+.\d*)""".r.unanchored
        logger.err.toString() match {
          case timeOutput(real, user, sys) => Duration(real.toDouble, SECONDS)
          case _ => throw CommandFailed(cmd, -1, jobName)
        }
      case Failure(ex: RuntimeException) => throw CommandFailed(cmd, ex.getMessage.split(' ').last.toInt, jobName) // "Nonzero exit value: XX"
      case Failure(ex) => throw CommandFailed(cmd, -1, jobName)
    }
  }
}

class BufferProcessLogger extends ProcessLogger {
  val out = new StringBuilder
  val err = new StringBuilder

  def out(s: => String): Unit = out.append(s + "\n")
  def err(s: => String): Unit = err.append(s + "\n")
  def buffer[T](f: => T): T = f
}

// TODO: eventually change this class to "write" to WebSockets or pusher.com
object ConsoleProcessLogger extends ProcessLogger {
  def out(s: => String): Unit = println("Out: " + s)
  def err(s: => String): Unit = println("Err: " + s)
  def buffer[T](f: => T): T = f
}
