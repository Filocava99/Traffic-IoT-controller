package util

import com.github.nscala_time.time.Imports.DateTime
import it.sc.server.entities.RecordedData
import reactivemongo.api.{AsyncDriver, Cursor, DB, MongoConnection}
import reactivemongo.api.bson.{BSONDateTime, BSONDocumentReader, document}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object MongoDBFindStoricData:
  private var entries: List[RecordedData] = List.empty

  // My settings (see available connection options)
  val mongoUri = "mongodb://localhost:27017"

  import ExecutionContext.Implicits.global // use any appropriate context

  // Connect to the database: Must be done only once per application
  val driver = AsyncDriver()
  val parsedUri = MongoConnection.fromString(mongoUri)

  // Database and collections: Get references
  val futureConnection = parsedUri.flatMap(driver.connect(_))

  def db1: Future[DB] = futureConnection.flatMap(_.database("TrafficFlow"))

  def entryCollection = db1.map(_.collection("storicCount"))

  implicit val entryReader: BSONDocumentReader[RecordedData] =
    BSONDocumentReader.from[RecordedData] { doc =>
      for {
        idCamera <- doc.getAsTry[String]("idCamera")
        time <- doc.getAsTry[BSONDateTime]("timestamp")
        data <- doc.getAsTry[Set[(Int, Int)]]("data")
      } yield RecordedData(idCamera, time, data.toMap)
    }

  def storicData: List[RecordedData] = entries

  /**
   * Returns all the data in the database
   */
  def apply(): Unit =
    val readRes = entryCollection.flatMap(_.find(document())
      .cursor[RecordedData]() // using the result cursor
      .collect[List](-1, Cursor.FailOnError[List[RecordedData]]()))

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
  def apply(idCamera: String): Unit =
    val readRes = entryCollection.flatMap(_.find(document("idCamera" -> idCamera))
      .cursor[RecordedData]() // using the result cursor
      .collect[List](-1, Cursor.FailOnError[List[RecordedData]]()))

    readRes.onComplete { // Dummy callbacks
      case Failure(e) => e.printStackTrace()
      case Success(readResult) =>
        entries = readResult
        println(s"successfully read with result: $readResult")
    }

