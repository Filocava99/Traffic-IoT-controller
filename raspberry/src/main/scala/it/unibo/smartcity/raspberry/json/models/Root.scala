package it.unibo.smartcity.raspberry.json.models

import it.unibo.smartcity.raspberry.json.models.Detection
import org.virtuslab.yaml.YamlCodec

case class Root(frame: String, classes: Map[String, Int], detections: List[Detection])derives YamlCodec
