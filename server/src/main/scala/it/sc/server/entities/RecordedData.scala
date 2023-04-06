package it.sc.server.entities

import reactivemongo.api.bson.BSONDateTime

case class RecordedData(idCamera: String, timeStamp: BSONDateTime, data: Map[Int, Int])
