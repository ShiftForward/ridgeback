package persistence.entities

import java.sql.Timestamp
import java.time.{ ZoneId, ZonedDateTime, LocalDateTime }

import utils.Profile

case class Test(
  id: Option[Int],
  projId: Option[Int],
  commit: String,
  startDate: Option[ZonedDateTime],
  endDate: Option[ZonedDateTime])

case class SimpleTest(
  projId: Option[Int],
  commit: String)

trait Tests extends Profile {
  import profile.api._

  // A Tests table with 5 columns: id, project id, commit sha hash, start timestamp and end timestamp
  class Tests(tag: Tag) extends Table[Test](tag, "Tests") {

    implicit def dateTime =
      MappedColumnType.base[ZonedDateTime, Timestamp](
        dt => Timestamp.valueOf(dt.toLocalDateTime),
        ts => ts.toLocalDateTime.atZone(ZoneId.systemDefault()))

    def id = column[Int]("TEST_ID", O.PrimaryKey, O.AutoInc)
    def projId = column[Int]("PROJ_ID")
    def commit = column[String]("COMMIT")
    def startDate = column[Option[ZonedDateTime]]("START_DATE")
    def endDate = column[Option[ZonedDateTime]]("END_DATE")

    def * = (id.?, projId.?, commit, startDate, endDate) <> (Test.tupled, Test.unapply)
    // TODO def project = foreignKey("PROJ_FK", projId, TableQuery[Projects])(_.id)
  }

  var tests = TableQuery[Tests]
}
