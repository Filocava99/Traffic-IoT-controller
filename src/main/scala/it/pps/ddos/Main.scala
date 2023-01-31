package it.pps.ddos

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import com.typesafe.config.{Config, ConfigFactory}
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.Deployer.{deviceServiceKey, setupClusterConfig}
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.sensor.{BasicSensor, Condition, ProcessedDataSensor}
import it.pps.ddos.device.{Device, Public, Timer}
import it.pps.ddos.grouping.tagging
import it.pps.ddos.grouping.tagging.TriggerMode
import it.pps.ddos.gui.view.DDosGUI
import it.pps.ddos.storage.tusow.{Server, TusowBinder}
import it.pps.ddos.utils.DataType
import it.pps.ddos.utils.GivenDataType.{IntDataType, *, given}
import javafx.application.Application
import javafx.embed.swing.JFXPanel
import org.agrona.concurrent.status.StatusIndicatorReader
import scalafx.application.JFXApp3

import java.io.File
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonLocalReturns

object Main:
  private val deviceServiceKey = ServiceKey[DeviceMessage]("DeviceService")

  def main(args: Array[String]): Unit =
    setupDeployer()
    executeParallelTask(LaunchTusowService.main(args))
    executeParallelTask(updateDeviceStatus())
    DDosGUI.main(args)

  private def executeParallelTask[T](code: => T): Future[T] = Future(code)

  private def updateDeviceStatus(): Unit =
    val actorMap = Deployer.getDevicesActorRefMap
    for i <- 1 to 100 do
      actorMap.foreach((device, actorRef) => actorRef ! UpdateStatus((Math.random() * 100).toInt.toString))
      Thread.sleep(5000)
      println(Deployer.getDevicesActorRefMap)

  private def setupDeployer(): Unit =
    Deployer.initSeedNodes()
    Deployer.addNodes(5)
    Deployer.deploy(createSomeStringSensors())
    Thread.sleep(10000)
    Deployer.deploy(createSomeStringToIntSensors())
    Thread.sleep(10000)
    Deployer.deploy(createSomeStringToHashMapSensors())
    Thread.sleep(5000)
    println(Deployer.getDevicesActorRefMap)

  private def createSomeStringSensors(): Graph[Device[String]] =
    val basic1 = new MyBasicCustomSensor("0")
    val basic2 = new MyBasicCustomSensor("1")

    val concatString1 = tagging.Tag[String, String]("tag1", List.empty, _ + "ciao ", TriggerMode.BLOCKING)
    val concatString2 = tagging.Tag[String, String]("tag2", List.empty, _ + "dal ", TriggerMode.BLOCKING)
    val concatString3 = tagging.Tag[String, String]("tag3", List.empty, _ + "team DDOS ", TriggerMode.BLOCKING)

    concatString1 ## concatString2
    concatString2 ## concatString3

    basic1 ## concatString1

    basic2 ## concatString3

    val graph = Graph[Device[String]](
      basic1 -> basic1,
      basic2 -> basic2,
    )
    graph

  private def createSomeStringToIntSensors(): Graph[Device[Int]] =
    val basic1 = new MyProcessedDataCustomSensor("2")
    val basic2 = new MyProcessedDataCustomSensor("3")

    val addNumber1 = tagging.Tag[Int, String]("tag4", List.empty, _ + 1.toString, TriggerMode.BLOCKING)
    val multiplyBy2 = tagging.Tag[String, Int]("tag5", List.empty, _.toInt * 2, TriggerMode.BLOCKING)
    val divideBy3 = tagging.Tag[Int, Int]("tag6", List.empty, _ / 3, TriggerMode.BLOCKING)
    val convertToFahrenheit = tagging.Tag[Int, Int]("tag7", List.empty, _ * 9 / 5 + 32, TriggerMode.BLOCKING)

    addNumber1 ## multiplyBy2
    multiplyBy2 ## divideBy3
    basic1 ## addNumber1
    basic2 ## convertToFahrenheit

    addNumber1 <-- (basic1, basic2)
    convertToFahrenheit <-- basic2

    val graph = Graph[Device[Int]](
      basic1 -> basic1,
      basic2 -> basic2,
    )
    graph

  private def createSomeStringToHashMapSensors(): Graph[Device[mutable.HashMap[Any, Any]]] =
    var ref = Option.empty[ActorRef[DeviceMessage]]
    val sys = ActorSystem(Behaviors.setup[DeviceMessage] { context =>
      ref = Option(context.spawn[DeviceMessage](new BasicSensor[String]("dummyReply", List.empty) with Public[String] with Timer(Duration(5, "seconds")).behavior(), "dummyReply"))
      context.system.receptionist ! Receptionist.Register(deviceServiceKey, ref.get)
      Behaviors.empty
    }, "ClusterSystem", ConfigFactory.load("application.conf"))

    val basic1 = new UltraCustomSensor("4")
    val basic2 = new UltraCustomSensor("5") with Condition[String, mutable.HashMap[Any, Any]](x => {
      for (i <- x) yield i
    }
    match
      case i if i.values.toList.contains('8') =>
        ref.get ! UpdateStatus("condizione verificata da sensor 5")
        true
      case _ => false
      , ref.get)

    val hash = tagging.Tag[mutable.HashMap[Any, Any], List[String]]("tagHash", List.empty, y => {
      for (i <- y.iterator.toList) yield i.hashCode().toString
    }, TriggerMode.BLOCKING)

    val hashReducing = tagging.Tag[mutable.HashMap[Any, Any], List[String]]("tagHashReduce", List.empty, (x, y) => {
      for (i <- y.iterator.toList) yield i.hashCode().toString
    }, List("".hashCode.toString), TriggerMode.BLOCKING)

    val addNumber1 = tagging.Tag[List[String], List[Int]]("tag8", List.empty, x => x.map(_.toInt + 1), TriggerMode.BLOCKING)

    basic2 ## hash
    hash ## addNumber1
    hashReducing <-- basic1

    val graph = Graph[Device[mutable.HashMap[Any, Any]]](
      basic2 -> basic1
    )
    graph



private class MyBasicCustomSensor(id: String) extends BasicSensor[String](id, List.empty) with Public[String] with Timer(Duration(5, "seconds"))

private class MyProcessedDataCustomSensor(id: String) extends ProcessedDataSensor[String, Int](id, List.empty, x => x.toInt) with Public[Int] with Timer(Duration(5, "seconds"))

private class UltraCustomSensor(id: String) extends ProcessedDataSensor[String, mutable.HashMap[Any, Any]](id, List.empty, x => mutable.HashMap(x -> x.charAt(0))) with Public[mutable.HashMap[Any, Any]] with Timer(Duration(5, "seconds"))