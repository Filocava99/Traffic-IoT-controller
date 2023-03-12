package it.sc.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import com.github.nscala_time.time.Imports.DateTime
import com.typesafe.config.ConfigFactory
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, Statuses}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import it.pps.ddos.grouping.tagging.{Deployable, MapTag, TriggerMode}
import it.sc.server.entities.RecordedData
import it.sc.server.{IdAnswer, IdRequest}
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.bson.*
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}

import scala.collection.immutable.List
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object StoringActor:

  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017"


  // Implicit document writer (scala DBEntry => mongoDB document)
  implicit val entryWriter: BSONDocumentWriter[RecordedData] =
    BSONDocumentWriter[RecordedData] { entry =>
      BSONDocument("idCamera" -> entry.idCamera,
        "timestamp" -> BSONDateTime(entry.timeStamp.toDate.getTime),
        "data" -> entry.data.toSet)
    }

  def apply(): Behavior[DeviceMessage] =
    Behaviors.setup[DeviceMessage] { context =>

      //Setup actor dispatcher for Future resolution
      implicit val executionContext: ExecutionContext =
        context.system.dispatchers.lookup(DispatcherSelector.fromConfig("akka.blocking-dispatcher"))

      // Connect to the database: Must be done only once per application
      val driver = AsyncDriver()
      val parsedUri = MongoConnection.fromString(mongoUri)

      // Database and collections: Get references
      val futureConnection = parsedUri.flatMap(driver.connect(_))
      def db1: Future[DB] = futureConnection.flatMap(_.database("TrafficFlow"))
      def entryCollection = db1.map(_.collection("storicCount"))

      Behaviors.receivePartial { (context, message) =>
        message match
          case Statuses[RecordedData](author, values: List[RecordedData]) =>
            values.map(entry =>
              //save in the DB the new recorded data
              entryCollection.flatMap(_.insert
                .one(RecordedData(entry.idCamera, entry.timeStamp, entry.data))
                .map(_ => {}))
            )
            Behaviors.same
          case _ => println("Unknown message"); Behaviors.same
      }
    }