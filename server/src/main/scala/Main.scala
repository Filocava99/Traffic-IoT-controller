import it.pps.ddos.deployment.Deployer
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.grouping.*
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import it.pps.ddos.deployment.Deployer.InternSpawn
import it.pps.ddos.device.DeviceProtocol.Statuses
import it.sc.server.{IdRequest, ServerActor, StoringActor}
import it.sc.server.entities.RecordedData
import com.github.nscala_time.time.Imports.DateTime
import it.sc.server.mongodb.MongoDBClient
import org.bson.types.ObjectId

import java.net.InetAddress

object Main{
  def main(args: Array[String]): Unit =
    val serverAddress: String = InetAddress.getLocalHost.getHostAddress
    println(serverAddress)
    Deployer.initSeedNodes(serverAddress)
    val as = Deployer.createActorSystem(serverAddress)
    as ! InternSpawn("server", ServerActor())
    Thread.sleep(3000)
//    val ref = Deployer.getActorRefViaReceptionist("server")
//    ref ! IdRequest("via manzoni 999", ref)
    as ! InternSpawn("storing", StoringActor())
    Thread.sleep(3000)
    val storingRef = Deployer.getActorRefViaReceptionist("storing")
    val fakeCameraId = new ObjectId()

//    storingRef ! Statuses[RecordedData](ref, List(RecordedData(fakeCameraId.stringify, 1000, Map(1->2, 0->102))))

}