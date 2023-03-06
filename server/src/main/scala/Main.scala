import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.deployment.Deployer.InternSpawn
import it.sc.server.ServerActor
import it.sc.server.IdRequest

object Main{
  def main(args: Array[String]): Unit =
    /*Deployer.initSeedNodes()
    Deployer.addNodes(1)
    Deployer.deploy(new MapGroup[Int,String]("id3", Set.empty, List.empty, i=> i.toString ))*/
    val as = Deployer.createActorSystem("as")
    as ! InternSpawn("serverTest", ServerActor())
    Thread.sleep(3000)
    val ref = Deployer.getActorRefViaReceptionist("serverTest")
    println(ref)
    ref ! IdRequest("via manzoni 5", ref)
    println("ciao")


}
