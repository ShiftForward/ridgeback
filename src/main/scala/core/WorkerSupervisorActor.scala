package core

import akka.actor.{ Props, Actor }
import utils.{ PersistenceModule, Configuration }

class WorkerSupervisorActor(modules: Configuration with PersistenceModule) extends Actor {
  override def receive: Receive = {
    case Run(yamlStr) => context.actorOf(Props(new TestRunnerActor)) ! Run(yamlStr)
    case TestError(ex) => println("TestError: " + ex)
    case CommandExecuted(cmd) => println("CommandExecuted: " + cmd)
    case CommandFailed(cmd, exitCode, jobName) => println("CommandFailed: " + cmd + " - " + exitCode + " - " + jobName)
    case CommandStdout(str) => println("CommandStdout: " + str)
    case CommandStderr(str) => println("CommandStderr: " + str)
    case MetricOutput(m, jobName) => println("MetricOutput: " + m + " - " + jobName)
    case InvalidOutput(cmd, jobName) => println("InvalidOutput: " + cmd + " - " + jobName)
    case Finished => println("Finished")
    case BadConfiguration(errs) => println("BadConfiguration: " + errs)
    case _ => println("UNKKK")
  }
}
