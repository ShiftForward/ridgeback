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
  }
}
