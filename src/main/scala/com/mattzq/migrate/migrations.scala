package com.mattzq
package migrate

import java.nio.file.Path
import java.nio.file.Files
import java.security.MessageDigest
import collection.JavaConverters.*
import scala.util.Try
import scala.util.Failure
import zio.*
import scala.util.Success

case class MigrationFile(
    id: Int,
    path: Path,
    checksum: String,
    content: String,
  ):
  def name = path.getFileName

case class Migrations(files: List[MigrationFile])


def listFiles(path: Path, extension: String): Option[List[Path]] =
  try
    Files
      .list(path)
      .toOption
      .flatMap(_.iterator.toOption)
      .map(files => files.asScala.toList.filter(_.toString.endsWith(extension)))
  catch case _ => None

object Migrations:
  def listMigrations(directory: Path): Task[List[Path]] =
    listFiles(directory, ".sql") match
      case Some(files) => ZIO.succeed(files)
      case None => ZIO.fail(Exception("Could not list migration directory!"))

  def readFile(path: Path): Task[String] =
    Files.readString(path).toOption match
      case Some(contents) => ZIO.succeed(contents)
      case None => ZIO.fail(Exception("Error reading file!"))

  def checksumFile(contents: String): Task[String] =
    MessageDigest
      .getInstance("SHA-1")
      .toOption
      .flatMap(_.digest(contents.getBytes).toOption) match
      case Some(digest) => ZIO.succeed(digest.map(b => String.format("%02x", Byte.box(b))).mkString)
      case None => ZIO.fail(Exception("Error hashing file contents!"))

  final val FILENAME_ID_REGEXP = raw"^(\d+).*".r

  def getFileId(filename: String): Int =
    filename.match
      case FILENAME_ID_REGEXP(id) => id.toInt
      case _ => -1

  def discover(directory: Path) =
    for {
      // check if directory is a directory and return list of files
      files <- listMigrations(directory)

      // read file contents
      contents <- ZIO.foreachPar(files)(filename => readFile(filename))

      // hash file contents
      checksums <- ZIO.foreachPar(contents)(content => checksumFile(content))

      files <-
        ZIO.succeed(
          files
            .lazyZip(contents)
            .lazyZip(checksums)
            .map((file, content, checksum) =>
              val id = getFileId(file.getFileName.toString)
              MigrationFile(id = id, path = file, checksum = checksum, content = content)
            )
            .sortWith((s, t) => s.id < t.id)
        )
    } yield Migrations(files)
