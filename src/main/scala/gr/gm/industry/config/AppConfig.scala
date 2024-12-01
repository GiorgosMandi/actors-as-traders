package gr.gm.industry.config
import pureconfig._
import pureconfig.generic.auto._

case class DbConfig(uri: String, username: String, password: String)
case class AppConfig(db: DbConfig)

object AppConfig {
  def load(): AppConfig = {
    ConfigSource.default.loadOrThrow[AppConfig]
  }
}
