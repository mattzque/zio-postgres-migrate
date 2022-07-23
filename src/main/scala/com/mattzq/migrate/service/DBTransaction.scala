package com.mattzq.migrate.service

import zio.{ZIO, ZLayer, Exit}

trait DBTransaction

case class DBTransactionImpl(private val connection: ZLayer[Any, Throwable, DBConnectionService]) extends DBTransaction

object DBTransactionImpl:
  def layer: ZLayer[DBConnectionService, Nothing, DBTransaction] =
    ZLayer.scoped {
      for {
        connection <- ZIO.service[DBConnectionService]
      } yield DBTransactionImpl {
        ZLayer.scoped {
          for {
            _ <- ZIO.addFinalizerExit {
              case Exit.Success(_) => ZIO.unit
              case Exit.Failure(_) => connection.rollback
            }
          } yield connection
        }
      }
    }
