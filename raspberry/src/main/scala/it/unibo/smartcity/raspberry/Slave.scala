package it.unibo.smartcity.raspberry

import akka.actor.typed.ActorRef
import com.github.nscala_time.time.Imports.DateTime
import it.pps.ddos.device.DeviceProtocol.{DeviceMessage, UpdateStatus}
import it.unibo.smartcity.raspberry.json.models.Root
import org.virtuslab.yaml.StringOps
import it.sc.server.entities.RecordedData
import reactivemongo.api.bson.{BSONDateTime, BSONObjectID}

import java.io.{BufferedReader, InputStreamReader}

object Slave:
    def apply(ddosSensor: ActorRef[DeviceMessage], idCamera: String): Unit =
        println("Starting YOLOV7 Object Tracking")
        val pb = new ProcessBuilder(
            "python",
            //            "-u",
            "/home/filippo/Scrivania/Traffic-IoT-controller/raspberry/src/main/resources/yolov7-object-tracking/detect_and_track.py",
            "--weights", "/home/filippo/Scrivania/Traffic-IoT-controller/raspberry/src/main/resources/yolov7-object-tracking/yolov7-tiny.pt",
            "--save-txt",
            "--save-bbox-dim",
            "--source", "/home/filippo/Scrivania/Traffic-IoT-controller/raspberry/src/main/resources/yolov7-object-tracking/video.mp4", //TODO change to 0 for webcam
            //"--classes", "\"0 1 2 3 5 7\"",
            "--classes", "0",
            //"--device", "0",
            "--name", "YOLOV7 Object Tracking"
        ).redirectErrorStream(true)
          .redirectError(ProcessBuilder.Redirect.INHERIT)
          .redirectOutput(ProcessBuilder.Redirect.PIPE) //.redirectOutput(ProcessBuilder.Redirect.PIPE)
        val process = pb.start()
        val bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream))
        var lastMaxIds = Map[Int, Int]()
        var detectedObjects = Map[Int, Int](0 -> 0, 1 -> 0, 2 -> 0, 3 -> 0, 5 -> 0, 7 -> 0)
        var dt: DateTime = DateTime.now()
        while (process.isAlive)
            var line = bufferedReader.readLine()
            //println("line: ")
            //println(line)
            if (line != null)
                if (line.startsWith("{"))
                    val root = line.as[Root]
                    root match
                        case Right(Root(frame, classes, detections)) =>
                            if (DateTime.now().getSecondOfMinute != dt.getSecondOfMinute)
                                ddosSensor ! UpdateStatus(new RecordedData(idCamera, BSONDateTime(dt.getMillis), detectedObjects))
                                dt = DateTime.now()
                                detectedObjects = Map[Int, Int](0 -> 0, 1 -> 0, 2 -> 0, 3 -> 0, 5 -> 0, 7 -> 0)
                            val maxIds: Map[Int, Int] = detections.groupBy(_.`class`).map((entry) => (entry._1, entry._2.map(_.id).max))
                            //println(maxIds)
                            maxIds.foreach(entry => {
                                if (!lastMaxIds.contains(entry._1)) {
                                    //println("pippo")
                                    detectedObjects += entry
                                } else if (entry._2 > lastMaxIds(entry._1)) // => detected new objects for that class
                                //println("pluto")
                                //detectedObjects += (entry._1 -> (detectedObjects(entry._1) + entry._2 - lastMaxIds(entry._1)))
                                    detectedObjects += (entry._1 -> (entry._2 - lastMaxIds(entry._1)))
                            })
                            lastMaxIds = maxIds
                            if (detectedObjects.nonEmpty)
                                println(detectedObjects)
                        case _ =>
        process.waitFor()
        while (process.isAlive) {}
        println("Process exited")