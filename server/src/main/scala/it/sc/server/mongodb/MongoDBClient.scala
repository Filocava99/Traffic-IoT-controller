package it.sc.server.mongodb

import com.github.nscala_time.time.Imports.DateTime
import com.mongodb.{ConnectionString, MongoClientSettings}
import com.mongodb.client.{MongoClient, MongoClients, MongoDatabase}
import com.mongodb.connection.ClusterSettings
import it.sc.server.entities.{Camera, RecordedData}
import org.bson.codecs.configuration.CodecRegistries
import reactivemongo.api.{AsyncDriver, DB, MongoConnection}
import reactivemongo.api.bson.{BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONObjectID}

import scala.concurrent.{ExecutionContext, Future}

object MongoDBClient {

    private val mongoUri = "mongodb://localhost:27017"
    private val database: Option[MongoDatabase] = connect()

    private def connect() = {
        try {
            val mongoClient: MongoClient = MongoClients.create(MongoClientSettings.builder
              .applyConnectionString(new ConnectionString(mongoUri))
              .codecRegistry(
                  CodecRegistries.fromRegistries(
                      CodecRegistries.fromCodecs(new MapToBSON(), new HashMapToBSON()),
                      MongoClientSettings.getDefaultCodecRegistry))
              .build)
            Some(mongoClient.getDatabase("TrafficFlow"));
        } catch {
            case e: Exception => println("Error connecting to MongoDB: " + e.getMessage); None
            case _ => None
        }
    }

    def getDB: Option[MongoDatabase] = database

}
