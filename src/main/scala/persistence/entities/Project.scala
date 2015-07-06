package persistence.entities

import utils.Profile

case class Project(
  id: Option[Int],
  name: String,
  gitRepo: String
)

case class SimpleProject(
  name: String,
  gitRepo: String
)

trait Projects extends Profile {
  import profile.api._

  // A Projects table with 3 columns: id, name, git repo url
  class Projects(tag: Tag) extends Table[Project](tag, "Projects") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def gitRepo = column[String]("gitRepo")

    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id.?, name, gitRepo) <> (Project.tupled, Project.unapply)
  }

  val projects = TableQuery[Projects]
}
