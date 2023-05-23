package ape.websocket.readers

import ape.reader.Reader
import ape.websocket.models.Message
import sttp.ws.WebSocket
import zio.stream.ZStream
import zio.{Task, ZIO}

import java.time.ZoneOffset


protected[websocket] class DefaultReader[ZE](ws: WebSocket[Task]) extends Reader[Any, ZE, Message] {

  override val name: String = "WebSocketReader"

  override protected[this] def read: ZIO[Any, Throwable, ZStream[ZE, Throwable, Message]] = for {
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