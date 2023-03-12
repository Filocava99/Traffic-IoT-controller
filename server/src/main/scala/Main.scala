import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.deployment.Deployer.InternSpawn
import it.pps.ddos.device.DeviceProtocol.Statuses
import it.sc.server.{IdRequest, ServerActor, StoringActor}
import reactivemongo.api.bson.BSONObjectID
import it.sc.server.entities.RecordedData
import com.github.nscala_time.time.Imports.DateTime

object Main{
  def main(args: Array[String]): Unit =
    /*Deployer.initSeedNodes()
    Deployer.addNodes(1)
    Deployer.deploy(new MapGroup[Int,String]("id3", Set.empty, List.empty, i=> i.toString ))*/
    val as = Deployer.createActorSystem("as")
    as ! InternSpawn("serverTest", ServerActor())
    Thread.sleep(3000)
    val ref = Deployer.getActorRefViaReceptionist("serverTest")
    ref ! IdRequest("via manzoni 10", ref)
    as ! InternSpawn("storingActor", StoringActor())
    Thread.sleep(3000)
    val storingRef = Deployer.getActorRefViaReceptionist("storingActor")
    val fakeCameraId = BSONObjectID.generate()
    storingRef ! Statuses[RecordedData](ref, List(RecordedData(fakeCameraId, DateTime.now(), Map(1->2, 0->99))))

}