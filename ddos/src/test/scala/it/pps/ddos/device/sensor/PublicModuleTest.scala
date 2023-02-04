package it.pps.ddos.device.sensor

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.Public
import it.pps.ddos.device.sensor.{BasicSensor, Sensor, SensorActor}
import it.pps.ddos.utils.GivenDataType.given
import it.pps.ddos.utils.DataType
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.immutable.List
import scala.concurrent.duration.Duration

class PublicModuleTest extends AnyFlatSpec:
  "A user-defined sensor that extends the 'Public' module" should "be able to accept subscription requests" in testSubscription()
  it should "broadcast its status to all subscribed actors" in testBroadcast()
  "and" should "propagate its status independently from who request the propagation" in testPublicPropagate()
  it should "be possible to unsubscribe from that sensor to stop receiving its status" in testUnsubscription()

  val testKit: ActorTestKit = ActorTestKit()

  private def preparePublicSensor(): ActorRef[DeviceMessage] =
    class PublicSensor extends BasicSensor[String]("1", List.empty) with Public[String]
    val sensor = testKit.spawn(SensorActor(new PublicSensor).behavior())
    sensor ! UpdateStatus("BroadcastTest")
    sensor

  private def testSubscription(): Unit =
    val sensor = preparePublicSensor()
    val testProbe = testKit.createTestProbe[Message]()
    sensor ! Subscribe(testProbe.ref)
    Thread.sleep(800)
    testProbe.expectMessage(SubscribeAck(sensor.ref))

  private def testBroadcast(): Unit =
    val sensor = preparePublicSensor()
    val probeList = for i <- 1 to 3 yield testKit.createTestProbe[Message]()
    for(testProbe <- probeList) sensor ! Subscribe(testProbe.ref)
    sensor ! PropagateStatus(sensor)
    Thread.sleep(800)
    for(testProbe <- probeList)
      testProbe.expectMessage(SubscribeAck(sensor))
      testProbe.expectMessage(Status(sensor, "BroadcastTest"))

  private def testPublicPropagate(): Unit =
    val sensor = preparePublicSensor()
    val testProbe = testKit.createTestProbe[Message]()
    sensor ! Subscribe(testProbe.ref)
    sensor ! PropagateStatus(testProbe.ref)
    testProbe.expectMessage(SubscribeAck(sensor))
    testProbe.expectMessage(Status(sensor, "BroadcastTest"))


  private def testUnsubscription(): Unit =
    val sensor = preparePublicSensor()
    val probeList = for i <- 1 to 3 yield testKit.createTestProbe[Message]()
    for(testProbe <- probeList) sensor ! Subscribe(testProbe.ref)
    sensor ! Unsubscribe(probeList(1).ref)
    sensor ! PropagateStatus(sensor)
    Thread.sleep(800)
    for(testProbe <- probeList) testProbe.expectMessage(SubscribeAck(sensor))
    for(i <- 0 to 2) i match {
      case 1 => probeList(1).expectMessage(UnsubscribeAck(sensor))
        probeList(1).expectNoMessage(Duration.create(800, "milliseconds"))
      case n => probeList(n).expectMessage(Status(sensor, "BroadcastTest"))
    }

