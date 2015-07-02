import sys.process._

object CommandFailed extends Exception { }

object CommandExecutor {
  def executeCommand(cmd : String) : Int = cmd !
  def executeCommandOutput(cmd : String) : String = cmd !!

  def executeMultipleCommands(cmds : List[String]) : Boolean = {
    try
      cmds.foreach(cmd => {
        val exitCode = executeCommand(cmd)
        // println("executing \"" + cmd + "\" - " + exitCode)
        if (exitCode != 0) throw CommandFailed
      })
    catch {
      case CommandFailed =>
        // println("interrupted process because a command failed")
        return false
    }

    true
  }

  def executeMultipleCommandsOutput(cmds : List[String]) : List[String] = {
    cmds.map(executeCommandOutput)
  }
}
