package persistence.dal

import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ Job, Jobs }
import slick.driver.JdbcProfile
import utils.DbModule

import scala.concurrent.Future

trait JobsDal {
  def save(job: Job): Future[Int]
  def getJobs(): Future[Seq[Job]]
  def getJobsByTestId(testId: Int): Future[Seq[Job]]
  def getJobById(id: Int): Future[Option[Job]]
  def createTables(): Future[Unit]
}

class JobsDalImpl(implicit val db: JdbcProfile#Backend#Database, implicit val profile: JdbcProfile) extends JobsDal with DbModule with Jobs with LazyLogging {
  import profile.api._

  override def save(job: Job): Future[Int] = db.run(jobs += job)

  override def getJobs(): Future[Seq[Job]] = db.run(jobs.result)

  override def getJobsByTestId(testId: Int): Future[Seq[Job]] = db.run(jobs.filter(_.testId === testId).result)

  override def getJobById(id: Int): Future[Option[Job]] = db.run(jobs.filter(_.id === id).result.headOption)

  override def createTables(): Future[Unit] = db.run(DBIO.seq(jobs.schema.create))
}
