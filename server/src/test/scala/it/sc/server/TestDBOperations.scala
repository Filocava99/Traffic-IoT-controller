
package it.sc.server

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.github.nscala_time.time.Imports.*
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.Public
import it.pps.ddos.device.sensor.{BasicSensor, Sensor, SensorActor}
import it.pps.ddos.grouping.*
import it.pps.ddos.utils.GivenDataType
import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalactic.Prettifier.default
import reactivemongo.api.bson.BSONObjectID

import scalafx.scene.control.Button

import java.util.UUID
import scala.collection.immutable.List
import scala.concurrent.duration.{Duration, FiniteDuration}
import it.sc.server.IdAnswer
import it.sc.server.IdRequest
import it.sc.server.ServerActor

class TestDBOperations extends AnyFlatSpec:
  //"A DBWriter" should "work" in testDBWriter()
  "The management of a new camera id" should "be done properly" in testCameraInit()
  /*"A MongoDBFind" should "returns all the data in the database" in testMongoDBFindAll()
  "A MongoDBFind" should "returns only the data in the database that match the specified ID" in testMongoDBFindByID()*/

  val testKit: ActorTestKit = ActorTestKit()
  private var testProbe = testKit.createTestProbe[DeviceMessage]()

//  def testDBWriter(): Unit =
//    val date = DateTime.now()
//    DBWriter(99, Map(date -> Set((0,0),(6,2),(3,2)), date + 3.seconds -> Set((0,322))))
//    while(true) {}

 /* def testMongoDBFindAll(): Unit =
    MongoDBFind()
    Thread.sleep(10000) // waiting for the read operation to complete

  def testMongoDBFindByID(): Unit =
    MongoDBFind(99)
    Thread.sleep(10000) // waiting for the read operation to complete*/

  def testCameraInit(): Unit =
    val serverRef = testKit.spawn(ServerActor())
    serverRef ! IdRequest("test", testProbe.ref)
    Thread.sleep(500)
    testProbe.expectMessage(IdAnswer(BSONObjectID.generate().stringify))

