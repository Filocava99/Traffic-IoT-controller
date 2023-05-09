package it.sc.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import com.github.nscala_time.time.Imports.DateTime
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.typesafe.config.ConfigFactory
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import it.pps.ddos.grouping.tagging.{Deployable, MapTag, TriggerMode}
import it.sc.server.entities.{Camera, RecordedData}
import it.sc.server.mongodb.MongoDBClient
import it.sc.server.{IdAnswer, IdRequest}
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object ServerActor:

    //implicit object cameraEntryReader extends BSONDocumentReader[Camera]:
    //    def readDocument(doc: BSONDocument) = for {
    //      id <- doc.getAsTry[BSONObjectID]("_id")
    //      details <- doc.getAsTry[String]("details")
    //    } yield Camera(id, details)
    //
    //  implicit val cameraEntryWriter: BSONDocumentWriter[Camera] =
    //    BSONDocumentWriter[Camera] { entry =>
    //      BSONDocument("_id" -> entry.id,
    //        "details" -> entry.details)
    //    }

    // My settings (see available connection options)

    def apply(): Behavior[DeviceMessage] =
        Behaviors.setup[DeviceMessage] { context =>
            val collection: MongoCollection[Document] = MongoDBClient.getDB.get.getCollection("cameras")

            def checkCameraId(details: String): List[Camera] =
                val cursor = collection.find(Filters.eq("details", details)).iterator();
                var list: List[Camera] = List.empty
                while (cursor.hasNext) do
                    val document = cursor.next()
                    list = Camera(document.getObjectId("_id"), document.getString("details")) :: list
                list

            def getCameraId(details: String): ObjectId =
                val cameraList = checkCameraId(details)
                println("Camera list: " + cameraList)
                if cameraList.isEmpty then
                    val uuid = new ObjectId()
                    println("New camera detected: " + uuid.toHexString)
                    //istantiate the broadcaster that will forward camera status to clients
                    Deployer.deploy(
                        new MapGroup[RecordedData, RecordedData]("broadcaster-" + uuid.toHexString, Set.empty, List.empty, i => i)
                          with Deployable[RecordedData, List[RecordedData]](TriggerMode.NONBLOCKING(true)))

                    //save in the DB the new camera
                    val result = collection.insertOne(new Document()
                      .append("_id", uuid)
                      .append("details", details))
                    println("Success! Inserted document id: " + result.getInsertedId)
                    uuid
                else
                    val uuid = cameraList.head.id
                    Deployer.deploy(
                        new MapGroup[RecordedData, RecordedData]("broadcaster-" + uuid.toHexString, Set.empty, List.empty, i => i)
                          with Deployable[RecordedData, List[RecordedData]](TriggerMode.NONBLOCKING(true)))
                    uuid

            Behaviors.receivePartial { (context, message) =>
                message match
                    case IdRequest(details: String, replyTo: ActorRef[DeviceMessage]) =>
                        val id = getCameraId(details)
                        replyTo ! IdAnswer(id.toString)
                        Behaviors.same
                    case _ => println(s"[Server Actor] Unknown message - ${message}"); Behaviors.same
            }
        }


