package persistence.dal

import org.specs2.time.NoTimeConversions
import persistence.entities.Project
import specUtils.BeforeAllAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class ProjectsDALSpec extends AbstractPersistenceSpec with BeforeAllAfterAll with NoTimeConversions {
  sequential

  lazy val modules = new Modules {}

  override def beforeAll() = {
    Await.result(modules.projectsDal.createTables(), 5.seconds)
  }

  "Project DAL" should {

    "return 1 on save" in {
      modules.projectsDal.save(Project(None, "projName", "gitRepo")) must beEqualTo(1).await
    }

    "return valid project on get" in {
      val project: Option[Project] = Await.result(modules.projectsDal.getProjectById(1), 5.seconds)
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

    "search by repo name" in {
      val project = Await.result(modules.projectsDal.getProjectByGitRepo("gitRepo"), 5.seconds)
      project.get.id must beSome(1)
    }
  }

  override def afterAll() = {
    modules.db.close()
  }
}
