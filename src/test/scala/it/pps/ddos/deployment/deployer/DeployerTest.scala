package it.pps.ddos.deployment.deployer

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.Deployer.ActorSysWithActor
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.Device
import it.pps.ddos.device.DeviceProtocol.Message
import it.pps.ddos.device.actuator.{Actuator, BasicState, FSM}
import it.pps.ddos.device.sensor.BasicSensor
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.utils.GivenDataType.given
import org.scalatest.matchers.must.Matchers.*

import java.lang.reflect.Field
import scala.collection.immutable.ListMap
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class DeployerTest extends AnyFlatSpec:
  //STANDARD DEPLOYER START SHARED
  Deployer.initSeedNodes()
  Deployer.addNodes(5)

  private def createTestBasicStateSensor(): Graph[Device[Double]] =
    val testKit: ActorTestKit = ActorTestKit()
    val testProbe = testKit.createTestProbe[Message]()
    val basic1 = new BasicSensor[Double]("0", List(testProbe.ref))
    val basic2 = new BasicSensor[Double]("1", List(testProbe.ref))
    val basic3 = new BasicSensor[Double]("2", List(testProbe.ref))
    val basic4 = new BasicSensor[Double]("3", List(testProbe.ref))
    val graph = Graph[Device[Double]](
      basic1 -> basic2,
      basic1 -> basic3,
      basic2 -> basic4,
      basic3 -> basic2,
    )
    graph.size must be(3)
    graph

  private def createTestBasicStateFSM(): FSM[String] =
    val stateA = BasicState[String]("A")
    val stateB = BasicState[String]("B")
    val stateC = BasicState[String]("C")
    var fsm = FSM[String](Option.empty, Option.empty, ListMap.empty)
    fsm = stateA -- "toB" -> stateB _U stateA -- "toC" -> stateC _U stateB -- "toC" -> stateC _U stateC -- "toA" -> stateA
    fsm

  private def reflectionMethod(filter: String): Field =
    val fields: Seq[Field] = Deployer.getClass.getDeclaredFields.toList
    fields.foreach(_.setAccessible(true))
    fields.filter(_.getName.contains(filter)).head


  "A graph of sensors should" should "be deploy without errors" in {
    Thread.sleep(2000)
    val graph = createTestBasicStateSensor()
    println(graph)
    Deployer.deploy(graph)
  }

  "A graph of actuator should" should "be deployed without errors" in {
    Thread.sleep(2000)
    val fsm = createTestBasicStateFSM()
    val a1 = Actuator[String]("a1", fsm)
    val a2 = Actuator[String]("a2", fsm)
    val a3 = Actuator[String]("a3", fsm)
    val a4 = Actuator[String]("a4", fsm)
    val graph = Graph[Device[String]](
      a1 -> a2,
      a1 -> a3,
      a2 -> a4,
    )
    println(graph)
    Deployer.deploy(graph)
  }

  "A graph should" should "have the actorSysWithActorList after the deploy set correctly " in {
    Thread.sleep(8000)
    val field = reflectionMethod("orderedActorSystemRefList")
    val value = field.get(Deployer)
    value match {
      case list: ListBuffer[_] =>
        println("checking actorSysWithActorList")
        println(list)
        list.size must be (5)
      case _ =>
    }
  }

  "A graph should" should "have the devicesActorRefMap after the deploy set correctly " in {
    Thread.sleep(2000)
    val field = reflectionMethod("devicesActorRefMap")
    val value = field.get(Deployer)
    value match {
      case map: Map[_, _] =>
        println("checking devicesActorRefMap")
        println(map)
        map.size must be (8)
      case _ =>
    }
  }


