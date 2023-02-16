import it.pps.ddos.deployment.Deployer
import it.pps.ddos.grouping.MapGroup

import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafx.scene.control.Button

import javafx.fxml.FXMLLoader
import javafx.scene.layout.VBox
import javafx.scene.Node
import javafx.scene.media.MediaView

/**
 * Define the management of the GUI
 */
class ClientView extends JFXApp3:
  override def start(): Unit =
    stage = new PrimaryStage {
      title.value = "Traffic IoT Controller"
      scene = new Scene {
        content = FXMLLoader(getClass.getResource("/view/TrafficIoTController.fxml")).load[VBox]
      }
    }
    stage.show()
    initCameras(stage.scene.get().lookup("#vboxView").asInstanceOf[VBox])

  private def initCameras(node: VBox) =
    // TODO: fare la ricerca degli attori tramite receptionist (vedi metodo nuovo in Deployer)
    for (k, v) <- Deployer.getDevicesActorRefMap yield {
      val button = new Button(k)
      button.setText(v.toString)
      button.onMouseClicked = () => mediaAndDataHandler()
      node.getChildren.add(button)
    }

  private def mediaAndDataHandler() =
    val media = stage.scene.get().lookup("#mediaView").asInstanceOf[MediaView]
    // TODO: add camera transmission
    println("#debug | media player object: " + media)
    // TODO: connection to the database

/**
 * Define the static view initialization
 */
object ClientView:
  private def startView(args: Array[String]) = new ClientView().main(args)

  def apply(): Unit =
    Deployer.initSeedNodes()
    Deployer.addNodes(1)
    Deployer.deploy(new MapGroup[Int, String]("client", Set.empty, List.empty, i => i.toString))
    startView(Array("ClientViewApp"))

/**
 * Start the GUI and the relative actor
 */
object ClientMain extends App:
  ClientView()


///**
// * Start only the GUI
// */
//object StarterGUI extends App:
//  new ClientView().main(Array("ClientViewApp"))