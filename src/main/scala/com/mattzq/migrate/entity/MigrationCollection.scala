package com.mattzq.migrate.entity

import scala.util.{Failure, Success, Try}

case class MigrationCollection(migrations: List[Migration]):
  def get(id: Int): Option[Migration] =
    migrations.find(migration => migration.id == id)

  def hasMigration(migration: Migration): Boolean =
    migrations.exists(m => m.id == migration.id)

  def filter(test: Migration => Boolean): MigrationCollection =
    MigrationCollection(migrations.filter(test))


