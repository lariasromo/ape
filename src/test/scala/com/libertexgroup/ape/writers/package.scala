package com.libertexgroup.ape

import com.libertexgroup.ape.models.dummy
import zio.Chunk
import zio.stream.ZStream

package object writers {
  val sampleRecords: Chunk[dummy] = Chunk(
    dummy("value1", "value2"),
    dummy("value3", "value4"),
    dummy("value4", "value5"),
    dummy("value6", "value7"),
    dummy("value8", "value9"),
  )

  val sampleData: ZStream[Any, Nothing, dummy] = ZStream.fromChunk(sampleRecords)
}
