package ape.websocket.readers

import ape.reader.Reader
import ape.websocket.models.Message
import sttp.ws.WebSocket
import zio.Task

trait WebsocketReaders {
  def default(ws: WebSocket[Task]): Reader[Any, Any, Message]
}