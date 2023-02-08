import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, UpdateStatus}
import org.virtuslab.yaml.YamlCodec
import akka.actor.typed.ActorRef
import com.github.nscala_time.time.Imports.*

import java.io.{BufferedReader, ByteArrayOutputStream, InputStreamReader}
import java.util.Scanner
import org.virtuslab.yaml.StringOps

object Slave:
    def apply(ddosSensor: ActorRef[DeviceMessage], idCamera: Int): Unit =
        val pb = new ProcessBuilder(
            "python",
            "-u",
            "raspberry/src/main/resources/yolov7-object-tracking/detect_and_track.py",
            "--weights", "raspberry/src/main/resources/yolov7-object-tracking/yolov7-tiny.pt",
            "--save-txt",
            "--save-bbox-dim",
            "--source", "raspberry/src/main/resources/yolov7-object-tracking/video.mp4", //TODO change to 0 for webcam
            "--classes", "0 1 2 3 5 7",
            //"--device", "0",
            "--name", "YOLOV7 Object Tracking"
        ).redirectOutput(ProcessBuilder.Redirect.PIPE)
        val process = pb.start()
        val bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream))
        var lastMaxIds = Map[Int, Int]()
        var detectedObjects = Map[Int, Int](0 -> 0, 1 -> 0, 2 -> 0, 3 -> 0, 5 -> 0, 7 -> 0)
        var dt: DateTime = DateTime.now()
        while(process.isAlive)
            var line = bufferedReader.readLine()
            if(line != null)
               if(line.startsWith("{"))
                   //println(line)
                   val root = line.as[Root]
                   root match
                       case Right(Root(frame, classes, detections)) =>
                           if(DateTime.now().getMinuteOfHour() != dt.getMinuteOfHour())
                               //ddosSensor ! UpdateStatus(new DataStructure(idCamera, dt, detectedObjects))
                               dt = DateTime.now()
                               detectedObjects = Map[Int, Int](0 -> 0, 1 -> 0, 2 -> 0, 3 -> 0, 5 -> 0, 7 -> 0)
                           val maxIds: Map[Int,Int] = detections.groupBy(_.class_name).map((entry) => (entry._1.toInt, entry._2.map(_.id).max))
                           maxIds.foreach((entry) => {
                               if(entry._2 > lastMaxIds(entry._1)) // => detected new objects for that class
                                 detectedObjects += (entry._1 -> (detectedObjects(entry._1) + entry._2 - lastMaxIds(entry._1)))
                           })
                           lastMaxIds = maxIds
                           if(detectedObjects.nonEmpty)
                            println(detectedObjects)
                       case _ =>
        process.waitFor()

case class Root(frame: String, classes: Map[String, Int], detections: List[Detection]) derives YamlCodec
case class Frame(currentFrame: Int, totalFrames: Int) derives YamlCodec
case class Classes(classes: Map[String, Int]) derives YamlCodec
case class Detection(
    id: Int,
    `class`: Int,
    class_name: String,
    centroid_x: Double,
    centroid_y: Double,
    width: Double,
    height: Double,
) derives YamlCodec