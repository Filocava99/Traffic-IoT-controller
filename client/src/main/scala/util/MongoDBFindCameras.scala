package util

import com.mongodb.ConnectionString
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import it.sc.server.entities.Camera
import it.sc.server.mongodb.MongoDBClient
import org.bson.Document
import org.bson.types.ObjectId

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object MongoDBFindCameras:
  private var entries: List[Camera] = List.empty


  val collection: MongoCollection[Document] = MongoDBClient.getDB.get.getCollection("cameras")

  def cameras: List[Camera] = entries

  /**
   * Returns all the data in the database
   */
  def apply(): Unit =
    val cursor = collection.find().iterator();
    var list: List[Camera] = List.empty
    while (cursor.hasNext) do
      val document = cursor.next()
      list = Camera(document.getObjectId("_id"), document.getString("details")) :: list
    entries = list
    println(s"successfully read with result: $entries")

  /**
   * Returns only the data in the database that match the specified idCamera
   *
   * @param idCamera
   */
  def apply(idCamera: String): Unit =
    val cursor = collection.find(Filters.eq("_id", new ObjectId(idCamera))).iterator();
    var list: List[Camera] = List.empty
    while (cursor.hasNext) do
      val document = cursor.next()
      list = Camera(document.getObjectId("_id"), document.getString("details")) :: list
    entries = list
    println(s"successfully read with result: $entries")