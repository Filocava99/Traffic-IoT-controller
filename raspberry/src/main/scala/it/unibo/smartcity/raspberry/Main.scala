package it.unibo.smartcity.raspberry

import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.Deployer.InternSpawn
import it.pps.ddos.device.DeviceProtocol.AddSource
import it.pps.ddos.device.sensor.StoreDataSensor
import it.pps.ddos.utils.GivenDataType.IntDataType
import it.unibo.smartcity.raspberry.RaspberryActor

import java.net.{Inet4Address, InetAddress, NetworkInterface}

/**
 * Remote instance main program
 */
object Main{
    def main(args: Array[String]): Unit =
        val iterator = NetworkInterface.getNetworkInterfaces().asIterator()
        var raspberryAddress = ""
        while(iterator.hasNext){
            val networkInterface = iterator.next()
            if(networkInterface.getName == "wlan0"){
                raspberryAddress = networkInterface.inetAddresses().filter(_.isInstanceOf[Inet4Address]).findFirst().get().getHostAddress
            }
        }
//        raspberryAddress = InetAddress.getLocalHost.getHostAddress
        println("Raspberry address: " + raspberryAddress)
        val as = Deployer.createActorSystem(raspberryAddress)
        Thread.sleep(3000)
        as ! InternSpawn("r1", RaspberryActor(args(0)))
}
