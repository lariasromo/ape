package com.libertexgroup.ape.readers.websocket

import com.libertexgroup.ape.Reader
import com.libertexgroup.models.websocket.Message
import sttp.ws.WebSocket
import zio.Task

// Readers
protected [readers] class Readers() {
  def default[E, ZE](ws: WebSocket[Task]): Reader[E, ZE, Message] = new DefaultReader[E, ZE](ws)
}
