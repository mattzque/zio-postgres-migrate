package com.mattzq
package migrate

import java.io.IOException
import java.nio.file.Paths
import zio.*

import java.sql.Connection
import scala.util.{Properties, Success, Failure}
import service.*

object Main:
  def main(args: Array[String]): Unit =
    val args2 =
      if args.length == 0 then args :+ "./migrations"
      else args

    if args2.length != 1 then
      scala.Console.err.println("Provide migration directory as first argument.")
      sys.exit(1)
    else
      val path = Paths.get(args2(0))
      if path == null then
        scala.Console.err.println("Migration directory not found!")
        sys.exit(1)
      else

        val checkTableProgram = for {
          _ <- Console.printLine("hi")
          _ <- MigrationRunnerService.run(path)
        } yield ()
        val runnableCheckTableProgram =
          checkTableProgram.provideSome(
            DBSettingsServiceImpl.layerFromEnv,
            DBConnectionServiceImpl.layer,
            MigrationServiceImpl.layer,
            FileServiceImpl.layer,
            DBAccessServiceImpl.layer,
            MigrationRunnerServiceImpl.layer,
          )

        val runtime = Runtime.default
        Unsafe.unsafe {
          runtime.unsafe.run(runnableCheckTableProgram).getOrThrowFiberFailure()
        }
