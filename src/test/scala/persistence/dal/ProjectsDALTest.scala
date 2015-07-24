package persistence.dal

import persistence.entities.Project
import utils.BeforeAllAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectsDALTest extends AbstractPersistenceTest with BeforeAllAfterAll {
  sequential

  lazy val modules = new Modules {}

  override def beforeAll() = {
    Await.result(modules.projectsDal.createTables(), Duration(5, SECONDS))
  }

  "Project DAL" should {

    "return 1 on save" in {
      modules.projectsDal.save(Project(None, "projName", "gitRepo")) must beEqualTo(1).await
    }

    "return valid project on get" in {
      val project: Option[Project] = Await.result(modules.projectsDal.getProjectById(1), Duration(5, SECONDS))
      project must beSome
      project.get.name === "projName"
      project.get.gitRepo === "gitRepo"
    }

    "return no projects on bad get" in {
      modules.projectsDal.getProjectById(2) must beNone.await
    }

    "return 2 projects after inserting another one" in {
      modules.projectsDal.save(Project(None, "projName", "gitRepo")) must beEqualTo(2).await
      modules.projectsDal.getProjects() must haveSize[Seq[Project]](2).await
    }

  }

  override def afterAll() = {
    modules.db.close()
  }
}
