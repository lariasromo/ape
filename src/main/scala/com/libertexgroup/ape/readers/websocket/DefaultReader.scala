package com.libertexgroup.ape.readers.websocket

import com.libertexgroup.ape.Reader
import com.libertexgroup.models.websocket.Message
import sttp.ws.WebSocket
import zio.stream.ZStream
import zio.{Task, ZIO}

import java.time.ZoneOffset


protected[websocket] class DefaultReader[E, ZE](ws: WebSocket[Task])
  extends Reader[E, ZE, Message] {

  override def apply: ZIO[E, Throwable, ZStream[ZE, Throwable, Message]] = for {
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