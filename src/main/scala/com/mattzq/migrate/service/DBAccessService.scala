package com.mattzq.migrate.service

import com.mattzq.migrate.entity.{Migration, MigrationCollection}
import zio.{RLayer, Task, ZIO, ZLayer, Console}

import scala.io.Source

trait DBAccessService:
  def createMigrationTable: Task[Unit]
  def hasMigrationTable: Task[Boolean]
  def getMigrationTable: Task[MigrationCollection]
  def runMigration(migration: Migration): Task[Unit]

object DBAccessService:
  def createMigrationTable =
    ZIO.serviceWithZIO[DBAccessService](_.createMigrationTable)

  def hasMigrationTable =
    ZIO.serviceWithZIO[DBAccessService](_.hasMigrationTable)

  def getMigrationTable =
    ZIO.serviceWithZIO[DBAccessService](_.getMigrationTable)

  def runMigration(migration: Migration) =
    ZIO.serviceWithZIO[DBAccessService](_.runMigration(migration))

case class DBAccessServiceImpl(connection: DBConnectionService) extends DBAccessService:
  final val MIGRATION_TABLE = "migration"
  final val MIGRATION_SQL_FILE = "create_migration_table.sql"
  final val QUERY_FIND_MIGRATION_ROW = s"SELECT id, name, hash FROM $MIGRATION_TABLE;"
  final val QUERY_INSERT_MIGRATION = "INSERT INTO migration (id, name, hash) VALUES (?, ?, ?);"

  override def createMigrationTable: Task[Unit] =
    for {
      _ <- Console.printLine("create migration table")
      query <- ZIO.attemptBlocking {
        val source = Source.fromFile(MIGRATION_SQL_FILE)
        val content = source.mkString
        source.close
        content
      }
      stmt <- connection.prepareStatement(query)
      _ <- connection.updatePreparedStatement(stmt)
      _ <- connection.commit
      _ <- Console.printLine("migration table created")
    } yield ()

  override def hasMigrationTable: Task[Boolean] =
    connection.hasTable(MIGRATION_TABLE)

  override def getMigrationTable: Task[MigrationCollection] =
    for {
      stmt <- connection.prepareStatement(QUERY_FIND_MIGRATION_ROW)
      results <- connection.queryPreparedStatement(stmt)
      migrations <-
        ZIO.attemptBlocking {
          var list: List[Migration] = List()
          while (results.next())
            val id = results.getInt(1)
            val name = results.getString(2)
            val hash = results.getString(3)
            if (name == null || hash == null)
              throw Exception("Invalid row in migration table!")

            list = list :+ Migration(id, name, hash, None)
          list
        }
    } yield MigrationCollection(migrations)

  override def runMigration(migration: Migration): Task[Unit] =
    for {
      _ <- Console.printLine(s"Applying Migration ${migration.id} (${migration.name})...")
      script <-
        migration.content match
          case Some(script) => ZIO.succeed(script)
          case None => ZIO.fail(Exception("Migration script missing!"))
      _ <-
        for {
          // Run the migration and insert it in the history in a single transaction
          stmt <- connection.prepareStatement(script)
          _ <- connection.updatePreparedStatement(stmt)
          stmt <- connection.prepareStatement(QUERY_INSERT_MIGRATION, Some(stmt => {
            stmt.setInt(1, migration.id)
            stmt.setString(2, migration.name)
            stmt.setString(3, migration.hash)
          }))
          _ <- connection.updatePreparedStatement(stmt)
          _ <- connection.commit
        } yield ()
      _ <- Console.printLine(s"Migration Applied successfully. (${migration.id})")
    } yield ()

object DBAccessServiceImpl:
  lazy val layer: RLayer[DBConnectionService, DBAccessService] =
    ZLayer.scoped {
      for {
        database <- ZIO.service[DBConnectionService]
      } yield DBAccessServiceImpl(database)
    }
