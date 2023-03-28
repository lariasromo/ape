package com.libertexgroup.ape.readers.websocket

import com.libertexgroup.ape.Reader
import com.libertexgroup.models.websocket.Message
import sttp.ws.WebSocket
import zio.stream.ZStream
import zio.{Task, ZIO}

import java.time.ZoneOffset


protected[readers] class DefaultReader[E1, E2](ws: WebSocket[Task])
  extends Reader[E1, E2, Message] {
  def a: ZIO[Any, Nothing, ZStream[Any, Throwable, Message]] = for {
    stream <- ZIO.succeed {
      ZStream
        .fromZIO(ws.receiveText())
        .forever
        .mapZIO(text => for {
          dt <- zio.Clock.currentDateTime
        } yield Message(dt.toLocalDateTime.toInstant(ZoneOffset.UTC).toEpochMilli, text))
    }
  } yield stream

  override def apply: ZIO[E1, Throwable, ZStream[E2, Throwable, Message]] = a
}