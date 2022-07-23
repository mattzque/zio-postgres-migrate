package com.mattzq.migrate
package service

import zio.{ ZLayer, RLayer }
import scala.util.Properties
import com.mattzq.migrate.entity.DBSettings

sealed trait DBSettingsService:
  def url: String

class DBSettingsServiceImpl(
    private val settings: DBSettings,
  ) extends DBSettingsService:
  override def url: String =
    settings.url

object DBSettingsServiceImpl:
  def createFromEnv: DBSettingsService =
    DBSettingsServiceImpl(
      DBSettings(
        database = Properties.envOrElse("POSTGRES_DB", "postgres"),
        username = Properties.envOrElse("POSTGRES_USER", "postgres"),
        password = Properties.envOrElse("POSTGRES_PASSWORD", "postgres"),
        hostname = Properties.envOrElse("POSTGRES_HOST", "localhost"),
        port = Properties.envOrElse("POSTGRES_PORT", "5432").toInt,
        ssl = Properties.envOrElse("POSTGRES_SSL", "false").toBoolean,
      )
    )

  lazy val layerFromEnv: RLayer[Any, DBSettingsService] =
    ZLayer.succeed(createFromEnv)
