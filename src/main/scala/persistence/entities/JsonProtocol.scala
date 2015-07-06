package persistence.entities

import spray.json.DefaultJsonProtocol
import java.sql.Timestamp

import spray.json._

object JsonProtocol extends DefaultJsonProtocol {
  implicit object TimestampJsonFormat extends JsonFormat[Timestamp] {
    def write(x: Timestamp) = JsNumber(x.getTime)
    def read(value: JsValue) = value match {
      case JsNumber(x) => new Timestamp(x.longValue())
      case x => deserializationError("Expected Timestamp as JsNumber, but got " + x)
    }
  }

  implicit val projectFormat = jsonFormat3(Project)
  implicit val testFormat = jsonFormat5(Test)
  implicit val simpleProjectFormat = jsonFormat2(SimpleProject)
  implicit val simpleTestFormat = jsonFormat2(SimpleTest)
}
