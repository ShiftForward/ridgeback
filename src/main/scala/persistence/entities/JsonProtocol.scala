package persistence.entities

import java.time.format.DateTimeFormatter
import java.time.{ ZonedDateTime }

import spray.json.DefaultJsonProtocol

import spray.json._

import scala.concurrent.duration._

object JsonProtocol extends DefaultJsonProtocol {
  implicit object ZonedDateTimeJsonFormat extends JsonFormat[ZonedDateTime] {
    def write(x: ZonedDateTime) = JsString(x.format(DateTimeFormatter.ISO_INSTANT))
    def read(value: JsValue) = value match {
      case JsString(x) => ZonedDateTime.parse(x)
      case x => deserializationError("Expected LocalDateTime as JsString, but got " + x)
    }
  }

  implicit object DurationJsonFormat extends JsonFormat[Duration] {
    def write(x: Duration) = JsNumber(x.toMillis)
    def read(value: JsValue) = value match {
      case JsNumber(x) => x.toLongExact.milliseconds
      case x => deserializationError("Expected Long as JsNumber, but got " + x)
    }
  }

  implicit val projectFormat = jsonFormat3(Project)
  implicit val testFormat = jsonFormat8(Test)
  implicit val jobFormat = jsonFormat6(Job)
  implicit val simpleProjectFormat = jsonFormat2(SimpleProject)
  implicit val simpleTestFormat = jsonFormat5(SimpleTest)
  implicit val simpleJobFormat = jsonFormat5(SimpleJob)
}
