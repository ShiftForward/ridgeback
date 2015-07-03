package persistence.dal

import persistence.entities.Project
import utils.BeforeAllAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectsDALTest extends AbstractPersistenceTest with BeforeAllAfterAll {
  sequential

  lazy val modules = new Modules {}

  override def beforeAll() = {
    modules.projectsDal.createTables()
  }

  "Project DAL" should {

    "return 1 on save" in {
      modules.projectsDal.save(Project(None, "projName", "gitRepo")) must beEqualTo (1).await
    }

    "return valid project on get" in {
      val supplier : Option[Project] = Await.result(modules.projectsDal.getProjectById(1), Duration(5, SECONDS))
      supplier must beSome
      supplier.get.name === "projName"
      supplier.get.gitRepo === "gitRepo"
    }

    "return no projects on bad get" in {
      modules.projectsDal.getProjectById(2) must beNone.await
    }

    "return 2 projects after inserting another one" in {
      modules.projectsDal.save(Project(None, "projName", "gitRepo")) must beEqualTo (1).await
      modules.projectsDal.getProjects() must haveSize[Vector[Project]](2).await
    }

  }

  override def afterAll() = {
    modules.db.close()
  }
}
