package com.mattzq.migrate
package service

import zio.{Exit, RLayer, Task, UIO, ULayer, URIO, URLayer, ZIO, ZLayer}

import java.sql.{PreparedStatement, ResultSet, Statement}
import scala.util.{Failure, Success}

trait DBConnectionService:
  def close: UIO[Unit]
  def rollback: UIO[Unit]
  def commit: UIO[Unit]
  def hasTable(table: String): Task[Boolean]
  def executePreparedQuery(query: String, setParamsFn: PreparedStatement => Unit, commit: Boolean = false): Task[ResultSet]
  def executeUpdatePreparedQuery(query: String, setParamsFn: PreparedStatement => Unit, commit: Boolean = false): Task[Int]
  def executeQuery(query: String, commit: Boolean = false): Task[ResultSet]
  def execute(query: String, commit: Boolean = false): Task[Boolean]
  def executeUpdate(query: String, commit: Boolean = false): Task[Int]

// companion object
object DBConnectionService:
  def hasTable(table: String) =
    ZIO.serviceWithZIO[DBConnectionService](_.hasTable(table))

  def execute(query: String, commit: Boolean = false) =
    ZIO.serviceWithZIO[DBConnectionService](_.execute(query, commit))

  def executeQuery(query: String, commit: Boolean = false) =
    ZIO.serviceWithZIO[DBConnectionService](_.executeQuery(query, commit))

  def executeUpdate(query: String, commit: Boolean = false) =
    ZIO.serviceWithZIO[DBConnectionService](_.executeUpdate(query, commit))

  def executeUpdatePreparedQuery(query: String, setParamsFn: PreparedStatement => Unit, commit: Boolean = false) =
    ZIO.serviceWithZIO[DBConnectionService](_.executeUpdatePreparedQuery(query, setParamsFn, commit))

case class DBConnectionServiceImpl(private val connection: java.sql.Connection)
    extends DBConnectionService:
  override def close: UIO[Unit] =
    println("DBConnectionServiceImpl close")
    connection.close
    ZIO.succeed(())

  override def rollback: UIO[Unit] =
    println("DBConnectionServiceImpl rollback")
    connection.rollback
    ZIO.succeed(())

  override def commit: UIO[Unit] =
    println("DBConnectionServiceImpl commit")
    connection.commit
    ZIO.succeed(())

  override def executePreparedQuery(query: String, setParamsFn: PreparedStatement => Unit, commit: Boolean = false): Task[ResultSet] =
    ZIO.attemptBlocking {
      try
        val stmt = connection.prepareStatement(query)
        if (stmt == null) then
          throw new Exception("Error preparing statement!")

        setParamsFn(stmt)
        val result = stmt.executeQuery()
        if (result == null) then
          throw new Exception("Error executing statement!")

        result
      catch
        case e: Throwable => {
          println(e)
          connection.rollback()
          throw e
        }
      finally
        if commit then
          connection.commit()
    }

  override def executeUpdatePreparedQuery(query: String, setParamsFn: PreparedStatement => Unit, commit: Boolean = false): Task[Int] =
    ZIO.attemptBlocking {
      try
        val stmt = connection.prepareStatement(query)
        if (stmt == null) then
          throw new Exception("Error preparing statement!")

        setParamsFn(stmt)
        stmt.executeUpdate()
      catch
        case e: Throwable => {
          connection.rollback()
          throw e
        }
      finally
        if commit then
          connection.commit()
    }

  override def hasTable(table: String): Task[Boolean] =
    for {
      result <- executePreparedQuery("SELECT to_regclass(?);", _.setString(1, table))
    } yield
      if result.next() then
        result.getString(1) != null
      else
        false

  override def executeQuery(query: String, commit: Boolean = false): Task[ResultSet] =
    ZIO.attemptBlocking {
      try
        val stmt = connection.createStatement()
        if (stmt == null) then
          throw new Exception("Error preparing statement!")

        val result = stmt.executeQuery(query)
        if (result == null) then
          throw new Exception("Error executing statement!")

        result
      catch
        case e: Throwable => {
          println(e)
          connection.rollback()
          throw e
        }
      finally
        if commit then
          connection.commit()
    }

  override def execute(query: String, commit: Boolean = false): Task[Boolean] =
    ZIO.attemptBlocking {
      try
        val stmt = connection.createStatement()
        if (stmt == null) then
          throw new Exception("Error preparing statement!")

        stmt.execute(query)
      catch
        case e: Throwable => {
          println(e)
          connection.rollback()
          throw e
        }
      finally
        if commit then
          connection.commit()
    }

  override def executeUpdate(query: String, commit: Boolean = false): Task[Int] =
    ZIO.attemptBlocking {
      try
        val stmt = connection.createStatement()
        if (stmt == null) then
          throw new Exception("Error preparing statement!")

        stmt.executeUpdate(query)
      catch
        case e: Throwable => {
          println(e)
          connection.rollback()
          throw e
        }
      finally
        if commit then
          connection.commit()
    }

// composition object
object DBConnectionServiceImpl:
  def connect(url: String) =
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
