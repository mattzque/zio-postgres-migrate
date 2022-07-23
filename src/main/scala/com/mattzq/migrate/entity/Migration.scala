package com.mattzq.migrate.entity

import java.nio.file.Path;

final case class Migration(
    id: Int,
    name: String,
    hash: String,
    content: Option[String])

object Migration:
  final val FILENAME_ID_REGEXP = raw"^(\d+).*".r

  private def getIdFromFilename(filename: String): Int =
    filename.match
      case FILENAME_ID_REGEXP(id) => id.toInt
      case _ => -1

  def byLocalFile(file: Path, content: String, hash: String): Migration =
    Migration(
      id=getIdFromFilename(file.getFileName.toString),
      name=file.getFileName.toString,
      hash=hash,
      content=Some(content))
