package persistence.dal

import org.specs2.time.NoTimeConversions
import persistence.entities.Job
import specUtils.BeforeAllAfterAll

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
      modules.jobsDal.save(Job(None, Some(1), Some(1), "name", "source", None, List(1.seconds, 2.seconds))) must beEqualTo(1).await
    }

    "return valid job on get" in {
      val job: Option[Job] = Await.result(modules.jobsDal.getJobById(1), 5.seconds)
      job must beSome
      job.get.id must beSome(1)
      job.get.projId must beSome(1)
      job.get.testId must beSome(1)
      job.get.jobName === "name"
      job.get.source === "source"
      job.get.durations === List(1.seconds, 2.seconds)
    }

    "return no jobs on bad get" in {
      modules.jobsDal.getJobById(2) must beNone.await
      modules.jobsDal.getJobsByTestId(3) must haveSize[Seq[Job]](0).await
    }

    "return 2 jobs after inserting another one" in {
      modules.jobsDal.save(Job(None, Some(1), Some(1), "name", "source", None, List(1.seconds))) must beEqualTo(2).await
      modules.jobsDal.getJobs() must haveSize[Seq[Job]](2).await
      modules.jobsDal.getJobsByTestId(1) must haveSize[Seq[Job]](2).await
    }

    "getJobsByTestId filters properly" in {
      modules.jobsDal.save(Job(None, Some(1), Some(2), "name2.1", "source", None, List(1.seconds))) must beEqualTo(3).await
      modules.jobsDal.save(Job(None, Some(1), Some(2), "name2.2", "source", None, List(1.seconds))) must beEqualTo(4).await
      modules.jobsDal.save(Job(None, Some(1), Some(3), "name3.1", "source", None, List(1.seconds))) must beEqualTo(5).await

      modules.jobsDal.getJobsByTestId(2) must haveSize[Seq[Job]](2).await
      modules.jobsDal.getJobsByTestId(3) must haveSize[Seq[Job]](1).await
      modules.jobsDal.getJobsByTestId(4) must haveSize[Seq[Job]](0).await
    }

    "return past jobs" in {
      val jobIds = Await.result(for {
        jobId1 <- modules.jobsDal.save(Job(None, Some(5), Some(1), "name", "source", None, List(1.seconds)))
        jobId2 <- modules.jobsDal.save(Job(None, Some(5), Some(2), "name", "source", None, List(2.seconds)))
        jobId3 <- modules.jobsDal.save(Job(None, Some(6), Some(2), "name", "source", None, List(2.seconds)))
        jobId4 <- modules.jobsDal.save(Job(None, Some(5), Some(3), "name2", "source", None, List(3.seconds)))
        jobId5 <- modules.jobsDal.save(Job(None, Some(5), Some(3), "name", "source", None, List(3.seconds)))
      } yield (jobId1, jobId2, jobId3, jobId4, jobId5), 5.seconds)

      val jobs = Await.result(for {
        job1 <- modules.jobsDal.getJobById(jobIds._1)
        job2 <- modules.jobsDal.getJobById(jobIds._2)
        job3 <- modules.jobsDal.getJobById(jobIds._3)
        job4 <- modules.jobsDal.getJobById(jobIds._4)
        job5 <- modules.jobsDal.getJobById(jobIds._5)
      } yield (job1, job2, job3, job4, job5), 5.seconds)

      val expectedPastJobs = Seq(jobs._5.get, jobs._2.get)

      modules.jobsDal.getPastJobs(jobs._1.get) must beEqualTo(expectedPastJobs).await
    }

  }

  override def afterAll() = {
    modules.db.close()
  }
}
