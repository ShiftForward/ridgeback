package persistence.entities

import java.sql.Timestamp

import utils.Profile

case class Test(
  id: Option[Int],
  projId: Option[Int],
  commit: String,
  startDate: Option[Timestamp],
  endDate: Option[Timestamp])

case class SimpleTest(
  projId: Option[Int],
  commit: String)

trait Tests extends Profile {
  import profile.api._

  // A Tests table with 5 columns: id, project id, commit sha hash, start timestamp and end timestamp
  class Tests(tag: Tag) extends Table[Test](tag, "Tests") {

    def id = column[Int]("TEST_ID", O.PrimaryKey, O.AutoInc)
    def projId = column[Int]("PROJ_ID")
    def commit = column[String]("COMMIT")
    def startDate = column[Option[Timestamp]]("START_DATE")
    def endDate = column[Option[Timestamp]]("END_DATE")

    def * = (id.?, projId.?, commit, startDate, endDate) <> (Test.tupled, Test.unapply)
    // TODO def project = foreignKey("PROJ_FK", projId, TableQuery[Projects])(_.id)
  }

  var tests = TableQuery[Tests]
}
