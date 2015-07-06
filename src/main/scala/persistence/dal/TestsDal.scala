package persistence.dal

import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ Test, Tests }
import slick.driver.JdbcProfile
import utils.DbModule

import scala.concurrent.Future

trait TestsDal {
  def save(test: Test): Future[Int]
  def getTests(): Future[Seq[Test]]
  def getTestsByProjId(projId: Int): Future[Seq[Test]]
  def getTestById(id: Int): Future[Option[Test]]
  def createTables(): Future[Unit]
}

class TestsDalImpl(implicit val db: JdbcProfile#Backend#Database, implicit val profile: JdbcProfile) extends TestsDal with DbModule with Tests with LazyLogging {
  import profile.api._

  override def save(test: Test): Future[Int] = { db.run(tests += test) }

  override def getTests(): Future[Seq[Test]] = { db.run(tests.result) }

  override def getTestsByProjId(projId: Int): Future[Seq[Test]] = { db.run(tests.filter(_.projId === projId).result) }

  override def getTestById(id: Int): Future[Option[Test]] = { db.run(tests.filter(_.id === id).result.headOption) }

  override def createTables(): Future[Unit] = {
    db.run(DBIO.seq(tests.schema.create))
  }
}
