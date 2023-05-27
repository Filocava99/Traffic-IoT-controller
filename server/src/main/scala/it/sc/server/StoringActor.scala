package it.sc.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, DispatcherSelector}
import com.github.nscala_time.time.Imports.DateTime
import com.typesafe.config.ConfigFactory
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.DeviceProtocol.{AckedStatus, DeviceMessage, Message, Statuses}
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import it.pps.ddos.grouping.tagging.{Deployable, MapTag, TriggerMode}
import it.sc.server.entities.RecordedData
import it.sc.server.mongodb.MongoDBClient
import it.sc.server.{IdAnswer, IdRequest}
import org.bson.Document
import org.bson.types.ObjectId

import scala.collection.immutable.{HashMap, List}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success}

object StoringActor:

  def apply(): Behavior[DeviceMessage] =
    Behaviors.setup[DeviceMessage] { context =>

      //Setup actor dispatcher for Future resolution
      implicit val executionContext: ExecutionContext =
        context.system.dispatchers.lookup(DispatcherSelector.fromConfig("akka.blocking-dispatcher"))

      Behaviors.receivePartial { (context, message) =>
        message match
//          case Statuses(author, values: List[RecordedData]) =>
//            val entryCollection = MongoDBClient.getDB.get.getCollection("storicCount")
//            values.foreach(entry =>
//              println("Received data from: " + author)
//              println(entry)
//              println("Inserting data in the DB")
//              val result = entryCollection.insertOne(new Document()
//                .append("_id", new ObjectId())
//                .append("idCamera", entry.idCamera)
//                .append("timeStamp", entry.timeStamp)
//                .append("data", entry.data.asInstanceOf[Map.Map2[Int, Int]]))
//              println("Data inserted: " + result.getInsertedId)
//            )
//            Behaviors.same
          case Statuses(author, values: List[Map[String, Object]]) =>
            val entryCollection = MongoDBClient.getDB.get.getCollection("storicCount")
            values.foreach(entry =>
              println("Received data from: " + author)
              println(entry)
              println("Inserting data in the DB")
              val result = entryCollection.insertOne(new Document()
                .append("_id", new ObjectId())
                .append("idCamera", entry("idCamera").asInstanceOf[String])
                .append("timeStamp", entry("timeStamp").asInstanceOf[Long])
                .append("data", entry("data").asInstanceOf[HashMap[Int, Int]]))
              println("Data inserted: " + result.getInsertedId)
              )
            Behaviors.same
          case AckedStatus(author, key, value) => context.self ! Statuses(author, List(value)); Behaviors.same
          case _ => println(s"[Storing Actor] Unknown message - ${message}"); Behaviors.same
      }
    }