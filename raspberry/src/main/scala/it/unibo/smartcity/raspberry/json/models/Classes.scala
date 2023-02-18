package it.unibo.smartcity.raspberry.json.models

import org.virtuslab.yaml.YamlCodec

case class Classes(classes: Map[String, Int])derives YamlCodec
