package utils

import scala.concurrent.duration.Duration

object RichDuration {
  implicit class RichDuration(val dur: Duration) {
    def toShortString: String = {
      Seq(
        " seconds" -> "s",
        " second" -> "s",
        " minutes" -> "min",
        " minute" -> "min",
        " hours" -> "h",
        " hour" -> "h",
        " days" -> "d",
        " day" -> "d",
        " nanoseconds" -> "ns",
        " nanosecond" -> "ns",
        " microseconds" -> "µs",
        " microsecond" -> "µs",
        " milliseconds" -> "ms",
        " millisecond" -> "ms",
        " nanoseconds" -> "ns",
        " nanosecond" -> "ns",
        "Duration.Undefined" -> "?")
        .foldLeft(dur.toString) {
          case (z, (s, r)) => z.replaceAll(s, r)
        }
    }

    def compareWithThreshold(otherDur: Duration, thresh: Int): Int = {
      def inThreshold = PercentageDiffDurationStat(dur, otherDur) <= thresh

      dur.compare(otherDur) match {
        case c if inThreshold => 0
        case c => c
      }
    }
  }
}
