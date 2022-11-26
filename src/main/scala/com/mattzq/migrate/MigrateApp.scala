package com.mattzq
package migrate

import java.io.IOException
import java.nio.file.{ Files, Path, Paths }

import zio.{ Console, ZIO, ZIOAppDefault }
import scopt.OParser

import service.*

case class Config(migrationPath: String = ".")

object Config:
  def parse(args: Seq[String]): Option[Config] =
    val builder = OParser.builder[Config]
    val parser =
      import builder.*
      OParser.sequence(
        programName("migrate"),
        head("migrate", "0.0.1"),
        arg[String]("<path>")
          .unbounded()
          .action((x, c) => c.copy(migrationPath = x))
          .text("Path to directory with migration files."),
      )
    OParser.parse(parser, args, Config())

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
      config <- ZIO.fromOption(Config.parse(args))

      path <-
        val path = Paths.get(config.migrationPath.nn)
        if path == null || !Files.exists(path) then
          ZIO.fail(s"Unable to read from migration directory: ${config.migrationPath}")
        else ZIO.succeed(path)

      _ <- program(path)
    } yield ()
