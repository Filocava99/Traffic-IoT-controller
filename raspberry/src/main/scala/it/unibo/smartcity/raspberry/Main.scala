package it.unibo.smartcity.raspberry

import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.Deployer.InternSpawn
import it.pps.ddos.device.DeviceProtocol.AddSource
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.utils.GivenDataType.IntDataType
import it.unibo.smartcity.raspberry.RaspberryActor
import java.net.InetAddress

/**
 * Remote instance main program
 */
object Main{
    def main(args: Array[String]): Unit =
        val rasberryAddress: InetAddress = InetAddress.getLocalHost.getAddress
        val as = Deployer.createActorSystem(rasberryAddress)
        Thread.sleep(3000)
        as ! InternSpawn("r1", RaspberryActor(args[0]))
}
