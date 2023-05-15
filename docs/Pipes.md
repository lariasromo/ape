[Pipes](src/main/scala/com/libertexgroup/pipes)
------

A writer has a main method `apply` that uses a stream of type `InputType` and stores this stream effect-fully with an environment of type `EnvType` and able to fail with a `Throwable` side effect

A good practice (like we do with readers) we keep a common trait for writers to the same target e.g. `ClickhouseWriter`, `S3Writer`, `KafkaWriter`, etc.
In most cases the same config we use for reading from a source can be reused to write to a target.

Example...

```scala

trait S3Writer[T] extends Writer[T]

class DefaultWriter[E] extends S3Writer[E] {
   override type EnvType = s3.S3 with E with Has[S3Config]
   override type InputType = Array[Byte]

   override def apply(stream: ZStream[E, Throwable, InputType]): ZIO[EnvType, Throwable, Unit] = ???
}
```
