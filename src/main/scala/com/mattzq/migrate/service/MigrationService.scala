package com.mattzq.migrate
package service

import java.nio.file.Path
import zio.*
import com.mattzq.migrate.entity.{Migration, MigrationCollection}
import com.mattzq.migrate.service.DBSettingsServiceImpl.createFromEnv


trait MigrationService:
  def discoverMigrations(path: Path): Task[MigrationCollection]

object MigrationService:
  def discoverMigrations(path: Path): ZIO[MigrationService, Throwable, MigrationCollection] =
    ZIO.serviceWithZIO[MigrationService](_.discoverMigrations(path))

case class MigrationServiceImpl(fileService: FileService) extends MigrationService:
  override def discoverMigrations(path: Path): Task[MigrationCollection] =
    for {
      // check if directory is a directory and return list of files
      files <- fileService.list(path, ".sql")

      // read file contents
      contents <- ZIO.foreachPar(files)(filename => fileService.read(filename))

      // hash file contents
      hashes <- ZIO.foreachPar(contents)(content => fileService.hashString(content))

      files <-
        ZIO.succeed(
          files
            .lazyZip(contents)
            .lazyZip(hashes)
            .map((file, content, hash) => {
              Migration.byLocalFile(file, content, hash)
            })
            .sortWith((s, t) => s.id < t.id)
        )
    } yield MigrationCollection(files)

object MigrationServiceImpl:
  lazy val layer: URLayer[FileService, MigrationService] =
    ZLayer {
      for {
        fileService <- ZIO.service[FileService]
      } yield MigrationServiceImpl(fileService)
    }

