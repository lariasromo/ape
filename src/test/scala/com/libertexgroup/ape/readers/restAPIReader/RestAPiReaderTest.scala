package com.libertexgroup.ape.readers.restAPIReader

import java.nio.charset.StandardCharsets

import com.libertexgroup.ape.models.dummy
import zio.test.{Spec, TestEnvironment, ZIOSpec}
import com.libertexgroup.ape.readers.jdbc.JDBCReaderTest.{reader, suite}
import com.libertexgroup.ape.readers.restAPIReader.RestAPiReaderTest.test
import com.libertexgroup.ape.utils.PostgresContainerService
import com.libertexgroup.configs.HttpClientConfig
import zio.http.{Body, Client, Request, Response, URL, ZClient}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.mockito.MockitoSugar.mock
import zio.http.model.Scheme.HTTP
import zio.test.{Assertion, Spec, ZIOSpecDefault, ZTestLogger, assertZIO}
import zio.{Chunk, Scope, ZIO, ZLayer}
import zio.http.model.{Headers, Method, Status, Version}
import java.net.InetAddress

import com.libertexgroup.ape.Ape
import zio.stream.ZStream
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}

object RestAPiReaderTest extends ZIOSpecDefault{






  override def spec = suite("RestAPiReaderTest")(
    test("write logs with request and response)") {

     // val request = Request.post(Body.fromString("""{"prop": "value"}"""), URL.empty)
      val mockServer = mock[Client]
      when(mockServer.request(any[Request]())(any(), any()))
        .thenReturn(ZIO.succeed(Response.text("Hi!")))

      val request = Request.post(Body.fromString("""{"prop": "value"}"""), URL.empty)

      for {
        stream <- Ape.readers.rest.byte(request).apply.provideLayer(ZLayer.succeed(mockServer))
        data <- stream.runCollect
      // _<-  zio.Console.printLine("pipes " + data.toArray.map(_.toChar).mkString)//new String( data.toArray, StandardCharsets.UTF_16))
      } yield {


        assertTrue(data.nonEmpty)
        assertTrue(data.toArray.map(_.toChar).mkString == "Hi!" )
      }

    }
 ,
      test("send real request)") {

        val bodyString = "{\"iso3Code\": { \"value\": \"COL\"}}"
        val BodyFromString = Body.fromString(bodyString)

        val url = URL.fromString("https://rest-service-marketingtables.fcil-env.com/api/v2.0/findNewBusinessUnitByCountryCodeIso3")

//        val remoteAd = InetAddress.getByName("https://rest-service-marketingtables.fcil-env.com")
        val request = Request(
          BodyFromString,
         Headers( "Content-Type","application/json"),
          Method.POST,
          url.toOption.get,
          Version.Http_1_1,
          None





        )

      for {
        stream<- Ape.readers.restApiReaderByte(request).apply.provideLayer(zio.http.Client.default)

        data <- stream.runCollect
      _<-  zio.Console.printLine("data:  " +data.toString)
         //new String( data.toArray, StandardCharsets.UTF_16))
      } yield {

         zio.Console.printLine("data:  " + data.toArray.toString)
        assertTrue(data.nonEmpty)
        assertTrue(data.toArray.map(_.toChar).mkString == "{\"codeiso3\":\"COL\",\"bu\":\"BU LatAm\",\"code\":\"CO\",\"country\":\"Colombia\"}" )
      }

    }

    ,
    test("send real request String)") {


      val bodyString = "{\"iso3Code\": { \"value\": \"COL\"}}"
      val BodyFromString = Body.fromString(bodyString)

      val url = URL.fromString("https://rest-service-marketingtables.fcil-env.com/api/v2.0/findNewBusinessUnitByCountryCodeIso3")

      //        val remoteAd = InetAddress.getByName("https://rest-service-marketingtables.fcil-env.com")
      val request = Request(
        BodyFromString,
        Headers( "Content-Type","application/json"),
        Method.POST,
        url.toOption.get,
        Version.Http_1_1,
        None





      )

      for {
        stream <- Ape.readers.restApiReaderString(request).apply
          .provideLayer(zio.http.Client.default)
        data <- stream.runCollect
        //new String( data.toArray, StandardCharsets.UTF_16))
      } yield {

        assertTrue(data.nonEmpty)
        assertTrue(data.head == "{\"codeiso3\":\"COL\",\"bu\":\"BU LatAm\",\"code\":\"CO\",\"country\":\"Colombia\"}" )
      }

    }
  )



}
