package it.sc.server

import concurrent.duration.DurationInt
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import com.github.nscala_time.time.Imports.DateTime
import com.typesafe.config.ConfigFactory
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import it.pps.ddos.grouping.tagging.{Deployable, MapTag, TriggerMode}
import it.sc.server.{IdAnswer, IdRequest}
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import it.sc.server.entities.Camera
import it.sc.server.entities.RecordedData

object ServerActor:

  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017"

  implicit object cameraEntryReader extends BSONDocumentReader[Camera]:
    def readDocument(doc: BSONDocument) = for {
      id <- doc.getAsTry[BSONObjectID]("_id")
      details <- doc.getAsTry[String]("details")
    } yield Camera(id, details)

  implicit val cameraEntryWriter: BSONDocumentWriter[Camera] =
    BSONDocumentWriter[Camera] { entry =>
      BSONDocument("_id" -> entry.id,
        "details" -> entry.details)
    }

  def apply(): Behavior[DeviceMessage] =
    Behaviors.setup[DeviceMessage] { context =>

      //Setup actor dispatcher for Future resolution
      implicit val executionContext: ExecutionContext =
        context.system.dispatchers.lookup(DispatcherSelector.fromConfig("akka.sequential-dispatcher"))

      // Connect to the database: Must be done only once per application
      val driver = AsyncDriver()
      val parsedUri = MongoConnection.fromString(mongoUri)

      // Database and collections: Get references
      val futureConnection = parsedUri.flatMap(driver.connect(_))
      def db1: Future[DB] = futureConnection.flatMap(_.database("TrafficFlow"))
      def entryCollection = db1.map(_.collection("cameras"))

      def checkCameraId(details: String): Future[List[Camera]] =
        val query = BSONDocument("details" -> BSONDocument(f"$$eq" -> details))
        entryCollection.flatMap(_.find(query).
          cursor[Camera](). // ... collect in a `List`
          collect[List](-1, Cursor.FailOnError[List[Camera]]()))

      def getCameraId(details: String): Future[BSONObjectID] =
        checkCameraId(details).flatMap(cameraList => Future {
          cameraList.isEmpty match
            case true => //the camera is new to the system => create an id, a broadcaster and save the new anagraphics in the database
              val uuid = BSONObjectID.generate()

              //istantiate the broadcaster that will forward camera status to clients
              Deployer.deploy(
                new MapGroup[RecordedData, RecordedData]("broadcaster-" + uuid.stringify, Set.empty, List.empty, i => i)
                  with Deployable[RecordedData, List[RecordedData]](TriggerMode.NONBLOCKING(true)))

              //save in the DB the new camera
              entryCollection.flatMap(_.insert
                .one(Camera(uuid, details))
                .map(_ => {}))
              uuid

            case false =>
              cameraList.head.id
        })

      Behaviors.receivePartial { (context, message) =>
        message match
          case IdRequest(details: String, replyTo: ActorRef[DeviceMessage]) =>
            val id = Await.result(getCameraId(details), 2.seconds)
            replyTo ! IdAnswer(id)
            Behaviors.same
          case IdAnswer(uuid) => println(uuid.stringify); Behaviors.same
          case _ => println("Unknown message"); Behaviors.same
      }
    }


