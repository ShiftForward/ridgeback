package core

import com.pusher.rest.Pusher
import com.typesafe.scalalogging.LazyLogging
import utils.Configuration

object EventType extends Enumeration {
  type EventType = Value
  val TestError, CmdExecuted, CmdFailed, Stdout, Stderr, Metric, InvalidOutput, Finished, BadConfiguration = Value
}

trait EventPublisherModule {
  def publish(projName: String, testId: Int, event: EventType.EventType, msg: String)
}

trait PusherEventPublisher extends EventPublisherModule {
  this: Configuration =>
  lazy val pusher = new Pusher(config.getString("pusher.appId"),
    config.getString("pusher.apiKey"), config.getString("pusher.apiSecret"))

  override def publish(projName: String, testId: Int, event: EventType.EventType, msg: String) = {
    val channelName = s"$projName-$testId"
    pusher.trigger(channelName, event.toString, msg)
  }
}

trait ConsoleEventPublisher extends EventPublisherModule with LazyLogging {
  this: LazyLogging =>
  override def publish(projName: String, testId: Int, event: EventType.EventType, msg: String) = {
    logger.info(s"[$projName-$testId] - $event: $msg")
  }
}
