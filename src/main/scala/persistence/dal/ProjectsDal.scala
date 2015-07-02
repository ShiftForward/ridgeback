package persistence.dal

import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{Project, Projects}
import slick.driver.JdbcProfile
import utils.DbModule

import scala.concurrent.Future

trait ProjectsDal {
  def save(proj: Project) : Future[Int]
  def getProjects() : Future[Vector[Project]]
  def getProjectById(id: Int) : Future[Vector[Project]]
  def createTables() : Future[Unit]
}

class ProjectsDalImpl(implicit val db: JdbcProfile#Backend#Database, implicit val profile: JdbcProfile) extends ProjectsDal with DbModule with Projects with LazyLogging {
  import profile.api._

  override def save(proj: Project) : Future[Int] = {  db.run(projects += proj).mapTo[Int] }

  override def getProjects() : Future[Vector[Project]] = { db.run(projects.result).mapTo[Vector[Project]] }

  override def getProjectById(id: Int) : Future[Vector[Project]] = { db.run(projects.filter(_.id === id).result).mapTo[Vector[Project]] }

  override def createTables() : Future[Unit] = {
    db.run(DBIO.seq(projects.schema.create))
  }
}
