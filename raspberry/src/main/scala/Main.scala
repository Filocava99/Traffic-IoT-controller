import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.device.DeviceProtocol._
import it.pps.ddos.utils.GivenDataType.IntDataType

/**
 * Remote instance main program
 */
class Main extends App {
  def main(): Unit =
    Deployer.addNodes(1)
    val idGruppo = Deployer.getActorRefViaReceptionist("id3")
    val f: Int => Int = _+2
    val sensor = new StoreDataSensor[Int]("raspberry1", List(idGruppo), f)
    Deployer.deploy(sensor)
    Thread.sleep(3000)
    val idRasp = Deployer.getActorRefViaReceptionist("raspberry1")
    idGruppo ! AddSource(idRasp)
    while(true){}
}
