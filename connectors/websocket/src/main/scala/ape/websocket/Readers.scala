package ape.websocket

import ape.reader.Reader
import ape.websocket.models.Message
import ape.websocket.readers.{DefaultReader, WebsocketReaders}
import sttp.ws.WebSocket
import zio.Task


// Readers
protected[websocket] class Readers() extends WebsocketReaders {
  def default(ws: WebSocket[Task]): Reader[Any, Any, Message] = new DefaultReader[Any](ws)
}

object Readers {
  def readers = new Readers()
}
