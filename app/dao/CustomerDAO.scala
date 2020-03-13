package dao

import java.sql.Timestamp
import java.time.{ LocalDateTime, ZoneId }

import com.typesafe.config.{ Config, ConfigFactory }
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery
import play.Logger

object CustomerDAO {
  def currentTimestamp: Timestamp = Timestamp.valueOf(LocalDateTime.now(ZoneId.systemDefault()))
  val config: Config = ConfigFactory.load()
  val JdbcProfile: JdbcProfile = try {
    val name = config.getString("slick.dbs.default.profile")
    Logger.debug(s"Loading JdbcProfile: $name")
    val clazz = Thread.currentThread().getContextClassLoader.loadClass(name)
    clazz.getField("MODULE$").get(clazz).asInstanceOf[JdbcProfile]
  } catch {
    case ex: Throwable ⇒ throw new java.util.ServiceConfigurationError("Could not get JdbcProfile from configuration.", ex)
  }

  case class Row(
      id:        Int       = 0,
      email:     String,
      stripeId:  String,
      userName:  String,
      isDeleted: Boolean   = false,
      createTs:  Timestamp = currentTimestamp,
      updateTs:  Timestamp = currentTimestamp
  )

  val table = TableQuery[TableDef]

  import JdbcProfile.api._

  final class TableDef(tag: Tag) extends Table[Row](tag, "c_customer") {
    val id = column[Int]("id", O.AutoInc, O.PrimaryKey)
    val email = column[String]("email", O.Length(256), O.Unique)
    val stripeId = column[String]("stripe_id", O.Length(256), O.Unique)
    val userName = column[String]("user_name", O.Length(256))
    val isDeleted = column[Boolean]("is_deleted")
    val createTs = column[Timestamp]("create_ts")
    val updateTs = column[Timestamp]("update_ts")

    def * = (
      id,
      email,
      stripeId,
      userName,
      isDeleted,
      createTs,
      updateTs
    ).mapTo[Row]
  }

  def +=(row: Row): DBIO[Row] = table returning table.map(_.id) into ((row, id) ⇒ row.copy(id = id)) += row
  def nonDeleted = table.filter(_.isDeleted === false)
  def findByEmail(email: String): DBIO[Option[Row]] = nonDeleted.filter(_.email === email).take(1).result.headOption

}

