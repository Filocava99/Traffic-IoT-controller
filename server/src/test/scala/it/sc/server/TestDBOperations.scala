
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
import org.scalatest.flatspec.AnyFlatSpec
import org.scalactic.Prettifier.default
import scalafx.scene.control.Button

import java.util.UUID
import scala.collection.immutable.List
import scala.concurrent.duration.{Duration, FiniteDuration}
import it.sc.server.IdAnswer
import it.sc.server.IdRequest
import it.sc.server.ServerActor
import org.bson.types.ObjectId

class TestDBOperations extends AnyFlatSpec:
  "The management of a new camera id" should "be done properly" in testCameraInit()

  val testKit: ActorTestKit = ActorTestKit()
  private var testProbe = testKit.createTestProbe[DeviceMessage]()

  def testCameraInit(): Unit =
    val serverRef = testKit.spawn(ServerActor())
    serverRef ! IdRequest("test", testProbe.ref)
    Thread.sleep(500)
    testProbe.expectMessage(IdAnswer(new ObjectId().toHexString))

