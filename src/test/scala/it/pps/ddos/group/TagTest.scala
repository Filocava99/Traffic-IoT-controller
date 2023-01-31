package it.pps.ddos.group

import akka.actor.testkit.typed.Effect.Spawned
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.sensor.{BasicSensor, ProcessedDataSensor, Sensor, SensorActor}
import it.pps.ddos.device.Public
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.grouping.{tagging, *}
import org.scalactic.Prettifier.default
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.Device
import it.pps.ddos.utils.GivenDataType.given

import scala.collection.immutable.List
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Failure
import scala.util.Try
import it.pps.ddos.device
import it.pps.ddos.grouping.tagging.{MapTag, Tag, Taggable, TriggerMode}

class TagTest extends AnyFlatSpec:
  "A Tag" should "register itself in any object that extends the Taggable trait" in testBasicTag()
  it should "be taggable itself, but cannot be used to create a circular tagging" in testNestedTag()
  it should "be syntactic sugar to generate a group in a simpler way" in testGroupEquality()
  it should "be applied to every taggable object passed in the <-- operator" in testInverseMarking()
  it should "produce functional groups when deployed via graph" in testTagDeployment()

  val testKit: ActorTestKit = ActorTestKit()
  class PublicSensor(id: String) extends BasicSensor[String](id, List.empty) with Public[String]

  private def preparePublicSensor(id: String): ActorRef[DeviceMessage] =
    val sensor = testKit.spawn(SensorActor(new PublicSensor(id)).behavior())
    sensor ! UpdateStatus("Status of sensor " + id)
    sensor

  private def prepareDevicesList(lenght: Int): List[ActorRef[DeviceMessage]] =
    var sensors: List[ActorRef[DeviceMessage]] = List.empty
    for i <- 1 to lenght yield sensors = sensors ++ List(preparePublicSensor(i.toString))
    sensors

  private var sensors: List[ActorRef[DeviceMessage]] = prepareDevicesList(3)
  private var testProbe = testKit.createTestProbe[DeviceMessage]()
  private var determinizer = testKit.spawn(Determinizer(testProbe.ref))

  private def resetVariables(): Unit =
    sensors = prepareDevicesList(3)
    testProbe = testKit.createTestProbe[DeviceMessage]()
    determinizer = testKit.spawn(Determinizer(testProbe.ref))

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

  private def testBasicTag(): Unit =
    val testTag = Tag[String, String]("test", List.empty, s => s, TriggerMode.BLOCKING)
    case class Probe() extends Taggable
    val p = Probe()
    p ## testTag
    assert(p.getTags() == List(testTag))

  private def testNestedTag(): Unit =
    val tag1 =  tagging.Tag[String, String]("1", List.empty, s => s, TriggerMode.BLOCKING)
    val tag2 =  tagging.Tag[String, String]("2", List.empty, s => s, TriggerMode.BLOCKING)
    val tag3 =  tagging.Tag[String, String]("3", List.empty, s => s, TriggerMode.BLOCKING)
    tag1 ## tag2
    tag2 ## tag3
    tag3 ## tag1 //should not work
    assert(tag1.getTags() == List(tag2))
    val thrown = intercept[IllegalArgumentException] {
      val ex = tag3 ## tag1
      if ex.isFailure then throw ex.failed.get
    }

  private def testGroupEquality(): Unit =
    val f: String => String = s => s
    val tag1: MapTag[String, String] =  tagging.Tag[String, String]("1", List.empty, f, TriggerMode.BLOCKING)
    val generatedGroup: MapGroup[String, String] = tag1.generateGroup(sensors)
    val normalGroup: MapGroup[String, String] = new MapGroup[String, String]("1", sensors, List.empty, f)
    assert(generatedGroup.equals(normalGroup))

  private def testInverseMarking(): Unit =
    resetVariables()
    val tag1 = tagging.Tag[String, String]("1", List.empty, s => s, TriggerMode.BLOCKING)
    val sensorA = new PublicSensor("A")
    val sensorB = new PublicSensor("B")
    val tagC = tagging.Tag[Int, String]("C", List.empty, i => i.toString, TriggerMode.NONBLOCKING)
    tag1 <--(sensorA, sensorB, tagC)
    assert(sensorA.getTags() == List(tag1) && sensorB.getTags() == List(tag1) && tagC.getTags() == List(tag1))

  private def testTagDeployment(): Unit =
    resetVariables()
    val f: String => String = s => s.toUpperCase
    val toUppercase = tagging.Tag[String, String]("tag1", List(determinizer), f , TriggerMode.BLOCKING)
    val addNumber = tagging.Tag[String, String]("tag2", List.empty, "1 " + _, TriggerMode.BLOCKING)
    val addNumber2 = tagging.Tag[String, String]("tag3", List.empty,"2 " + _, TriggerMode.BLOCKING)
    val addNumber3 = tagging.Tag[String, String]("tag4", List.empty,"3 " + _, TriggerMode.BLOCKING)
    val addNumber4 = tagging.Tag[String, String]("tag5", List.empty,"4 " + _, TriggerMode.BLOCKING)
    val sensorA = new PublicSensor("a")
    val sensorB = new PublicSensor("b")

    addNumber ## addNumber2
    addNumber2 ## addNumber3
    addNumber3 ## addNumber4
    addNumber4 ## toUppercase
    sensorA ## addNumber
    sensorB ## toUppercase

    Deployer.initSeedNodes()
    Deployer.addNodes(1)
    Thread.sleep(500)
    val graph = Graph[Device[String]](
      sensorA -> sensorA,
      sensorB -> sensorB,
    )
    Deployer.deploy(graph)
    Thread.sleep(10000)
    val actorMap = Deployer.getDevicesActorRefMap
    actorMap("a") ! UpdateStatus("a")
    actorMap("b") ! UpdateStatus("b")
    Thread.sleep(10000)
    actorMap("a") ! PropagateStatus(testProbe.ref)
    actorMap("b") ! PropagateStatus(testProbe.ref)

    Thread.sleep(500)
    testProbe.expectMessage(FiniteDuration(10, "second"), Status(actorMap("tag1"), List("4 3 2 1 A", "B")))