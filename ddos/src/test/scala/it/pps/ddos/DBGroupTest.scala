package it.pps.ddos
import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.Public
import it.pps.ddos.device.sensor.{BasicSensor, Sensor, SensorActor}
import it.pps.ddos.utils.GivenDataType.given
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.grouping.*
import org.scalactic.Prettifier.default

import scala.collection.immutable.List
import scala.concurrent.duration.{Duration, FiniteDuration}
import com.github.nscala_time.time.Imports._

class DBGroupTest extends AnyFlatSpec:
  "A DBWriter" should "work" in testDBWriter()

  def testDBWriter(): Unit =
    DBWriter(99, Map(DateTime.now()->Set((0,0),(1,2), (1,2))))
    while(true) {}

