package com.libertexgroup.ape

import com.libertexgroup.ape.models.dummy
import zio.Chunk
import zio.stream.ZStream

package object pipes {
  val sampleRecords: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value3", "value4"),
    dummy("value5", "value6"),
    dummy("value7", "value8"),
    dummy("value9", "value10"),
  )

  val sampleData: ZStream[Any, Throwable, dummy] = ZStream.fromChunk(sampleRecords)
}
