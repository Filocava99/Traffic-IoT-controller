package it.sc.server.entities

case class RecordedData(idCamera: String, timeStamp: Long, data: Map[Int, Int])
