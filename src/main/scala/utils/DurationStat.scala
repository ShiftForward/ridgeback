package utils

import scala.concurrent.duration._

trait DurationStat /* extends (List[Duration] => Duration) */ {
  def apply(durations: List[Duration]): Option[Duration]
}

object MinDurationStat extends DurationStat {
  def apply(durations: List[Duration]) = if (durations.isEmpty) None else Some(durations.min)
}

object MaxDurationStat extends DurationStat {
  def apply(durations: List[Duration]) = if (durations.isEmpty) None else Some(durations.max)
}

object MeanDurationStat extends DurationStat {
  def apply(durations: List[Duration]) =
    if (durations.isEmpty) None else Some((durations.map(_.toMillis).sum / durations.length).milliseconds)
}

object MedianDurationStat extends DurationStat {
  def apply(durations: List[Duration]) = {
    val ordered = durations.sorted
    ordered.size match {
      case 0 => None
      case 1 => Some(ordered.head)
      case s if s % 2 == 0 => Some((ordered(s / 2 - 1) + ordered(s / 2)) / 2)
      case s => Some(ordered(Math.floor(s / 2).toInt))
    }
  }
}
