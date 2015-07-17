package utils.json

import spray.json._

import scala.reflect.ClassTag
import scala.util.Try

object Implicits {

  final implicit class RichJSValue(val json: JsValue) extends AnyVal {

    /**
     * Get's a field from a dot-separated path (eg. `root.node.leaf`)
     *
     * @param path The dot separated path
     * @tparam JV The type of JSON value to return
     * @return The JSON field value (wrapped in an option)
     */
    def getPath[JV <: JsValue: ClassTag](path: String): Option[JV] =
      getPath(path.split("\\.").toList)

    def getPath[JV <: JsValue: ClassTag](pathElements: List[String]): Option[JV] =
      (pathElements, json) match {
        case (Nil, jv: JV) => Some(jv)
        case (elem :: rem, JsObject(fields)) => fields.get(elem).flatMap(_.getPath(rem))
        case (ToInt(i) :: rem, JsArray(elems)) => elems.drop(i).headOption.flatMap(_.getPath(rem))
        case _ => None
      }

    /**
     * Unwraps a JSON value. If the given value is a JSON string, number or
     * boolean, a `String`, `BigDecimal` or `Boolean` is returned
     * respectively. If the given value is a JSON array or object, a `List[Any]`
     * or `Map[String, Any]` is returned respectively, where each of the values
     * is recursively unwrapped. If the given value is a JSON null, `null` is
     * returned.
     * @return the unwrapped JSON value.
     */
    def toValue: Any = json match {
      case JsString(str) => str
      case JsNumber(num) => num
      case JsObject(map) => map.mapValues(_.toValue).map(identity)
      case JsArray(elems) => elems.map(_.toValue)
      case JsBoolean(bool) => bool
      case JsNull => null
    }
  }
}

object ToInt {
  def unapply(str: String) = Try(str.toInt).toOption
}
