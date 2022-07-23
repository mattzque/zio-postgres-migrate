package com.mattzq.migrate
package service

import com.mattzq.migrate.entity.MigrationCollection

import java.nio.file.Path
import zio.*

import scala.util.{Failure, Success, Try}

class MigrationValidationError(message: String) extends Exception(message)

trait MigrationRunnerService:
  def run(path: Path): Task[Unit]

object MigrationRunnerService:
  def run(path: Path) =
    ZIO.serviceWithZIO[MigrationRunnerService](_.run(path))

case class MigrationRunnerServiceImpl(db: DBAccessService, migrationService: MigrationService) extends MigrationRunnerService:
  override def run(path: Path): Task[Unit] =
    for {
      _ <- Console.printLine(s"hai")

      // read migrations from local directory
      migrationsLocal <- migrationService.discoverMigrations(path)

      // check if the database has the migration table
      hasDbMigrationTable <- db.hasMigrationTable
      _ <- Console.printLine(s"hasDbMigrationTable? $hasDbMigrationTable")

      // create migration table if not existing
      _ <-
        if hasDbMigrationTable then
          ZIO.succeed(())
        else
          db.createMigrationTable

      // read migrations from database table
      migrationHistory <- db.getMigrationTable

      // check the history is empty or matches the migration files in the local directory
      _ <-
        validate(migrationHistory, migrationsLocal) match
          case Failure(error) => ZIO.fail(error)
          case Success(_) => ZIO.succeed(())

      // gather list of migrations to execute
      plan <- ZIO.succeed(assemblePlan(migrationHistory, migrationsLocal))

      _ <-
        Console.printLine(s"plan -> $plan")

      _ <- ZIO.foreach(plan.migrations)(migration => {
        db.runMigration(migration)
      })


    } yield ()

  def assemblePlan(history: MigrationCollection, local: MigrationCollection): MigrationCollection =
    local.filter(m => !history.hasMigration(m))

  def validate(history: MigrationCollection, local: MigrationCollection): Try[Boolean] =
    if history.migrations.isEmpty then
      Success(true)
    else
      history.migrations
        .map(migration => {
          local.get(migration.id) match
            case Some(otherMigration) => {
              if (migration.hash == otherMigration.hash) then
                Success(true)
              else
                Failure(MigrationValidationError(s"Migration(${migration.id}) hash miss-match!"))
            }
            case None => {
              Failure(MigrationValidationError(s"Migration(${migration.id}) not found!"))
            }
        })
        .find(p => p.isFailure)
        .getOrElse(Success(true))

object MigrationRunnerServiceImpl:
  lazy val layer: RLayer[DBAccessService & MigrationService, MigrationRunnerService] =
    ZLayer.scoped {
      for {
        db <- ZIO.service[DBAccessService]
        migrationService <- ZIO.service[MigrationService]
      } yield MigrationRunnerServiceImpl(db, migrationService)
    }
