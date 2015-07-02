package persistence.entities

import persistence.entities.{SimpleProject, Project}
import spray.json.DefaultJsonProtocol

object JsonProtocol extends DefaultJsonProtocol {
  implicit val projectFormat = jsonFormat3(Project)
  implicit val simpleProjectFormat = jsonFormat2(SimpleProject)
}
