package persistence.dal

import persistence.entities.Job
import utils.BeforeAllAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class JobsDALTest extends AbstractPersistenceTest with BeforeAllAfterAll {
  sequential

  lazy val modules = new Modules {}

  override def beforeAll() = {
    modules.jobsDal.createTables()
  }

  "Jobs DAL" should {

    "no jobs on get" in {
      modules.jobsDal.getJobs() must haveSize[Seq[Job]](0).await
    }

    "return 1 on save" in {
      modules.jobsDal.save(Job(None, Some(1), Some(1), "name", "source", "format", 1.0)) must beEqualTo(1).await
    }

    "return valid job on get" in {
      val job: Option[Job] = Await.result(modules.jobsDal.getJobById(1), Duration(5, SECONDS))
      job must beSome
      job.get.id must beSome(1)
      job.get.projId must beSome(1)
      job.get.testId must beSome(1)
      job.get.jobName === "name"
      job.get.source === "source"
      job.get.format === "format"
      job.get.value must beCloseTo(1.0 within 2.significantFigures)
    }

    "return no jobs on bad get" in {
      modules.jobsDal.getJobById(2) must beNone.await
      modules.jobsDal.getJobsByTestId(3) must haveSize[Seq[Job]](0).await
    }

    "return 2 jobs after inserting another one" in {
      modules.jobsDal.save(Job(None, Some(1), Some(1), "name", "source", "format", 1.0)) must beEqualTo(2).await
      modules.jobsDal.getJobs() must haveSize[Seq[Job]](2).await
      modules.jobsDal.getJobsByTestId(1) must haveSize[Seq[Job]](2).await
    }

    "getJobsByTestId filters properly" in {
      modules.jobsDal.save(Job(None, Some(1), Some(2), "name2.1", "source", "format", 1.0)) must beEqualTo(3).await
      modules.jobsDal.save(Job(None, Some(1), Some(2), "name2.2", "source", "format", 1.0)) must beEqualTo(4).await
      modules.jobsDal.save(Job(None, Some(1), Some(3), "name3.1", "source", "format", 1.0)) must beEqualTo(5).await

      modules.jobsDal.getJobsByTestId(2) must haveSize[Seq[Job]](2).await
      modules.jobsDal.getJobsByTestId(3) must haveSize[Seq[Job]](1).await
      modules.jobsDal.getJobsByTestId(4) must haveSize[Seq[Job]](0).await
    }

  }

  override def afterAll() = {
    modules.db.close()
  }
}
