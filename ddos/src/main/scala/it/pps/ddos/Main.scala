package it.pps.ddos

import it.pps.ddos.deployment.Deployer

class Main extends App {
  def main(): Unit =
    Deployer.initSeedNodes()

}
