package com.mattzq
package migrate

import java.sql.Connection
import java.sql.DriverManager
import scala.util.Try
import scala.util.Failure
import scala.util.Success

class DatabaseConnection(private val connection: Connection):
  def hasTable(table: String): Try[Boolean] =
    connection
      .prepareStatement("SELECT to_regclass(?);")
      .toFailure(Exception("Error preparing statement!"))
      .flatMap { stmt =>
        stmt.setString(1, table)
        stmt
          .executeQuery()
          .toFailure(Exception("Error executing statement!"))
          .map { resultSet =>
            if resultSet.next() then resultSet.getString(1) != null
            else false
          }
      }

object DatabaseConnection:
  def connect(settings: Settings): Option[DatabaseConnection] =
    DriverManager
      .getConnection(settings.url)
      .toOption
      .map(DatabaseConnection(_))
