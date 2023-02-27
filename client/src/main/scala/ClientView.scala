import akka.actor.typed.ActorRef
import it.pps.ddos.deployment.Deployer
import it.pps.ddos.deployment.graph.Graph
import it.pps.ddos.device.Device
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, Unsubscribe}
import it.pps.ddos.device.sensor.BasicSensor
import it.pps.ddos.grouping.MapGroup
import it.pps.ddos.{DBEntry, MongoDBFind}
import scalafx.Includes.*
import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.stage.Stage
import scalafx.scene.control.Button
import scalafx.collections.ObservableBuffer
import javafx.fxml.FXMLLoader
import javafx.scene.layout.{Pane, VBox}
import javafx.scene.Node
import javafx.scene.media.MediaView
import javafx.scene.control.ListView

/**
 * Define the management of the GUI
 */
class ClientView extends JFXApp3:
  private var actualActorRef: ActorRef[DeviceMessage] = _

  override def start(): Unit =
    stage = new PrimaryStage {
      title.value = "Traffic IoT Controller"
      scene = new Scene {
        content = FXMLLoader(getClass.getResource("/view/TrafficIoTController.fxml")).load[VBox]
      }
    }
    stage.show()
    initVideocam(stage.scene.get().lookup("#vboxView").asInstanceOf[VBox])

  private def initVideocam(node: VBox) =
    // TODO: fare la ricerca degli attori tramite receptionist (vedi metodo nuovo in Deployer)
    for (k, v) <- Deployer.getDevicesActorRefMap yield {
      val button = new Button(k)
      button.setText(v.toString)
      button.onMouseClicked = () => mediaAndDataHandler(k.toInt, v)
      node.getChildren.add(button)
    }

  private def mediaAndDataHandler(id: Int, ref: ActorRef[DeviceMessage]) =
    if actualActorRef == null || actualActorRef != ref then
      ClientView.clientActor.actualActorRef ! Unsubscribe(actualActorRef)
      actualActorRef = ref
    val media = stage.scene.get().lookup("#mediaView").asInstanceOf[MediaView]
    // TODO: add camera transmission
    println("#debug | media player object: " + media)
    // connection to the database and getting data
    MongoDBFind(id)
    Thread.sleep(2000) // wait for the query to complete
    displayInfo()

  private def displayInfo() =
    val info = stage.scene.get().lookup("#listView").asInstanceOf[ListView[DBEntry]]
    info.items = ObservableBuffer.from(MongoDBFind.data)

/**
 * Define the static view initialization
 */
object ClientView:
  private val clientActor: ClientActor = ClientActor()
  private def startView(args: Array[String]) = new ClientView().main(args)

  def apply(): Unit =
    Deployer.initSeedNodes()
    Deployer.addNodes(2)
    Deployer.deploy(new MapGroup[Int, String]("99", Set.empty, List.empty, i => i.toString))
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