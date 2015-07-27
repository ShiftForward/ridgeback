package persistence.dal

import org.specs2.time.NoTimeConversions
import persistence.entities.Job
import utils.BeforeAllAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class JobsDALSpec extends AbstractPersistenceSpec with BeforeAllAfterAll with NoTimeConversions {
  sequential

  lazy val modules = new Modules {}

  override def beforeAll() = {
    Await.result(modules.jobsDal.createTables(), 5.seconds)
  }

  "Jobs DAL" should {

    "no jobs on get" in {
      modules.jobsDal.getJobs() must haveSize[Seq[Job]](0).await
    }

    "return 1 on save" in {
      modules.jobsDal.save(Job(None, Some(1), Some(1), "name", "source", 1.seconds)) must beEqualTo(1).await
    }

    "return valid job on get" in {
      val job: Option[Job] = Await.result(modules.jobsDal.getJobById(1), 5.seconds)
      job must beSome
      job.get.id must beSome(1)
      job.get.projId must beSome(1)
      job.get.testId must beSome(1)
      job.get.jobName === "name"
      job.get.source === "source"
      job.get.duration === 1.seconds
    }

    "return no jobs on bad get" in {
      modules.jobsDal.getJobById(2) must beNone.await
      modules.jobsDal.getJobsByTestId(3) must haveSize[Seq[Job]](0).await
    }

    "return 2 jobs after inserting another one" in {
      modules.jobsDal.save(Job(None, Some(1), Some(1), "name", "source", 1.seconds)) must beEqualTo(2).await
      modules.jobsDal.getJobs() must haveSize[Seq[Job]](2).await
      modules.jobsDal.getJobsByTestId(1) must haveSize[Seq[Job]](2).await
    }

    "getJobsByTestId filters properly" in {
      modules.jobsDal.save(Job(None, Some(1), Some(2), "name2.1", "source", 1.seconds)) must beEqualTo(3).await
      modules.jobsDal.save(Job(None, Some(1), Some(2), "name2.2", "source", 1.seconds)) must beEqualTo(4).await
      modules.jobsDal.save(Job(None, Some(1), Some(3), "name3.1", "source", 1.seconds)) must beEqualTo(5).await

      modules.jobsDal.getJobsByTestId(2) must haveSize[Seq[Job]](2).await
      modules.jobsDal.getJobsByTestId(3) must haveSize[Seq[Job]](1).await
      modules.jobsDal.getJobsByTestId(4) must haveSize[Seq[Job]](0).await
    }

  }

  override def afterAll() = {
    modules.db.close()
  }
}
