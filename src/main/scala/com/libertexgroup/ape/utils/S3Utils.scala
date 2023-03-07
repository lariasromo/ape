package com.libertexgroup.ape.utils

import zio.Duration

import java.time.{LocalDateTime, ZoneOffset}

object S3Utils {
  def pathConverter(path: String, spacedDuration: Duration): LocalDateTime => List[String] = date => {
    val zero: Int => String = i => if (i < 10) s"0$i" else i.toString
    (
      date.minus(spacedDuration).toEpochSecond(ZoneOffset.UTC) / 3600L
        to
        date.toEpochSecond(ZoneOffset.UTC) / 3600L
      )
      .map(s => LocalDateTime.ofEpochSecond(s, 0, ZoneOffset.UTC))
      .map(dt => s"$path" +
        s"/year=${zero(dt.getYear)}" +
        s"/month=${zero(dt.getMonthValue)}" +
        s"/day=${zero(dt.getDayOfYear)}" +
        s"/hour=${zero(dt.getHour)}"
      ).toList
  }
}
