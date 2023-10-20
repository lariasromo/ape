import ape.pipe.Pipe
import ape.reader.Reader
import zio.Console.printLine
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault, assertTrue}
import zio.{Chunk, Scope}


object PipelineZipTest extends ZIOSpecDefault {

  case class Person(name:String)
  case class Vasil(name:String)
  case class Luis(name:String)
  case class Topi(name:String)
  case class Sergey(name:String)

  val data: ZStream[Any, Nothing, Person] = ZStream.fromChunk(
    Chunk(
      Person("a"),
      Person("b"),
      Person("c"),
      Person("d"),
      Person("e"),
      Person("f"),
      Person("g"),
      Person("h"),
    )
  )


  val pipeVasil = Pipe.UnitZPipe[Any, Any, Person, Vasil](s =>
    s.tap(p => printLine("Vasil: " + p.name)).map(p => Vasil(p .name))
  )
    .filter(p=>Seq("a", "d") contains p.name)

  val pipeLuis = Pipe.UnitZPipe[Any, Any, Person, Luis](s =>
    s.tap(p => printLine("Luis: " + p.name)).map(p => Luis(p.name))
  )
    .filter(p=>Seq("b", "e", "a") contains p.name)

  val pipeTopi = Pipe.UnitZPipe[Any, Any, Person, Topi](s =>
    s.tap(p => printLine("Topi: " + p.name)).map(p => Topi(p.name))
  )
    .filter(p=>Seq("c", "f", "d") contains p.name)

  val pipeSergey = Pipe.UnitZPipe[Any, Any, Person, Sergey](s =>
    s.tap(p => printLine("Sergey: " + p.name)).map(p => Sergey(p.name))
  )
    .filter(p=>Seq("g", "h", "b") contains p.name)

  val twoWritersDeposit: Pipe[Any with Scope, Any, Person, (Vasil, Luis)] =
    pipeVasil ++ pipeLuis
  val twoWriterRegistration: Pipe[Any with Scope, Any, Person, (Topi, Sergey)] =
    pipeTopi ++ pipeSergey

//  val pipeAll = twoWritersDeposit ++ twoWriterRegistration
  val pipeAll: Pipe[Any with Scope, Any, Person, Person] = pipeVasil +++ (pipeLuis, pipeTopi, pipeSergey)

  val reader = Reader.UnitReaderStream(data)
  val pipe: ZStream[Any with Scope, Throwable, Person] = reader +++ (pipeVasil, pipeLuis, pipeTopi, pipeSergey)


  override def spec: Spec[TestEnvironment with Scope, Any] = suite("PipelineZipTest")(
    test("Hello world") {
      for {
        _ <- pipe.runDrain
      } yield {
        assertTrue(true)
      }
    }
  )
}
