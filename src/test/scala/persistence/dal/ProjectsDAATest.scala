package persistence.dal

import akka.util.Timeout
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import persistence.entities.Project

import scala.concurrent.Await
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ProjectsDAATest extends FunSuite with AbstractPersistenceTest with BeforeAndAfterAll {
  implicit val timeout = Timeout(5.seconds)

  val modules = new Modules {
  }

  test("ProjectsActor: Testing Projects Actor") {
    Await.result(modules.projectsDal.createTables(),5.seconds)
    val numberOfEntities : Int = Await.result(modules.projectsDal.save(Project(None, "projName", "gitRepo")), 5.seconds)
    assert (numberOfEntities == 1)
    val supplier : Seq[Project] = Await.result(modules.projectsDal.getProjectById(1), 5.seconds)
    assert (supplier.length == 1)
    assert (supplier.head.name.compareTo("projName") == 0)
    assert (supplier.head.gitRepo.compareTo("gitRepo") == 0)
    val empty : Seq[Project] = Await.result(modules.projectsDal.getProjectById(2), 5.seconds)
    assert (empty.isEmpty)
  }

  override def afterAll(): Unit ={
    modules.db.close()
  }
}
