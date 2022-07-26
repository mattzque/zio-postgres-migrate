package com.mattzq
package migrate

import java.io.IOException
import java.nio.file.{ Files, Path, Paths }
import zio.{ Console, ZIO, ZIOAppDefault }

import java.sql.Connection
import scala.util.{ Failure, Properties, Success }
import service.*

import org.rogach.scallop.*

class Config(arguments: Seq[String]) extends ScallopConf(arguments):
  version("migrate 0.0.1")
  val migrationPath =
    trailArg[String](required = true, descr = "Path to directory with migration files.")
  verify()

object MigrateApp extends ZIOAppDefault:
  def program(path: Path) =
    MigrationRunnerService
      .run(path)
      .provideSome(
        DBSettingsServiceImpl.layerFromEnv,
        DBConnectionServiceImpl.layer,
        MigrationServiceImpl.layer,
        FileServiceImpl.layer,
        DBAccessServiceImpl.layer,
        MigrationRunnerServiceImpl.layer,
      )

  def run =
    for {
      args <- getArgs.map(_.toList)
      config <- ZIO.succeed(Config(args))

      path <-
        val path = Paths.get(config.migrationPath().nn)
        if path == null || !Files.exists(path) then
          ZIO.fail(s"Unable to read from migration directory: ${config.migrationPath}")
        else ZIO.succeed(path)

      _ <- program(path)
    } yield ()
