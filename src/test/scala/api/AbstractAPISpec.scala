package api

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import core.ConsoleEventPublisher
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import persistence.dal.{ JobsDal, TestsDal, ProjectsDal }
import spray.testkit.Specs2RouteTest
import utils.{ ActorModule, ConfigurationModuleImpl, PersistenceModule }

trait AbstractAPISpec extends Specification with Specs2RouteTest with Mockito {

  trait Modules extends ConfigurationModuleImpl with ActorModule with PersistenceModule
      with ConsoleEventPublisher with LazyLogging {
    val system = AbstractAPISpec.this.system

    override val projectsDal = mock[ProjectsDal]
    override val testsDal = mock[TestsDal]
    override val jobsDal = mock[JobsDal]

    override def config = getConfig.withFallback(super.config)
  }

  def getConfig: Config = ConfigFactory.empty()
}
