package it.unibo.smartcity.raspberry.json.models

import org.virtuslab.yaml.YamlCodec

case class Frame(currentFrame: Int, totalFrames: Int)derives YamlCodec
