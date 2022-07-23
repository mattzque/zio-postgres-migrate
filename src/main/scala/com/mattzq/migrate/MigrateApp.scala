package com.mattzq
package migrate

import java.io.IOException
import java.nio.file.{ Files, Path, Paths }
import zio.{ Console, ZIO, ZIOAppDefault }

import java.sql.Connection
import scala.util.{ Failure, Properties, Success }
import service.*

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
      args <- getArgs

      firstArg <-
        args.headOption match
          case Some(firstArg) if (firstArg: String | Null) != null => ZIO.succeed(firstArg)
          case _ => ZIO.fail("Please give the path to the migration directory as first argument.")

      path <-
        val path = Paths.get(firstArg.nn)
        if path == null || !Files.exists(path) then
          ZIO.fail(s"Unable to read from migration directory: ${firstArg}")
        else ZIO.succeed(path)

      _ <- program(path)
    } yield ()
