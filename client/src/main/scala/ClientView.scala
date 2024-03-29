import akka.actor.typed.ActorRef
import com.github.nscala_time.time.Imports.{DateTime, DateTimeFormat, Duration}
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.Device
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Subscribe, Unsubscribe}
import it.pps.ddos.device.sensor.BasicSensor
import it.pps.ddos.grouping.MapGroup
import it.sc.server.entities.RecordedData
import it.pps.ddos.deployment.Deployer.InternSpawn
import it.pps.ddos.utils.GivenDataType.DoubleDataType
import util.{MongoDBFindCameras, MongoDBFindStoricData}
import javafx.animation.KeyValue
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafx.scene.control.Button
import scalafx.collections.ObservableBuffer
import javafx.scene.control.ProgressBar
import javafx.util.Duration
import javafx.animation.{KeyFrame, Timeline}
import javafx.fxml.FXMLLoader
import javafx.scene.layout.{Pane, VBox}
import javafx.scene.Node
import javafx.scene.media.MediaView
import javafx.scene.control.ListView
import javafx.concurrent.Task
import javafx.scene.web.{WebEngine, WebView}

import java.util.concurrent.TimeUnit
import java.util.{Timer, TimerTask}
import scala.collection.mutable
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * Define the management of the GUI
 */
class ClientView extends JFXApp3:
  private var actualID: String = _
  private var timer = new Timer()
  private var currentCameraViewId: String = ""
  private val insideCameraView = mutable.Map[String, Boolean]()

  override def start(): Unit =
    MongoDBFindCameras()
    stage = new PrimaryStage {
      title.value = "Traffic IoT Controller"
      scene = new Scene {
        content = FXMLLoader(getClass.getResource("/gui/TrafficIoTController.fxml")).load[VBox]
      }
    }
    stage.resizable = false
    stage.show()
    initVideocamera(stage.scene.get().lookup("#vboxView").asInstanceOf[VBox])

  private def initVideocamera(node: VBox) =
    for v <- MongoDBFindCameras.cameras yield {
      val button = new Button(v.id.toHexString)
      button.setText(v.details)
      button.prefWidth = 352
      button.prefHeight = 26
      button.onMouseClicked = () => mediaAndDataHandler(v.id.toHexString, Deployer.getActorRefViaReceptionist("broadcaster-" + v.id.toHexString))
      node.getChildren.add(button)
    }

  private def checkID(id: String, ref: ActorRef[DeviceMessage]): Unit =
    if Option(actualID).isEmpty then actualID = id
    else if actualID != id then
      ref ! Unsubscribe(Deployer.getActorRefViaReceptionist(actualID))
      actualID = id
      ref ! Subscribe(Deployer.getActorRefViaReceptionist(actualID))

  private def mediaAndDataHandler(id: String, ref: ActorRef[DeviceMessage]): Unit =
    if insideCameraView.contains(currentCameraViewId) then insideCameraView(currentCameraViewId) = false
    currentCameraViewId = id
    insideCameraView(id) = true
    checkID(id, ref)
    val media = stage.scene.get().lookup("#webView").asInstanceOf[WebView]
    media.getEngine.load("http://192.168.1.18:5000")
    reloadWebView(id, media.getEngine)
    // connection to the database and get data to display
    MongoDBFindStoricData(id)
    displayInfo

  private def reloadWebView(id: String, engine: WebEngine): Unit =
    new Thread(){
        override def run(): Unit =
            while (insideCameraView(id)) do
              engine.reload()
            println("reload")
            Thread.sleep(1000)
    }.start()

  private def displayInfo =
    val info = stage.scene.get().lookup("#listView").asInstanceOf[ListView[String]]
    var items: ObservableBuffer[RecordedData] = ObservableBuffer.empty

    // create the progress bar
    val progressBar = new ProgressBar {
      setLayoutX(360.0)
      setLayoutY(995.0)
      setPrefWidth(1550.0)
      setPrefHeight(22.0)
    }

    val task = new Task[Unit] {
      override def call(): Unit =
        val totalTime = javafx.util.Duration.seconds(2.5) // total time to display the progress bar
        val startTime = System.currentTimeMillis() // start time of the progress bar
        val endTime = startTime + totalTime.toMillis // end time of the progress bar

        while (System.currentTimeMillis() < endTime) {
          val progress = (System.currentTimeMillis() - startTime).toDouble / totalTime.toMillis // calculate progress percentage
          updateProgress(progress, 1) // update the progress bar
          Thread.sleep(50) // delay to slow down the progress bar
        }
        progressBar.setVisible(false)
    }

    task.setOnScheduled(_ => items = ObservableBuffer.from(MongoDBFindStoricData.storicData)) // take data from "storicData" database collection
    task.setOnSucceeded(_ => info.items = formattedInfo(items)) // set the GUI list of details with the data retrieved

    progressBar.setVisible(true)
    progressBar.progress <== task.progressProperty() // bind the progress bar to the progress of the task

    stage.scene.get().content.add(progressBar) // add the progress bar to the GUI scene content

    /* Initialize and start the thread to display the progress bar */
    val progressBarThread: Thread = new Thread(task)
    progressBarThread.start()
    progressBarThread.join(50)

  private def formattedInfo(infos: ObservableBuffer[RecordedData]): ObservableBuffer[String] =
    infos.map(entry => new DateTime(entry.timeStamp).toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")) + "\t" + entry.data.map {
        case (key, value) => key match
          case 0 => "People: " + value
          case 1 => "Bycicles: " + value
          case 2 => "Cars: " + value
          case 3 => "Motorcycles: " + value
          case 5 => "Busses: " + value
          case 7 => "Trucks :" + value
          case _ => ""
      }.mkString(", "))

/**
 * Define the static view initialization
 */
object ClientView:
  def apply(): Unit = new ClientView().main(Array("ClientViewApp"))