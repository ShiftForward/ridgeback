package core

import com.pusher.rest.Pusher
import com.typesafe.scalalogging.LazyLogging
import persistence.entities.Project
import utils.Configuration

import scala.collection.mutable

object EventType extends Enumeration {
  type EventType = Value
  val TestError, CmdExecuted, CmdFailed, Stdout, Stderr, Metric, InvalidOutput, Finished, BadConfiguration = Value
}

trait EventPublisherModule {
  def publish(proj: Project, testId: Int, event: EventType.EventType, msg: String)
  def getPastEvents(proj: Project, testId: Int): Seq[(String, String)]

  protected def getChannelName(proj: Project, testId: Int): String = s"${proj.name}-$testId"
}

trait CachedEventPublisherModule extends EventPublisherModule {
  def publish(proj: Project, testId: Int, event: EventType.EventType, msg: String) = {
    pastEvents.getOrElseUpdate(getChannelName(proj, testId), mutable.MutableList[(String, String)]()) +=
      (event.toString -> msg)
  }

  lazy val pastEvents = mutable.Map[String, mutable.MutableList[(String, String)]]()
  override def getPastEvents(proj: Project, testId: Int): Seq[(String, String)] =
    pastEvents.getOrElse(getChannelName(proj, testId), Seq())
}

trait PusherEventPublisher extends CachedEventPublisherModule with LazyLogging {
  this: Configuration =>
  lazy val pusher = new Pusher(config.getString("pusher.appId"),
    config.getString("pusher.apiKey"), config.getString("pusher.apiSecret"))

  override def publish(proj: Project, testId: Int, event: EventType.EventType, msg: String) = {
    val channelName = getChannelName(proj, testId)
    logger.debug(s"[$channelName] - $event: $msg")
    pusher.trigger(channelName, event.toString, msg)
    super.publish(proj, testId, event, msg)
  }
}

trait ConsoleEventPublisher extends CachedEventPublisherModule {
  this: LazyLogging =>
  override def publish(proj: Project, testId: Int, event: EventType.EventType, msg: String) = {
    val channelName = getChannelName(proj, testId)
    logger.info(s"[$channelName] - $event: $msg")
    super.publish(proj, testId, event, msg)
  }
}
