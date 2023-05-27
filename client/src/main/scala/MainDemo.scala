import it.pps.ddos.deployment.Deployer

import java.net.InetAddress

object MainDemo extends App:
    val serverAddress: String = InetAddress.getLocalHost.getHostAddress
    Deployer.createActorSystem(serverAddress)
    ClientView()