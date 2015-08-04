package core

import com.pusher.rest.Pusher
import com.typesafe.scalalogging.LazyLogging
import utils.Configuration

import scala.collection.mutable

object EventType extends Enumeration {
  type EventType = Value
  val TestError, CmdExecuted, CmdFailed, Stdout, Stderr, Metric, InvalidOutput, Finished, BadConfiguration = Value
}

trait EventPublisherModule {
  def publish(projName: String, testId: Int, event: EventType.EventType, msg: String)
  def getPastEvents(channelName: String): Seq[(String, String)]
}

trait PusherEventPublisher extends EventPublisherModule with LazyLogging {
  this: Configuration =>
  lazy val pusher = new Pusher(config.getString("pusher.appId"),
    config.getString("pusher.apiKey"), config.getString("pusher.apiSecret"))

  override def publish(projName: String, testId: Int, event: EventType.EventType, msg: String) = {
    val channelName = s"$projName-$testId"
    logger.debug(s"[$channelName] - $event: $msg")
    pusher.trigger(channelName, event.toString, msg)
    pastEvents.getOrElseUpdate(channelName, mutable.MutableList[(String, String)]()) += (event.toString -> msg)
  }

  lazy val pastEvents = mutable.Map[String, mutable.MutableList[(String, String)]]()
  override def getPastEvents(channelName: String): Seq[(String, String)] = pastEvents.getOrElse(channelName, Seq())
}

trait ConsoleEventPublisher extends EventPublisherModule {
  this: LazyLogging =>
  override def publish(projName: String, testId: Int, event: EventType.EventType, msg: String) = {
    val channelName = s"$projName-$testId"
    logger.info(s"[$channelName] - $event: $msg")
    pastEvents.getOrElseUpdate(s"$channelName", mutable.MutableList[(String, String)]()) += (event.toString -> msg)
  }

  lazy val pastEvents = mutable.Map[String, mutable.MutableList[(String, String)]]()
  override def getPastEvents(channelName: String): Seq[(String, String)] = pastEvents.getOrElse(channelName, Seq())
}
