package com.mattzq.migrate
package service

import zio.Runtime.Scoped
import zio.{ Exit, RLayer, Scope, Task, UIO, ULayer, URIO, URLayer, ZEnvironment, ZIO, ZLayer }

import java.sql.{ PreparedStatement, ResultSet, Statement }
import scala.util.{ Failure, Success, Try }

trait DBConnectionService:
  def close: UIO[Unit]
  def rollback: UIO[Unit]
  def commit: UIO[Unit]
  def hasTable(table: String): Task[Boolean]
  def prepareStatement(
      query: String,
      setParamsFn: Option[PreparedStatement => Unit] = None,
    ): Task[PreparedStatement]
  def queryPreparedStatement(stmt: PreparedStatement): Task[ResultSet]
  def updatePreparedStatement(stmt: PreparedStatement): Task[Int]

// companion object
object DBConnectionService:
  def hasTable(table: String): ZIO[DBConnectionService, Throwable, Boolean] =
    ZIO.serviceWithZIO[DBConnectionService](_.hasTable(table))

  def prepareStatement(
      query: String,
      setParamsFn: Option[PreparedStatement => Unit] = None,
    ): ZIO[DBConnectionService, Throwable, PreparedStatement] =
    ZIO.serviceWithZIO[DBConnectionService](_.prepareStatement(query, setParamsFn))

  def queryPreparedStatement(
      stmt: PreparedStatement
    ): ZIO[DBConnectionService, Throwable, ResultSet] =
    ZIO.serviceWithZIO[DBConnectionService](_.queryPreparedStatement(stmt))

  def updatePreparedStatement(stmt: PreparedStatement): ZIO[DBConnectionService, Throwable, Int] =
    ZIO.serviceWithZIO[DBConnectionService](_.updatePreparedStatement(stmt))

case class DBConnectionServiceImpl(private val connection: java.sql.Connection)
    extends DBConnectionService:
  override def close: UIO[Unit] =
    println("DBConnectionServiceImpl close")
    connection.close()
    ZIO.succeed(())

  override def rollback: UIO[Unit] =
    println("DBConnectionServiceImpl rollback")
    connection.rollback()
    ZIO.succeed(())

  override def commit: UIO[Unit] =
    println("DBConnectionServiceImpl commit")
    connection.commit()
    ZIO.succeed(())

  override def prepareStatement(
      query: String,
      setParamsFn: Option[PreparedStatement => Unit] = None,
    ): Task[PreparedStatement] =
    ZIO.attemptBlocking {
      val stmt = connection.prepareStatement(query)
      if stmt == null then throw new Exception("Error preparing statement!")

      setParamsFn.foreach(_(stmt))
      stmt
    }

  override def queryPreparedStatement(stmt: PreparedStatement): Task[ResultSet] =
    ZIO.attemptBlocking {
      try
        val result = stmt.executeQuery()
        if result == null then throw new Exception("Error executing statement!")

        result
      catch
        case e: Throwable =>
          connection.rollback()
          throw e
    }

  override def updatePreparedStatement(stmt: PreparedStatement): Task[Int] =
    ZIO.attemptBlocking {
      try stmt.executeUpdate()
      catch
        case e: Throwable =>
          connection.rollback()
          throw e
    }

  override def hasTable(table: String): Task[Boolean] =
    for {
      stmt <- prepareStatement("SELECT to_regclass(?);", Some(_.setString(1, table)))
      result <- queryPreparedStatement(stmt)
    } yield
      if result.next() then result.getString(1) != null
      else false

// composition object
object DBConnectionServiceImpl:
  def connect(url: String): Task[DBConnectionServiceImpl] =
    for {
      _ <- ZIO.attempt(Class.forName("org.postgresql.Driver"))
      connection <- ZIO.attemptBlocking {
        val connection = java.sql.DriverManager.getConnection(url)
        if (connection == null) throw Exception("getConnection returned null!")
        else
          connection.setAutoCommit(false)
          connection
      }
    } yield DBConnectionServiceImpl(connection)

  lazy val layer: RLayer[DBSettingsService, DBConnectionService] =
    ZLayer.scoped {
      for {
        url <- ZIO.serviceWith[DBSettingsService](_.url)
        connection <- ZIO.acquireRelease(connect(url))(_.close)
      } yield connection
    }
