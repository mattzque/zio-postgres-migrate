package com.mattzq.migrate.entity


final case class DBSettings(
    val database: String,
    val username: String,
    val password: String,
    val hostname: String,
    val port: Int,
    val ssl: Boolean):
  def url: String =
    s"jdbc:postgresql://$hostname:$port/$database?user=$username&password=$password&ssl=$ssl"
