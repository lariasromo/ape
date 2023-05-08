package com.libertexgroup.readers.websocket

import com.libertexgroup.ape.reader.Reader
import com.libertexgroup.models.websocket.Message
import sttp.ws.WebSocket
import zio.Task

trait WebsocketReaders {
  def default(ws: WebSocket[Task]): Reader[Any, Any, Message]
}