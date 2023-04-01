package com.libertexgroup

import zio.{durationInt, durationLong}

import java.time.ZonedDateTime

object ex {
  def main(args: Array[String]) = {
    val start = ZonedDateTime.now().minus(10.days)
    val end = ZonedDateTime.now()
    val step: zio.Duration = 1.hour
    val pattern: ZonedDateTime => String = d => {
      s"${d.getYear}-${d.getMonthValue}-${d.getDayOfMonth} ${d.getHour}"
    }
    val s = (start.toEpochSecond - start.toEpochSecond) / step.toSeconds
    val e = (end.toEpochSecond - start.toEpochSecond) / step.toSeconds
    val dates = (s to e).map(s => end.minus(step.multipliedBy(s)))

    dates.map(pattern).foreach(println(_))
  }

}
