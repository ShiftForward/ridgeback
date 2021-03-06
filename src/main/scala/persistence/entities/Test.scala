package persistence.entities

import java.sql.Timestamp
import java.time.{ ZoneId, ZonedDateTime, LocalDateTime }

import utils.Profile

case class Test(
  id: Option[Int],
  projId: Option[Int],
  commit: String,
  branch: Option[String],
  prId: Option[Int],
  localDir: Option[String],
  startDate: Option[ZonedDateTime],
  endDate: Option[ZonedDateTime])

trait Tests extends Profile {
  import profile.api._

  class Tests(tag: Tag) extends Table[Test](tag, "Tests") {

    implicit def dateTime =
      MappedColumnType.base[ZonedDateTime, Timestamp](
        dt => Timestamp.valueOf(dt.toLocalDateTime),
        ts => ts.toLocalDateTime.atZone(ZoneId.systemDefault()))

    def id = column[Int]("testId", O.PrimaryKey, O.AutoInc)
    def projId = column[Int]("projId")
    def commit = column[String]("commit")
    def branch = column[Option[String]]("branch")
    def prId = column[Option[Int]]("prId")
    def localDir = column[Option[String]]("localDir")
    def startDate = column[Option[ZonedDateTime]]("startDate")
    def endDate = column[Option[ZonedDateTime]]("endDate")

    def * = (id.?, projId.?, commit, branch, prId, localDir, startDate, endDate) <> (Test.tupled, Test.unapply)
    // def project = foreignKey("PROJ_FK", projId, TableQuery[Projects])(_.id)
  }

  var tests = TableQuery[Tests]
}
