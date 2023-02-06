package it.pps.ddos.storage.tusow

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.{Device, Public, Timer}
import it.pps.ddos.utils.GivenDataType.given
import it.pps.ddos.device.DeviceProtocol.DeviceMessage
import it.pps.ddos.device.sensor.BasicSensor
import it.unibo.coordination.tusow.grpc.{IOResponse, TupleSpaceID, TupleSpaceType}
import org.scalatest.flatspec.AnyFlatSpec
import it.pps.ddos.storage.tusow.TusowBinder

import scala.concurrent.duration.Duration

class TusowBinderTest extends AnyFlatSpec:
  private val sys: ActorSystem[DeviceMessage] = ActorSystem(Behaviors.empty, "ClusterSystem", ConfigFactory.load("application.conf"))
  setupDeployer()

  private def setupDeployer(): Unit =
    Deployer.initSeedNodes()
    Deployer.addNodes(2)
    Deployer.deploy(createSensorGraph())
    Thread.sleep(10000)

  private def createSensorGraph(): Graph[Device[Double]] =
    val basic1 = new BasicSensor[Double]("0", List.empty) with Public[Double] with Timer(Duration(1, "second"))
    val basic2 = new BasicSensor[Double]("1", List.empty) with Public[Double] with Timer(Duration(1, "second"))
    val basic3 = new BasicSensor[Double]("2", List.empty) with Public[Double] with Timer(Duration(1, "second"))
    val basic4 = new BasicSensor[Double]("3", List.empty) with Public[Double] with Timer(Duration(1, "second"))
    val graph = Graph[Device[Double]](
      basic1 -> basic2,
      basic1 -> basic3,
      basic2 -> basic4,
      basic3 -> basic2
    )
    graph

  "A TusowBinder should" should "start correctly, write to the server and get correct IOResponse back" in {
    Server.start()
    TusowBinder(sys)
    Thread.sleep(12000)
    TusowBinder.getLatestResponse match
      case Some(IOResponse(response,message,unknownFields)) =>
          println("response: " + response + " message: " + message + " unknownFields: " + unknownFields)
          succeed
      case None => fail("No response from server")
  }

//def createSensor(): Graph[Device[String]]
