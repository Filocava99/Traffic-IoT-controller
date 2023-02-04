import java.io.{BufferedReader, ByteArrayOutputStream, InputStreamReader}
import java.util.Scanner

object Main{
    def main(args: Array[String]): Unit = {
        val pb = new ProcessBuilder(
            "python",
            "-u",
            "src/main/resources/yolov7-object-tracking/detect_and_track.py",
            "--weights", "yolov7.pt",
            "--save-txt",
            "--save-bbox-dim",
            "--save-with-object-id",
            "--source", "src/main/resources/yolov7-object-tracking/video.mp4", //TODO change to 0 for webcam
            "--classes", "0",
            "--device", "0",
            "--name", "YOLOV7 Object Tracking"
        ).redirectOutput(ProcessBuilder.Redirect.PIPE)
        val process = pb.start()
        val bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream))
        while(process.isAlive){
            while(bufferedReader.ready()){
                println(bufferedReader.readLine())
            }
        }
        process.waitFor()
    }
}
