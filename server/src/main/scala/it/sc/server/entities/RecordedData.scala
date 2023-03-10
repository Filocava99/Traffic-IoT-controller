package it.sc.server.entities

import com.github.nscala_time.time.Imports.DateTime
import reactivemongo.api.bson.BSONObjectID

case class RecordedData(idCamera: BSONObjectID, timeStamp: DateTime, data: Set[(Int,Int)])
