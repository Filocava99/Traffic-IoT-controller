//import it.pps.ddos.deployment.Deployer
//import it.pps.ddos.deployment.Deployer.InternSpawn
//import it.pps.ddos.device.sensor.BasicSensor
//import it.pps.ddos.utils.GivenDataType.given
//import it.pps.ddos.deployment.graph.Graph
//import it.pps.ddos.device.Device
//
//object MainDemo extends App:
//  Deployer.initSeedNodes()
//  val as = Deployer.createActorSystem("ClusterSystem")
//  as ! InternSpawn("clientActor", ClientActor().behavior())
//  val sensorA = new BasicSensor[Double]("Camera1", List.empty)
//  val sensorB = new BasicSensor[Double]("Camera2", List.empty)
//  val sensorC = new BasicSensor[Double]("Camera3", List.empty)
//  val graph = Graph[Device[Double]](
//    sensorA -> sensorA,
//    sensorB -> sensorB,
//    sensorC -> sensorC
//  )
//  Deployer.deploy(graph)
//  ClientView()
//
