package com.libertexgroup.ape.readers.websocket

import com.libertexgroup.models.websocket.Message
import sttp.ws.WebSocket
import zio.stream.ZStream
import zio.{Task, ZIO}

import java.time.ZoneOffset


protected[readers] class DefaultReader[E1, E2](ws: WebSocket[Task])
  extends com.libertexgroup.ape.readers.Reader[E1, E2, Message] {
  def apply: ZIO[Any, Nothing, ZStream[Any, Throwable, Message]] = for {
    stream <- ZIO.succeed {
      ZStream
        .fromZIO(ws.receiveText())
        .forever
        .mapZIO(text => for {
          dt <- zio.Clock.currentDateTime
        } yield Message(dt.toLocalDateTime.toInstant(ZoneOffset.UTC).toEpochMilli, text))
    }
  } yield stream
}