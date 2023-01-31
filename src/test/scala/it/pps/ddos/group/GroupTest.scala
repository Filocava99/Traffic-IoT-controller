package it.pps.ddos.group

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

class GroupTest extends AnyFlatSpec:

  "A group of devices" should "apply a user defined function to each sensor of the group and produce an aggregatd output" in testBasicFunctionality()
  it should "be possible to nest groups within each other to perform more complex aggregations" in testNestingGroups()
  "A blocking group of devices" should "not compute the aggregated result until all devices has sent their status" in testBlockingBehavior()
  it should "be possible to update already stored values until computation is triggered" in testUpdatingValues()
  "A non-blocking group of devices" should "trigger its computation whenever a new value is received from the sources list" in testNonBlockingBehavior()
  "A ReduceGroup" should "reduce the whole list of values in an aggregated single value" in testReduce()

  val testKit: ActorTestKit = ActorTestKit()

  private def preparePublicSensor(id: String): ActorRef[DeviceMessage] =
    class PublicSensor extends BasicSensor[String](id, List.empty) with Public[String]
    val sensor = testKit.spawn(SensorActor(new PublicSensor).behavior())
    sensor ! UpdateStatus("Status of sensor " + id)
    sensor

  private def prepareDevicesList(lenght: Int): List[ActorRef[DeviceMessage]] =
    var sensors: List[ActorRef[DeviceMessage]] = List.empty
    for i <- 1 to lenght yield sensors = sensors ++ List(preparePublicSensor(i.toString))
    sensors

  private object Determinizer:
    def apply(destination: ActorRef[DeviceMessage]): Behavior[Message] =
      Behaviors.setup { ctx =>
        Behaviors.receivePartial { (ctx, message) =>
          message match
            case Statuses[String](author, value) =>
              destination ! Status(author, value.sorted)
              Behaviors.same
        }
      }

  private var sensors: List[ActorRef[DeviceMessage]] = prepareDevicesList(3)
  private var testProbe = testKit.createTestProbe[Message]()
  private var determinizer = testKit.spawn(Determinizer(testProbe.ref))

  private def resetVariables(): Unit =
    sensors = prepareDevicesList(3)
    testProbe = testKit.createTestProbe[Message]()
    determinizer = testKit.spawn(Determinizer(testProbe.ref))

  private def testBasicFunctionality(): Unit =
    resetVariables()
    val toUppercaseActor = testKit.spawn(BlockingGroup(new MapGroup[String, String]("id", sensors, List(determinizer), f => f.toUpperCase )))
    Thread.sleep(500)
    sensors.foreach(s => s ! PropagateStatus(testProbe.ref))
    testProbe.expectMessage(Status(toUppercaseActor.ref, List("STATUS OF SENSOR 1", "STATUS OF SENSOR 2", "STATUS OF SENSOR 3")))

  private def testNestingGroups(): Unit =
    resetVariables()
    case class LabeledValue(val label: String, val value: String)
    var sensors: List[ActorRef[DeviceMessage]] = prepareDevicesList(3)

    val labelAGroup = testKit.spawn(BlockingGroup(
      new MapGroup[String, LabeledValue]("id", List(sensors(0), sensors(2)), List.empty, f => LabeledValue("label-a", f.toUpperCase))
    ))

    val labelBGroup = testKit.spawn(BlockingGroup(
      new MapGroup[String, LabeledValue]("id", List(sensors(1)), List.empty, f => LabeledValue("label-b", f))
    ))

    Thread.sleep(500)
    val toUppercaseActor = testKit.spawn(BlockingGroup(
      new MapGroup[LabeledValue, String]("id", List(labelAGroup, labelBGroup),  List(determinizer), f => f.label + " " + f.value.toUpperCase)
    ))

    Thread.sleep(500)
    sensors.foreach(s => s ! PropagateStatus(testProbe.ref))
    Thread.sleep(2000)
    //testProbe.expectMessage(Status(labelAGroup, List(LabeledValue("label-a","STATUS OF SENSOR 3"), LabeledValue("label-a","STATUS OF SENSOR 1"))))
    testProbe.expectMessage(Status(toUppercaseActor.ref, List("label-a STATUS OF SENSOR 1", "label-a STATUS OF SENSOR 3", "label-b STATUS OF SENSOR 2")))

  private def testBlockingBehavior(): Unit =
    resetVariables()
    val toUppercaseActor = testKit.spawn(BlockingGroup(new MapGroup[String, String]("id", sensors, List(determinizer), f => f.toUpperCase )))
    Thread.sleep(500)
    for i <- 0 to 1 yield sensors(i) ! PropagateStatus(testProbe.ref)
    testProbe.expectNoMessage(FiniteDuration(2, "second"))

  private def testUpdatingValues(): Unit =
    resetVariables()
    val toUppercaseActor = testKit.spawn(BlockingGroup(new MapGroup[String, String]("id", sensors, List(determinizer), f => f.toUpperCase )))
    Thread.sleep(500)
    for i <- 0 to 1 yield sensors(i) ! PropagateStatus(testProbe.ref) //first sensor stored status = "status of sensor 1"
    Thread.sleep(100)
    sensors(0) ! UpdateStatus("a new value")
    Thread.sleep(100)
    sensors(0) ! PropagateStatus(testProbe.ref) //first sensor stored status = "a new value"
    Thread.sleep(100)
    sensors(2) ! PropagateStatus(testProbe.ref)
    testProbe.expectMessage(Status(toUppercaseActor, List("A NEW VALUE", "STATUS OF SENSOR 2", "STATUS OF SENSOR 3")))

  private def testNonBlockingBehavior(): Unit =
    resetVariables()
    val toUppercaseActor = testKit.spawn(NonBlockingGroup(new MapGroup[String, String]("id", sensors, List(determinizer), f => f.toUpperCase )))
    Thread.sleep(500)
    sensors(0) ! PropagateStatus(testProbe.ref)
    testProbe.expectMessage(Status(toUppercaseActor, List("STATUS OF SENSOR 1")))
    Thread.sleep(500)
    sensors(1) ! PropagateStatus(testProbe.ref)
    testProbe.expectMessage(Status(toUppercaseActor, List("STATUS OF SENSOR 1", "STATUS OF SENSOR 2")))

  private def testReduce(): Unit =
    resetVariables()
    val toUppercaseActor = testKit.spawn(BlockingGroup(new ReduceGroup[String, String]("id", sensors, List(testProbe.ref), _ + " | " + _, "")))
    for s <- sensors yield s ! UpdateStatus("status of sensor")
    Thread.sleep(500)
    for s <- sensors yield s ! PropagateStatus(testProbe.ref); Thread.sleep(500)
    testProbe.expectMessage(Status(toUppercaseActor, " | status of sensor | status of sensor | status of sensor"))