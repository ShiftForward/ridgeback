package api

import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.scalalogging.LazyLogging
import core.ConsoleEventPublisher
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import persistence.dal.{ JobsDal, ProjectsDal, TestsDal }
import spray.testkit.{ RouteTest, Specs2Interface }
import utils.{ ActorModule, ConfigurationModuleImpl, PersistenceModule }

trait Specs2RouteTest extends RouteTest with Specs2Interface

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
