package persistence.entities

import utils.Profile
import scala.concurrent.duration._

case class Job(
  id: Option[Int],
  projId: Option[Int],
  testId: Option[Int],
  jobName: String,
  source: String,
  threshold: Option[Int],
  durations: List[Duration])

case class SimpleJob(
  projId: Option[Int],
  testId: Option[Int],
  jobName: String,
  source: String,
  threshold: Option[Int],
  durations: List[Duration])

trait Jobs extends Profile {
  import profile.api._

  class Jobs(tag: Tag) extends Table[Job](tag, "Jobs") {

    implicit val durationsSlick =
      MappedColumnType.base[List[Duration], String](
        durations => durations.map(d => d.toMillis).mkString(","),
        str => str.split(',').map(s => s.toLong.milliseconds).toList)

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def projId = column[Int]("projId")
    def testId = column[Int]("testId")
    def jobName = column[String]("jobName")
    def source = column[String]("source")
    def threshold = column[Option[Int]]("threshold")
    def durations = column[List[Duration]]("values")

    def * = (id.?, projId.?, testId.?, jobName, source, threshold, durations) <> (Job.tupled, Job.unapply)
    // def project = foreignKey("job_project", projId, projectsTable)(_.id, onDelete = ForeignKeyAction.Cascade)
    // def test = foreignKey("job_test", testId, testsTable)(_.id, onDelete = ForeignKeyAction.Cascade)
  }

  var jobs = TableQuery[Jobs]
}
