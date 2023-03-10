package com.libertexgroup.ape.utils

import zio.Duration

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

object S3Utils {
  def pathConverter(path: String, spacedDuration: Duration): ZonedDateTime => List[String] = date => {
    val zero: Int => String = i => if (i < 10) s"0$i" else i.toString
      date.minus(spacedDuration.multipliedBy(2)).toEpochSecond
        .to( date.toEpochSecond)
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
}
