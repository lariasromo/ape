package com.libertexgroup.ape

import zio.stream.ZStream

package object utils {
  def recursiveListFiles(f: java.io.File): Array[java.io.File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  def getFileTree(f: java.io.File): ZStream[Any, Throwable, java.io.File] =
    ZStream.fromIterable(recursiveListFiles(f).filter(_.isFile))
}
