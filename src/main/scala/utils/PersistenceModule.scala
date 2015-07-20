package utils

import persistence.dal._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile

trait Profile {
  val profile: JdbcProfile
}

trait DbModule extends Profile {
  val db: JdbcProfile#Backend#Database
}

trait PersistenceModule {
  val projectsDal: ProjectsDal
  val testsDal: TestsDal
  val jobsDal: JobsDal
}

trait PersistenceModuleImpl extends PersistenceModule with DbModule {
  this: Configuration =>

  // use an alternative database configuration ex:
  // private val dbConfig : DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("pgdb")
  private val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("h2db")

  override implicit val profile: JdbcProfile = dbConfig.driver
  override implicit val db: JdbcProfile#Backend#Database = dbConfig.db

  override val projectsDal = new ProjectsDalImpl()
  override val testsDal = new TestsDalImpl()
  override val jobsDal = new JobsDalImpl()
}
