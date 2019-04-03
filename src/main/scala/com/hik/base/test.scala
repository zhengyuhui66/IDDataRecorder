package com.hik.base

import java.text.SimpleDateFormat
import java.util.{Calendar, Date, GregorianCalendar}

import com.hik.base.util.{CETCProtocol, CommFunUtils}

object test {
  def main(args: Array[String]): Unit = {
    val l="F000037F23F8E33A0C8CAF68A3C466B9DC"
    val m=l.substring(l.length-12)
    println(m)
  }
}
