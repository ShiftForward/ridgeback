package persistence.entities

import utils.Profile

case class Job(
  id: Option[Int],
  projId: Option[Int],
  testId: Option[Int],
  jobName: String,
  metricName: String,
  value: Double)

case class SimpleJob(
  projId: Option[Int],
  testId: Option[Int],
  jobName: String,
  metricName: String,
  value: Double)

trait Jobs extends Profile {
  import profile.api._

  class Jobs(tag: Tag) extends Table[Job](tag, "Jobs") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def projId = column[Int]("projId")
    def testId = column[Int]("testId")
    def jobName = column[String]("jobName")
    def metricName = column[String]("metricName")
    def value = column[Double]("value")

    def * = (id.?, projId.?, testId.?, jobName, metricName, value) <> (Job.tupled, Job.unapply)
    // def project = foreignKey("job_project", projId, projectsTable)(_.id, onDelete = ForeignKeyAction.Cascade)
    // def test = foreignKey("job_test", testId, testsTable)(_.id, onDelete = ForeignKeyAction.Cascade)
  }
}
