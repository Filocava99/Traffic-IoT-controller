package it.sc.server.entities

import reactivemongo.api.bson.BSONObjectID

case class Camera(id: BSONObjectID, details: String)
