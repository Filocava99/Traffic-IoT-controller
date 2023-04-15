package it.sc.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import com.github.nscala_time.time.Imports.DateTime
import com.typesafe.config.ConfigFactory
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message, Statuses, AckedStatus}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import it.pps.ddos.grouping.tagging.{Deployable, MapTag, TriggerMode}
import it.sc.server.entities.RecordedData
import it.sc.server.{IdAnswer, IdRequest}
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}

import scala.collection.immutable.List
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object StoringActor:

  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017"

  // Implicit document writer (scala DBEntry => mongoDB document)
  implicit val entryWriter2: BSONDocumentWriter[RecordedData] =
    BSONDocumentWriter[RecordedData] { entry  =>
      println("Writing data to DB")
      println("data" -> entry.data.map(x => x._1.toString -> x._2))
      println("idCamera" -> entry.idCamera)
      println("timestamp" -> DateTime.now().getMillis)
      println(BSONDocument(
        "idCamera" -> entry.idCamera,
        "timestamp" -> DateTime.now().getMillis,
        "data" -> entry.data.map(x => x._1.toString -> x._2)))
      BSONDocument(
        "idCamera" -> entry.idCamera,
        "timestamp" -> DateTime.now().getMillis
        //"data" -> entry.data.map(x => BSONDocument(x._1.toString -> x._2))
      )
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
          case Statuses(author, values: List[Map[String, Object]]) =>
            values.foreach(entry =>
              println("Received data from: " + author)
              println(entry)
              //save in the DB the new recorded data
              entryCollection.flatMap(_.insert.one(RecordedData(entry("idCamera").asInstanceOf[String], entry("timeStamp").asInstanceOf[Long], entry("data").asInstanceOf[Map[Int, Int]]))
                .map(_ => {})) //await
            )
            Behaviors.same
          case AckedStatus(author, key, value) => context.self ! Statuses(author, List(value)); Behaviors.same
          case _ => println("Unknown message"); Behaviors.same
      }
    }