package it.pps.ddos

import it.pps.ddos.deployment.Deployer

import it.pps.ddos.deployment.Deployer.{deviceServiceKey, setupClusterConfig}
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.DeviceProtocol.*
import it.pps.ddos.device.sensor.{BasicSensor, Condition, ProcessedDataSensor, SensorActor}
import it.pps.ddos.device.{Device, Public, Timer}
import it.pps.ddos.grouping.tagging
import it.pps.ddos.grouping.tagging.TriggerMode
import it.pps.ddos.gui.view.DDosGUI
import it.pps.ddos.storage.tusow.{Server, TusowBinder}
import it.pps.ddos.utils.DataType
import it.pps.ddos.utils.GivenDataType.{IntDataType, *, given}
import javafx.application.Application
import javafx.embed.swing.JFXPanel
import org.agrona.concurrent.status.StatusIndicatorReader
import scalafx.application.JFXApp3

import java.io.File
import scala.collection.immutable.HashMap
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.util.control.NonLocalReturns

class Main extends App {
  def main(): Unit =
    Deployer.initSeedNodes()

}
