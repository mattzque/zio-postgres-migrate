package com.mattzq
package migrate

import scala.util.Properties

case class Settings(
    database: String,
    username: String,
    password: String,
    hostname: String,
    port: Int,
    ssl: Boolean,
  ):
  def url: String =
    s"jdbc:postgresql://$hostname:$port/$database?user=$username&password=$password&ssl=$ssl"

object Settings:
  def getFromEnv: Settings =
    Settings(
      database = Properties.envOrElse("POSTGRES_DB", "postgres"),
      username = Properties.envOrElse("POSTGRES_USER", "postgres"),
      password = Properties.envOrElse("POSTGRES_PASSWORD", "postgres"),
      hostname = Properties.envOrElse("POSTGRES_HOST", "localhost"),
      port = Properties.envOrElse("POSTGRES_PORT", "5432").toInt,
      ssl = Properties.envOrElse("POSTGRES_SSL", "false").toBoolean,
    )
