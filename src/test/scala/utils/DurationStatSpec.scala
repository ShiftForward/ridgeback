package utils

import scala.concurrent.duration._

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

class DurationStatSpec extends Specification with NoTimeConversions {

  "A DurationStat" should {
    "calculate the minimum duration" in {
      "empty list" in {
        MinDurationStat(List()) must beNone
      }

      "single element list" in {
        MinDurationStat(List(5.seconds)) must beSome(5.seconds)
      }

      "non-empty list" in {
        MinDurationStat(List(2.seconds, 3.milliseconds)) must beSome(3.milliseconds)
      }
    }

    "calculate the maximum duration" in {
      "empty list" in {
        MaxDurationStat(List()) must beNone
      }

      "single element list" in {
        MaxDurationStat(List(5.seconds)) must beSome(5.seconds)
      }

      "non-empty list" in {
        MaxDurationStat(List(10.seconds, 1.hours, 2.milliseconds)) must beSome(1.hours)
      }
    }

    "calculate the mean duration" in {
      "empty list" in {
        MeanDurationStat(List()) must beNone
      }

      "single element list" in {
        MeanDurationStat(List(5.seconds)) must beSome(5.seconds)
      }

      "even list" in {
        MeanDurationStat(List(10.seconds, 21.seconds)) must beSome(15500.milliseconds) // 15.5 secs
      }

      "odd list" in {
        MeanDurationStat(List(1.millisecond, 2.seconds, 4.milliseconds)) must beSome(668.milliseconds) // 668.3333
      }
    }

    "calculate the median duration" in {
      "empty list" in {
        MedianDurationStat(List()) must beNone
      }

      "single element list" in {
        MedianDurationStat(List(5.seconds)) must beSome(5.seconds)
      }

      "even list" in {
        MedianDurationStat(List(10.seconds, 20.seconds, 30.seconds, 1.millisecond)) must beSome(15.seconds)
      }

      "odd list" in {
        MedianDurationStat(List(1.millisecond, 2.seconds, 4.milliseconds)) must beSome(4.milliseconds)
      }
    }
  }
}
