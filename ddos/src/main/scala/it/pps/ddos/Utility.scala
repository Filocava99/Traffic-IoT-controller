package it.pps.ddos

import com.github.nscala_time.time.Imports.*
import org.joda.time.DateTime
import it.pps.ddos.DBWriter.{db1, entryCollection}
import javafx.event.{ActionEvent, Event}
import reactivemongo.api.*
import reactivemongo.api.bson.*
import reactivemongo.api.bson.collection.BSONCollection
import reactivemongo.api.commands.WriteResult

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scalafx.scene.control.Button

case class DBEntry(idCamera: Int, time: DateTime, data: Set[(Int,Int)])

object DBWriter:

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

  // Implicit document writer (scala DBEntry => mongoDB document)
  implicit val entryWriter: BSONDocumentWriter[DBEntry] =
  BSONDocumentWriter[DBEntry] { entry =>
    BSONDocument("idCamera" -> entry.idCamera,
                 "timestamp" -> BSONDateTime(entry.time.toDate.getTime),
                 "data" -> entry.data)
  }

  def apply(idCamera: Int, entryMap: Map[DateTime, Set[(Int,Int)]]): Unit =
    val writeRes = entryCollection.flatMap(_.insert
      .many(entryMap.map((date, values) => DBEntry(idCamera, date, values)))
      .map(_ => {}))

    writeRes.onComplete { // Dummy callbacks
      case Failure(e) => e.printStackTrace()
      case Success(writeResult) =>
        println(s"successfully inserted document with result: $writeResult")
    }

    writeRes.map(_ => {}) // in this example, do nothing with the success

    println("F2 is COMPLETED")


object MongoDBFind:
  private var entries: List[DBEntry] = List.empty

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

  implicit val entryReader: BSONDocumentReader[DBEntry] =
    BSONDocumentReader.from[DBEntry] { doc =>
      for {
        idCamera <- doc.getAsTry[Int]("idCamera")
        time <- doc.getAsTry[BSONDateTime]("timestamp")
        data <- doc.getAsTry[Set[(Int, Int)]]("data")
      } yield DBEntry(idCamera, new DateTime(Date(time.value)), data)
    }

  def data: List[DBEntry] = entries

  /**
   * Returns all the data in the database
   */
  def apply(): Unit =
    val readRes = entryCollection.flatMap(_.find(document())
      .cursor[DBEntry]() // using the result cursor
      .collect[List](-1, Cursor.FailOnError[List[DBEntry]]()))

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
  def apply(idCamera: Int): Unit =
    val readRes = entryCollection.flatMap(_.find(document("idCamera" -> idCamera))
      .cursor[DBEntry]() // using the result cursor
      .collect[List](-1, Cursor.FailOnError[List[DBEntry]]()))

    readRes.onComplete { // Dummy callbacks
      case Failure(e) => e.printStackTrace()
      case Success(readResult) =>
        entries = readResult
        println(s"successfully read with result: $readResult")
    }

