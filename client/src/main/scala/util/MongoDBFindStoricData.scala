package util

import com.github.nscala_time.time.Imports.DateTime
import com.mongodb.ConnectionString
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import it.sc.server.entities.RecordedData
import it.sc.server.mongodb.MongoDBClient
import org.bson.Document
import util.MongoDBFindCameras.collection

import scala.collection.immutable.HashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object MongoDBFindStoricData:
  private var entries: List[RecordedData] = List.empty

  def storicData: List[RecordedData] = entries

  val collection: MongoCollection[Document] = MongoDBClient.getDB.get.getCollection("storicCount")

  /**
   * Returns all the data in the database
   */
  def apply(): Unit =
    val cursor = collection.find().iterator();
    var list: List[RecordedData] = List.empty
    while (cursor.hasNext) do
      val document = cursor.next()
      list = RecordedData(document.getString("idCamera"), document.getLong("timestamp"), document.get("data", classOf[HashMap[Int, Int]])) :: list
    entries = list
    println(s"successfully read with result: $entries")

  /**
   * Returns only the data in the database that match the specified idCamera
   *
   * @param idCamera
   */
  def apply(idCamera: String): Unit =
    val cursor = collection.find(Filters.eq("cameraId", idCamera)).iterator();
    var list: List[RecordedData] = List.empty
    while (cursor.hasNext) do
      val document = cursor.next()
      list = RecordedData(document.getString("idCamera"), document.getLong("timestamp"), document.get("data", classOf[HashMap[Int, Int]])) :: list
    entries = list
    println(s"successfully read with result: $entries")