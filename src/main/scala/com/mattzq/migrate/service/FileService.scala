package com.mattzq.migrate
package service

import java.nio.file.Path
import java.nio.file.Files

import java.security.MessageDigest
import collection.JavaConverters.*
import scala.util.Try
import scala.util.Failure
import zio.*
import scala.util.Success

import com.mattzq.migrate.toOption

sealed trait FileService:
  /** 
   * List files in a directory filtered by extension.
   */
  def list(path: Path, extension: String): Task[List[Path]] 

  /**
   * Read file contents into a string.
   */
  def read(path: Path): Task[String] 

  /**
   * Hash contents to SHA-1 hex string. 
   */
  def hashString(contents: String): Task[String] 

class FileServiceImpl extends FileService:
  override def list(path: Path, extension: String): Task[List[Path]] =
    ZIO.attemptBlocking {
      Files
        .list(path)
        .toOption
        .flatMap(_.iterator.toOption)
        .map(files => files.asScala.toList.filter(_.toString.endsWith(extension)))
        .getOrElse(List.empty)
    }
  
  override def read(path: Path): Task[String] =
    ZIO.attemptBlocking {
      Files.readString(path).toOption.getOrElse("")
    }

  override def hashString(contents: String): Task[String] =
    ZIO.attemptBlocking {
      MessageDigest
        .getInstance("SHA-1")
        .toOption
        .flatMap(_.digest(contents.getBytes).toOption)
        .map(_.map(b => String.format("%02x", Byte.box(b))).mkString)
        .getOrElse(throw Exception("Error hashing file contents!"))
    }

object FileServiceImpl:
  val layer: ULayer[FileService] = ZLayer.succeed(FileServiceImpl())