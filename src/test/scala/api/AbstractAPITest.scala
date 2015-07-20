package api

import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import persistence.dal.{ JobsDal, TestsDal, ProjectsDal }
import spray.testkit.Specs2RouteTest
import utils.{ ActorModule, ConfigurationModuleImpl, PersistenceModule }

trait AbstractAPITest extends Specification with Specs2RouteTest with Mockito {

  trait Modules extends ConfigurationModuleImpl with ActorModule with PersistenceModule {
    val system = AbstractAPITest.this.system

    override val projectsDal = mock[ProjectsDal]
    override val testsDal = mock[TestsDal]
    override val jobsDal = mock[JobsDal]

    override def config = getConfig.withFallback(super.config)
  }

  def getConfig: Config = ConfigFactory.empty()
}
