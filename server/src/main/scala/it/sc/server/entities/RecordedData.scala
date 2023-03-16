package it.sc.server.entities

import com.github.nscala_time.time.Imports.DateTime

case class RecordedData(idCamera: String, timeStamp: DateTime, data: Map[Int, Int])
