package it.sc.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.github.nscala_time.time.Imports.DateTime
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Message}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import it.sc.server.{IdAnswer, IdRequest}
import reactivemongo.api.{AsyncDriver, DB, MongoConnection}
import reactivemongo.api.bson.{BSONDateTime, BSONDocument, BSONDocumentReader, BSONDocumentWriter}
import reactivemongo.api.Cursor
import reactivemongo.api.bson.collection.BSONCollection
import it.pps.ddos.grouping.tagging.{Deployable, MapTag, TriggerMode}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.util.Failure
import akka.actor.typed.DispatcherSelector
import com.typesafe.config.ConfigFactory

case class Camera(id: UUID, details: String)
case class DataCamera(idCamera: Int, timeStamp: DateTime, data: Map[Int, Int])

object ServerActor:

  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017"
  //ConfigFactory.load("application.conf")


  implicit object cameraEntryReader extends BSONDocumentReader[Camera]:
    def readDocument(doc: BSONDocument) = for {
      id <- doc.getAsTry[UUID]("_id")
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
        context.system.dispatchers.lookup(DispatcherSelector.fromConfig("akka.my-blocking-dispatcher"))

      // Connect to the database: Must be done only once per application
      val driver = AsyncDriver()
      val parsedUri = MongoConnection.fromString(mongoUri)

      // Database and collections: Get references
      val futureConnection = parsedUri.flatMap(driver.connect(_))
      def db1: Future[DB] = futureConnection.flatMap(_.database("TrafficFlow"))
      def entryCollection = db1.map(_.collection("cameras"))
      def getCameraId(details: String): Future[List[Camera]] =
        val query = BSONDocument("details" -> BSONDocument(f"$$eq" -> details))

        entryCollection.flatMap(_.find(query).
          cursor[Camera](). // ... collect in a `List`
          collect[List](-1, Cursor.FailOnError[List[Camera]]()))

      Behaviors.receivePartial { (context, message) =>
        //implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-blocking-dispatcher")
        message match
          case IdRequest(details: String, replyTo: ActorRef[DeviceMessage]) =>

            getCameraId(details).onComplete {
              case Success(cameraList) =>
                cameraList.isEmpty match
                  case true => //the camera is new to the system => create an id, a broadcaster and save the new anagraphics in the database
                    val uuid = UUID.randomUUID()

                    //istantiate the broadcaster that will forward camera status to clients
                    Deployer.deploy(
                      new MapGroup[DataCamera, DataCamera]("broadcaster-" + uuid.toString, Set.empty, List.empty, i=>i)
                        with Deployable[DataCamera, List[DataCamera]](TriggerMode.NONBLOCKING(true)))

                    //save in the DB the new camera
                    entryCollection.flatMap(_.insert
                      .one(Camera(uuid, details))
                      .map(_ => {}))
                    replyTo ! IdAnswer(uuid)

                  case false =>
                    replyTo ! IdAnswer(cameraList.head.id)

              case Failure(e) => e.printStackTrace()
            }
            Behaviors.same
          case IdAnswer(uuid) => println(uuid); Behaviors.same
          case _ => println("Unknown message"); Behaviors.same
      }
    }


