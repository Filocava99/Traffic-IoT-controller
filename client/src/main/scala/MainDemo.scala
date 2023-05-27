import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.Deployer.InternSpawn
import it.pps.ddos.device.sensor.BasicSensor
import it.pps.ddos.utils.GivenDataType.given
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.Device

import java.net.InetAddress

object MainDemo extends App:
  val clientAddress = InetAddress.getLocalHost.getHostAddress
  Deployer.initSeedNodes(clientAddress)
  val as = Deployer.createActorSystem(clientAddress)
  val sensorA = new BasicSensor[Double]("Camera1", List.empty)
  val sensorB = new BasicSensor[Double]("Camera2", List.empty)
  val sensorC = new BasicSensor[Double]("Camera3", List.empty)
  as ! InternSpawn("clientActor", ClientActor().behavior())
  as ! InternSpawn("Camera1", sensorA.behavior())
  as ! InternSpawn("Camera2", sensorB.behavior())
  as ! InternSpawn("Camera3", sensorC.behavior())
  ClientView()
