package view

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.github.nscala_time.time.Imports.*
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.Public
import it.pps.ddos.device.sensor.{BasicSensor, Sensor, SensorActor}
import it.pps.ddos.grouping.*
import it.pps.ddos.utils.GivenDataType
import util.{ MongoDBFindCameras, MongoDBFindStoricData }
import org.scalactic.Prettifier.default
import org.scalatest.flatspec.AnyFlatSpec
import scalafx.scene.control.Button

import scala.collection.immutable.List
import scala.concurrent.duration.{Duration, FiniteDuration}

class DBGroupTest extends AnyFlatSpec:
  "A MongoDBFind" should "returns all the data in the database" in testMongoDBFindAll()
  "A MongoDBFind" should "returns only the data in the database that match the specified ID" in testMongoDBFindByID()

  def testMongoDBFindAll(): Unit =
    MongoDBFindCameras()
    Thread.sleep(10000) // waiting for the read operation to complete

  def testMongoDBFindByID(): Unit =
    //MongoDBFindCameras(new ObjectId("64171d4ea9b70f6f6e4d9241").get)
    Thread.sleep(10000) // waiting for the read operation to complete