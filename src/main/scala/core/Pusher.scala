package core

import com.pusher.rest.Pusher
import utils.Configuration

object EventType extends Enumeration {
  type EventType = Value
  val TestError, CmdExecuted, CmdFailed, Stdout, Stderr, Metric, InvalidOutput, Finished, BadConfiguration = Value
}

class PusherService(modules: Configuration, projName: String) {

  lazy val pusher = new Pusher(modules.config.getString("pusher.appId"),
    modules.config.getString("pusher.apiKey"), modules.config.getString("pusher.apiSecret"))

  def send(testId: Int, event: EventType.EventType, msg: String) = {

    val channelName = s"$projName-$testId"
    val eventName = event.toString

    pusher.trigger(channelName, eventName, msg)
  }
}
