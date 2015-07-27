package persistence.entities

import utils.Profile
import scala.concurrent.duration._

case class Job(
  id: Option[Int],
  projId: Option[Int],
  testId: Option[Int],
  jobName: String,
  source: String,
  duration: Duration)

case class SimpleJob(
  projId: Option[Int],
  testId: Option[Int],
  jobName: String,
  source: String,
  duration: Duration)

trait Jobs extends Profile {
  import profile.api._

  class Jobs(tag: Tag) extends Table[Job](tag, "Jobs") {

    implicit val durationSlick =
      MappedColumnType.base[Duration, Long](
        d => d.toMillis,
        l => l.milliseconds)

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def projId = column[Int]("projId")
    def testId = column[Int]("testId")
    def jobName = column[String]("jobName")
    def source = column[String]("source")
    def duration = column[Duration]("value")

    def * = (id.?, projId.?, testId.?, jobName, source, duration) <> (Job.tupled, Job.unapply)
    // def project = foreignKey("job_project", projId, projectsTable)(_.id, onDelete = ForeignKeyAction.Cascade)
    // def test = foreignKey("job_test", testId, testsTable)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  var jobs = TableQuery[Jobs]
}
