package com.libertexgroup.ape.utils

import com.libertexgroup.configs.S3Config
import zio.{Duration, ZIO}

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

object S3Utils {
  def dateRange(start: ZonedDateTime, end: ZonedDateTime, step: Duration) = {
    val s = (start.toEpochSecond - start.toEpochSecond) / step.toSeconds
    val e = (end.toEpochSecond - start.toEpochSecond) / step.toSeconds
    (s to e).map(s => end.minus(step multipliedBy s))
  }

  def pathConverter(path: String):ZIO[S3Config, Nothing, ZonedDateTime => List[String]] = for {
    config <- ZIO.service[S3Config]
  } yield {
      val conv: ZonedDateTime => List[String] = date => {
      val zero: Int => String = i => if (i < 10) s"0$i" else i.toString
      val margin = config.filePeekDurationMargin.orNull
      date.minus(margin).toEpochSecond
        .to(date.toEpochSecond)
        .map(s => LocalDateTime.ofEpochSecond(s, 0, ZoneOffset.UTC))
        .map(dt => {
          val location = s"$path" +
            s"/year=${zero(dt.getYear)}" +
            s"/month=${zero(dt.getMonthValue)}" +
            s"/day=${zero(dt.getDayOfMonth)}" +
            s"/hour=${zero(dt.getHour)}"
          location
        })
        .toList
    }
    conv
  }
}
