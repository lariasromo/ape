# S3FileReaderService
This service is used by the S3 readers, its purpose is to constantly and in the background list files from S3 and send them to a queue such queue is being read by a new ZStream in the Reader implementation.

This mechanism alongside the backpressure flag enabled in the S3Config class will help the readers and writers to
- Enable heavy stream computation while avoiding exceptions from S3 when keeping the file open for a long period of time.
- Avoid skipping files when processing multiple files per hour
- Read files that are partitioned on S3 by some path pattern e.g. `s3://bucket/files/year=XXXX/month=XX/day=XX/file.gz` or `s3://bucket/files/date=XXXX-XX-XX/file.gz`

This service is constructed and passed to the APE pipeline as a ZIO layer.
### Examples of FileReaderService
Simple service which only lists files one time from an S3 path.
```scala
import com.libertexgroup.ape.readers.s3.S3FileReaderServiceStatic

val location = "kafka/topic=audience/"
val serviceLayer = S3FileReaderServiceStatic.live(location)
```
More complex service which constantly reads files from an S3 path `pattern`. The `pattern` should be a function of type `ZIO[S3Config, Nothing, ZonedDateTime => List[String]]` which will form a string given a point of time.

The APE contains a handy method `S3Utils.pathConverter(location)` which will append the path: `year=XXXX/month=XX/day=XX` to a given location parameter.

```scala
import com.libertexgroup.ape.readers.s3.S3FileReaderServiceStream
import com.libertexgroup.configs.S3Config
import com.libertexgroup.utils.S3Utils
import zio.s3.S3
import zio.ZLayer

import java.io.IOException

val location = "kafka/topic=audience/"
val readerService: ZLayer[S3 with S3Config, Throwable, S3FileReaderServiceStream] = ZLayer.fromZIO {
  S3Utils.pathConverter(location).flatMap(S3FileReaderServiceStream.make(_))
}
```