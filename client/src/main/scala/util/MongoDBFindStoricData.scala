package util

import com.github.nscala_time.time.Imports.DateTime
import com.mongodb.ConnectionString
import com.mongodb.client.model.Filters
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import it.sc.server.entities.RecordedData
import it.sc.server.mongodb.MongoDBClient
import org.bson.codecs.{Codec, DecoderContext}
import org.bson.json.JsonReader
import org.bson.{BsonDocumentReader, Document}
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
      val codec: Codec[Map.Map2[Int, Int]] = MongoDBClient.getDB.get.getCodecRegistry.get(classOf[Map.Map2[Int, Int]])
      val dataInJson = document.get("data").asInstanceOf[Document].toJson()
      val jsonReader = new JsonReader(dataInJson)
      val data = codec.decode(jsonReader, DecoderContext.builder().build())
      list = RecordedData(document.getString("idCamera"), document.getLong("timestamp"), data) :: list
    entries = list
    println(s"successfully read with result: $entries")

  /**
   * Returns only the data in the database that match the specified idCamera
   *
   * @param idCamera
   */
  def apply(idCamera: String): Unit =
    val cursor = collection.find(Filters.eq("idCamera", idCamera)).iterator();
    var list: List[RecordedData] = List.empty
    while (cursor.hasNext) do
      val document = cursor.next()
      val codec: Codec[HashMap[Int, Int]] = MongoDBClient.getDB.get.getCodecRegistry.get(classOf[HashMap[Int, Int]])
      val dataInJson = document.get("data").asInstanceOf[Document].toJson()
      val jsonReader = new JsonReader(dataInJson)
      val data = codec.decode(jsonReader, DecoderContext.builder().build())
      list = RecordedData(document.getString("idCamera"), document.getLong("timestamp"), data) :: list
    entries = list
    println(s"successfully read with result: $entries")