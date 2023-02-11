import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping._

class Main extends App{
  def main(): Unit =
    Deployer.initSeedNodes()
    Deployer.addNodes(1)
    Deployer.deploy(new MapGroup[Int,String]("id3", Set.empty, List.empty, i=> i.toString ))
    while(true){}

}
