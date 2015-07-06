package persistence.dal

import org.specs2.mutable.Specification
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import utils._

trait AbstractPersistenceTest extends Specification  {

  trait Modules extends ConfigurationModuleImpl with PersistenceModule with DbModule {

    private lazy val dbConfig : DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("h2test")

    override implicit lazy val profile: JdbcProfile = dbConfig.driver
    override implicit lazy val db: JdbcProfile#Backend#Database = dbConfig.db

    override lazy val projectsDal = new ProjectsDalImpl()
    override lazy val testsDal = new TestsDalImpl()
  }
}
