package persistence.dal

import persistence.entities.Test
import utils.BeforeAllAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class TestsDALTest extends AbstractPersistenceTest with BeforeAllAfterAll {
  sequential

  lazy val modules = new Modules {}

  override def beforeAll() = {
    modules.testsDal.createTables()
  }

  "Tests DAL" should {

    "no tests on get" in {
      modules.testsDal.getTests() must haveSize[Seq[Test]](0).await
    }

    "return 1 on save" in {
      modules.testsDal.save(Test(None, Some(1), "commit", None, None)) must beEqualTo(1).await
    }

    "return valid test on get" in {
      val test: Option[Test] = Await.result(modules.testsDal.getTestById(1), Duration(5, SECONDS))
      test must beSome
      test.get.id must beSome(1)
      test.get.projId must beSome(1)
      test.get.commit === "commit"
      test.get.startDate must beNone
      test.get.endDate must beNone
    }

    "return no tests on bad get" in {
      modules.testsDal.getTestById(2) must beNone.await
      modules.testsDal.getTestsByProjId(3) must haveSize[Seq[Test]](0).await
    }

    "return 2 tests after inserting another one" in {
      modules.testsDal.save(Test(None, Some(2), "commit", None, None)) must beEqualTo(2).await
      modules.testsDal.getTests() must haveSize[Seq[Test]](2).await
      modules.testsDal.getTestsByProjId(1) must haveSize[Seq[Test]](1).await
    }

    "save returns the new id" in {
      modules.testsDal.save(Test(None, Some(2), "commit", None, None)) must beEqualTo(3).await
      modules.testsDal.save(Test(None, Some(2), "commit", None, None)) must beEqualTo(4).await
    }
  }

  override def afterAll() = {
    modules.db.close()
  }
}
