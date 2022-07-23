package com.mattzq.migrate.service

import zio.{ Task, ULayer, ZIO, ZLayer }
import zio.test.{ Spec, TestEnvironment, ZIOSpecDefault, assertTrue }
import zio.test.Assertion.*
import com.mattzq.migrate.entity.Migration

import java.nio.file.{ Files, Path }
import java.security.MessageDigest

class FileServiceMock extends FileService:
  override def list(path: Path, extension: String): Task[List[Path]] =
    ZIO.succeed(
      List(
        Path.of("/0001-test-foo.sql").nn,
        Path.of("/0002-test-bar.sql").nn,
        Path.of("/0003-test-baz.sql").nn,
      )
    )

  override def read(path: Path): Task[String] =
    ZIO.succeed("foo")

  override def hashString(contents: String): Task[String] =
    ZIO.succeed(s"sha($contents)")

object FileServiceMock:
  val layer: ULayer[FileService] = ZLayer.succeed(FileServiceMock())

object MigrationServiceSpec extends ZIOSpecDefault:
  def spec: Spec[Environment & TestEnvironment, Any] = suite("MigrationServiceSuite")(
    test("#discoverMigrations") {
      for {
        migrations <- MigrationService.discoverMigrations(Path.of("/mock").nn)
      } yield assertTrue(
        migrations.migrations == List(
          Migration(1, "0001-test-foo.sql", "sha(foo)", Some("foo")),
          Migration(2, "0002-test-bar.sql", "sha(foo)", Some("foo")),
          Migration(3, "0003-test-baz.sql", "sha(foo)", Some("foo")),
        )
      )
    }
  ).provideLayer(MigrationServiceImpl.layer).provideLayer(FileServiceMock.layer)
