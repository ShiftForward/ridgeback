import java.sql.Timestamp

import slick.driver.H2Driver.api._
import slick.lifted.{ProvenShape, ForeignKeyQuery}

// A Projects table with 3 columns: id, name, git repo url
class Projects(tag: Tag)
  extends Table[(Int, String, String)](tag, "PROJECTS") {

  def id: Rep[Int] = column[Int]("PROJ_ID", O.PrimaryKey, O.AutoInc)
  def name: Rep[String] = column[String]("PROJ_NAME")
  def gitRepo: Rep[String] = column[String]("GIT_REPO")

  // Every table needs a * projection with the same type as the table's type parameter
  def * : ProvenShape[(Int, String, String)] =
    (id, name, gitRepo)
}

// A Tests table with 5 columns: id, project id, commit sha hash, start timestamp and end timestamp
class Tests(tag: Tag)
  extends Table[(Int, Int, String, Timestamp, Timestamp)](tag, "TESTS") {

  def id: Rep[Int] = column[Int]("TEST_ID", O.PrimaryKey, O.AutoInc)
  def projId: Rep[Int] = column[Int]("PROJ_ID")
  def commit: Rep[String] = column[String]("COMMIT")
  def startDate: Rep[Timestamp] = column[Timestamp]("START_DATE")
  def endDate: Rep[Timestamp] = column[Timestamp]("END_DATE")

  def * : ProvenShape[(Int, Int, String, Timestamp, Timestamp)] =
    (id, projId, commit, startDate, endDate)

  // A reified foreign key relation that can be navigated to create a join
  def supplier: ForeignKeyQuery[Projects, (Int, String, String)] =
    foreignKey("PROJ_FK", projId, TableQuery[Projects])(_.id)
}

// A Jobs table with 5 columns: id, test id, job name, metric name and value
class Jobs(tag: Tag)
  extends Table[(String, Int, Double, Int, Int)](tag, "METRICS") {

  def id: Rep[Int] = column[Int]("JOB_ID", O.PrimaryKey, O.AutoInc)
  def testId: Rep[Int] = column[Int]("TEST_ID")
  def jobName: Rep[String] = column[String]("JOB_NAME")
  def metricName: Rep[String] = column[String]("METRIC_NAME")
  def value: Rep[Double] = column[Double]("VALUE")

  def * : ProvenShape[(Int, Int, String, String, Double)] =
    (id, testId, jobName, metricName, value)

  // A reified foreign key relation that can be navigated to create a join
  def supplier: ForeignKeyQuery[Tests, (Int, Int, String, Timestamp, Timestamp)] =
    foreignKey("TEST_FK", testId, TableQuery[Tests])(_.id)
}
