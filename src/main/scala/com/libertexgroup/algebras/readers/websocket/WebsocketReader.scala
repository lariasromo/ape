package com.libertexgroup.algebras.readers.websocket

import com.libertexgroup.algebras.readers.Reader
import com.libertexgroup.models.websocket.Message
import sttp.ws.WebSocket
import zio.{Task, ZIO}
import zio.stream.ZStream

import java.time.ZoneOffset


class WebsocketReader(ws: WebSocket[Task]) extends Reader[Any, Any, Message] {
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