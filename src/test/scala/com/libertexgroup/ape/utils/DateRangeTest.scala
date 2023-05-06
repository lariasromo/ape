package com.libertexgroup.ape.utils

import com.libertexgroup.utils.S3Utils
import zio.test.{Spec, TestEnvironment, ZIOSpec, assertTrue}
import zio.{Scope, ZIO, ZLayer, durationInt}

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

object DateRangeTest extends ZIOSpec[Any] {
  val range: Seq[String] = {
    val now = ZonedDateTime
      .of(LocalDateTime.of(2023, 4, 1, 10, 0), ZoneId.of("UTC"))
    val start = now.minus(1.days)
    val end = now
    val step: zio.Duration = 1.hour
    val pattern: ZonedDateTime => String = d => {
      s"${d.getYear}-${d.getMonthValue}-${d.getDayOfMonth} ${d.getHour}"
    }
    S3Utils.dateRange(start, end, step).map(pattern)
  }

  override def spec: Spec[Any with TestEnvironment with Scope, Any] =
    suite("DateRangeTest")(
      test("Dates are formed"){
        for {
          dates <- ZIO.succeed(range)
        } yield {
          val expDates = Seq(
            "2023-4-1 10", "2023-4-1 9", "2023-4-1 8", "2023-4-1 7", "2023-4-1 6", "2023-4-1 5",
            "2023-4-1 4", "2023-4-1 3", "2023-4-1 2", "2023-4-1 1", "2023-4-1 0", "2023-3-31 23", "2023-3-31 22",
            "2023-3-31 21", "2023-3-31 20", "2023-3-31 19", "2023-3-31 18", "2023-3-31 17", "2023-3-31 16",
            "2023-3-31 15", "2023-3-31 14", "2023-3-31 13", "2023-3-31 12", "2023-3-31 11", "2023-3-31 10"
          )
          assertTrue(dates.equals(expDates))
        }
      }
    )

  override def bootstrap: ZLayer[Any, Any, Any] = ZLayer.empty
}
