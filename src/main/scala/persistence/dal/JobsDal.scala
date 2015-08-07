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
  def getJobsByProjId(projId: Int): Future[Seq[Job]]
  def getJobById(id: Int): Future[Option[Job]]
  def getPastJobs(job: Job): Future[Seq[Job]]
  def createTables(): Future[Unit]
}

class JobsDalImpl(implicit val db: JdbcProfile#Backend#Database, implicit val profile: JdbcProfile) extends JobsDal with DbModule with Jobs with LazyLogging {
  import profile.api._

  override def save(job: Job): Future[Int] = db.run((jobs returning jobs.map(_.id)) += job)

  override def getJobs(): Future[Seq[Job]] = db.run(jobs.result)

  override def getJobsByTestId(testId: Int): Future[Seq[Job]] = db.run(jobs.filter(_.testId === testId).result)

  override def getJobsByProjId(projId: Int): Future[Seq[Job]] = db.run(jobs.filter(_.projId === projId).result)

  override def getJobById(id: Int): Future[Option[Job]] = db.run(jobs.filter(_.id === id).result.headOption)

  // recent jobs appear first
  override def getPastJobs(job: Job): Future[Seq[Job]] = {
    db.run(jobs.filter { j =>
      j.id =!= job.id &&
        j.projId === job.projId &&
        j.jobName === job.jobName
    }.sortBy(_.id.desc).result)
  }

  override def createTables(): Future[Unit] = db.run(jobs.schema.create)
}
