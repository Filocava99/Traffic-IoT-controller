package it.pps.ddos
import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.Public
import it.pps.ddos.device.sensor.{BasicSensor, Sensor, SensorActor}
import it.pps.ddos.utils.GivenDataType
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.grouping.*
import org.scalactic.Prettifier.default

import scala.collection.immutable.List
import scala.concurrent.duration.{Duration, FiniteDuration}
import com.github.nscala_time.time.Imports.*
import org.joda.time.DateTime
import it.pps.ddos.DBWriter

class DBGroupTest extends AnyFlatSpec:
  "A DBWriter" should "work" in testDBWriter()

  def testDBWriter(): Unit =
    val date = DateTime.now()
    DBWriter(99, Map(date -> Set((0,0),(6,2),(3,2)), date + 3.seconds -> Set((0,322))))
    while(true) {}

