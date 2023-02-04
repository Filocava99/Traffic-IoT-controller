package it.pps.ddos.gui.view

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.cluster.typed.{Cluster, ClusterSingleton, SingletonActor}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.DeviceProtocol.Message
import it.pps.ddos.device.actuator.BasicState
import it.pps.ddos.device.sensor.BasicSensor
import it.pps.ddos.device.{Device, Public, Timer}
import it.pps.ddos.gui.controller.DDosController
import it.pps.ddos.storage.tusow.Server
import javafx.application.Application
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Button, ListView}
import scalafx.scene.layout.{BorderPane, HBox, VBox}

import java.io.File
import java.util.concurrent.TimeUnit
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContextExecutor}

object DDosGUI extends JFXApp3:

  private val guiController = new DDosController()


  override def start(): Unit =
    guiController.start()
    createGUI(guiController)

  private def createGUI(guiController: DDosController): Unit =
    var actorList: ListView[String] = new ListView[String]()
    var actorGroups: ListView[String] = new ListView[String]()
    var history: ListView[String] = new ListView[String]()
    val updateButton: Button = new Button("Update")
    val buttonBox: HBox = new HBox(updateButton)
    val mainBox: VBox = new VBox {
      spacing = 10
      padding = Insets(10)
      children = Seq(actorList, actorGroups, history, buttonBox)
    }

    val root: BorderPane = new BorderPane
    root.setCenter(mainBox)
    root.setPadding(Insets(25))

    val sceneMain: Scene = new Scene(root, 1000, 800)

    updateButton.onAction = _ =>
      history.items = ObservableBuffer.from(guiController.getMsgHistory)
      actorList.items = ObservableBuffer.from(guiController.getListOfRef.map("Device : "+_.path))
      actorGroups.items = ObservableBuffer.from(guiController.getListOfGroups)

    stage = new PrimaryStage:
      title = "DDos Demo"
      scene = sceneMain


