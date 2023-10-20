package ape.redis

import ape.reader.Reader
import ape.redis.models.dummy
import zio.Chunk
import zio.stream.ZStream

package object readers {
  val dummyData: Chunk[dummy] = Chunk(
    dummy("Lorem ipsum dolor sit amet", "consectetur adipiscing elit."),
    dummy("Nulla mi velit", "rutrum sit amet justo a, temporbibendum diam."),
    dummy("Phasellus vestibulum sem nec ante fermentum", "et vulputate libero auctor."),
    dummy("Donec facilisis mauris", "a consequat tempor."),
    dummy("Vivamus ullamcorper mi leo", "sit amet cursus massa posuere ac."),
    dummy("Nunc at sodales mi", "Maecenas nec enim quam."),
    dummy("In nec risus vel massa", "ultricies convallis."),
    dummy("Duis magna velit, sollicitudin quis feugiat eu", "facilisis convallis orci.")
  )
  val dummyStream: ZStream[Any, Throwable, dummy] = ZStream.fromChunk(dummyData)

  val stringData: Chunk[String] = Chunk(
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
    "Nulla mi velit, rutrum sit amet justo a, temporbibendum diam.",
    "Phasellus vestibulum sem nec ante fermentum, et vulputate libero auctor.",
    "Donec facilisis mauris a consequat tempor.",
    "Vivamus ullamcorper mi leo, sit amet cursus massa posuere ac.",
    "Nunc at sodales mi. Maecenas nec enim quam.",
    "In nec risus vel massa ultricies convallis.",
    "Duis magna velit, sollicitudin quis feugiat eu, facilisis convallis orci."
  )
  val stringStream: ZStream[Any, Nothing, String] = ZStream.fromChunk(stringData)

  val dummyReader = Reader.UnitReaderStream(dummyStream)
  val stringReader = Reader.UnitReaderStream(stringStream)
}
