One example of using the reastAPI reader along with a kafka writer

```scala
object runRestApiReaderApe  extends ZIOAppDefault{

  val bodyString = "{\"iso3Code\": { \"value\": \"COL\"}}"
  val BodyFromString = Body.fromString(bodyString)

  val url = URL.fromString("https://rest-service-marketingtables.fcil-env.com/api/v2.0/findNewBusinessUnitByCountryCodeIso3")


  val request = Request(
    BodyFromString,
    Headers( "Content-Type","application/json"),
    Method.POST,
    url.toOption.get,
    Version.Http_1_1,
    None

  )

   val transformer : String => ProducerRecord[String,String] = meg => new ProducerRecord[String,String]("key",meg)


  val pipeline =
    Ape.readers.restApiReaderString(request).withTransform(transformer) --> Ape.pipes.kafkaStringWriter[Any]

  val kafkaConfigLayer = ZLayer.succeed(KafkaConfig(
    "mytopics",
    List( "qa-fxb2-kafka-21-1.qa-env.com:9092"),
    "groupForMarketingInfoPipe12123224",
    Duration.ofSeconds(1),
    1,
    AutoOffsetStrategy.Latest
  )
  )
  val kafkaProducerLayer =  (kafkaConfigLayer >+> KafkaUtils.producerLayer) ++ zio.http.Client.default

  val runningPipe =  for {

    runningPipe <- pipeline
    _ <- runningPipe.run
  } yield ()
  //val kafkaLayer = KafkaConfig.live

  override def run = {
    runningPipe.provideLayer(kafkaProducerLayer)

}

}
```


line 
```scala
 val pipeline =
    Ape.readers.restApiReaderString(request).withTransform(transformer) --> Ape.pipes.kafkaStringWriter[Any]

```
is where the pipeline is defined. So reading from HTTP -> tranforming -> writing to Kafka