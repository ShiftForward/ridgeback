package utils

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import scala.concurrent.duration._

class RichDurationSpec extends Specification with NoTimeConversions {
  import utils.RichDuration._

  "RichDuration should" in {
    "write short strings" in {
      1.seconds.toShortString === "1s"
      2.seconds.toShortString === "2s"
      100.milliseconds.toShortString === "100ms"
    }

    "compare with threshold" in {
      val d1 = 2500.milliseconds
      val d2 = 2501.milliseconds
      val d3 = 3000.milliseconds
      val d4 = 3.seconds

      d1.compareWithThreshold(d2, 10) must be_==(0)
      d1.compareWithThreshold(d3, 50) must be_==(0)
      d3.compareWithThreshold(d4, 10) must be_==(0)
      d2.compareWithThreshold(d3, 10) must be_<(0)
      d3.compareWithThreshold(d2, 10) must be_>(0)
      d2.compareWithThreshold(d3, 100) must be_==(0)
    }
  }
}
