import it.pps.ddos.device.DeviceProtocol.DeviceMessage
import org.virtuslab.yaml.YamlCodec

import java.io.{BufferedReader, ByteArrayOutputStream, InputStreamReader}
import java.util.Scanner
import org.virtuslab.yaml.StringOps

object Slave(ddosSensor: ActorRef[DeviceMessage]){
    def main(args: Array[String]): Unit = {
        val pb = new ProcessBuilder(
            "python",
            "-u",
            "raspberry/src/main/resources/yolov7-object-tracking/detect_and_track.py",
            "--weights", "raspberry/src/main/resources/yolov7-object-tracking/yolov7-tiny.pt",
            "--save-txt",
            "--save-bbox-dim",
            "--source", "raspberry/src/main/resources/yolov7-object-tracking/video.mp4", //TODO change to 0 for webcam
            "--classes", "0",
            //"--device", "0",
            "--name", "YOLOV7 Object Tracking"
        ).redirectOutput(ProcessBuilder.Redirect.PIPE)
        val process = pb.start()
        val bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream))
        var lastMaxIds = Map[String, Int]()
        while(process.isAlive){
            var line = bufferedReader.readLine()
            if(line != null){
               if(line.startsWith("{")){
                   //println(line)
                   val root = line.as[Root]
                   root match
                       case Right(Root(frame, classes, detections)) =>
                           val maxIds = detections.groupBy(_.class_name).map((entry) => (entry._1, entry._2.map(_.id).max))
                           var message = Map[String, Int]()
                           maxIds.foreach((entry) => {
                               if(lastMaxIds.contains(entry._1)){
                                   if(entry._2 > lastMaxIds(entry._1)){
                                       message += (entry._1 -> (entry._2 - lastMaxIds(entry._1)))
                                   }
                               }else{
                                      message += (entry._1 -> entry._2)
                               }
                           })
                           lastMaxIds = maxIds
                           if(message.nonEmpty)
                            println(message)
                       case _ =>
               }
            }
        }
        process.waitFor()
    }
}

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