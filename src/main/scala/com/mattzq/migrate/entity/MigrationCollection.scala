package com.mattzq.migrate.entity

import scala.util.{ Failure, Success, Try }
import com.mattzq.migrate.formatTable

case class MigrationCollection(migrations: List[Migration]):
  def get(id: Int): Option[Migration] =
    migrations.find(migration => migration.id == id)

  def hasMigration(migration: Migration): Boolean =
    migrations.exists(m => m.id == migration.id)

  def filter(test: Migration => Boolean): MigrationCollection =
    MigrationCollection(migrations.filter(test))

  def toTableString(title: String): String =
    val line = "-" * 120
    if migrations.isEmpty then
      List(
        "\n",
        title,
        line,
        "No Migrations",
      ).mkString("\n")
    else
      List(
        line,
        title,
        line,
        formatTable(
          migrations,
          columns = List("Id", "Name", "Hash"),
          widths = List(10, 50, 40),
          row =>
            List(
              row.id.toString,
              row.name,
              row.hash,
            ),
        ),
      ).mkString("\n")
