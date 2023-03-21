package util

import it.sc.server.entities.Camera
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}
import reactivemongo.api.bson.{BSONDocumentReader, BSONObjectID, document}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object MongoDBFindCameras:
  private var entries: List[Camera] = List.empty

  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017"

  import ExecutionContext.Implicits.global // use any appropriate context

  // Connect to the database: Must be done only once per application
  val driver = AsyncDriver()
  val parsedUri = MongoConnection.fromString(mongoUri)

  // Database and collections: Get references
  val futureConnection = parsedUri.flatMap(driver.connect(_))

  def db1: Future[DB] = futureConnection.flatMap(_.database("TrafficFlow"))

  def entryCollection = db1.map(_.collection("cameras"))

  implicit val entryReader: BSONDocumentReader[Camera] =
    BSONDocumentReader.from[Camera] { doc =>
      for {
        idCamera <- doc.getAsTry[BSONObjectID]("_id")
        data <- doc.getAsTry[String]("details")
      } yield Camera(idCamera, data)
    }

  def cameras: List[Camera] = entries

  /**
   * Returns all the data in the database
   */
  def apply(): Unit =
    val readRes = entryCollection.flatMap(_.find(document())
      .cursor[Camera]() // using the result cursor
      .collect[List](-1, Cursor.FailOnError[List[Camera]]()))

    readRes.onComplete { // Dummy callbacks
      case Failure(e) => e.printStackTrace()
      case Success(readResult) =>
        entries = readResult
        println(s"successfully read with result: $readResult")
    }

  /**
   * Returns only the data in the database that match the specified idCamera
   *
   * @param idCamera
   */
  def apply(idCamera: BSONObjectID): Unit =
    val readRes = entryCollection.flatMap(_.find(document("_id" -> idCamera))
      .cursor[Camera]() // using the result cursor
      .collect[List](-1, Cursor.FailOnError[List[Camera]]()))

    readRes.onComplete { // Dummy callbacks
      case Failure(e) => e.printStackTrace()
      case Success(readResult) =>
        entries = readResult
        println(s"successfully read with result: $readResult")
    }

