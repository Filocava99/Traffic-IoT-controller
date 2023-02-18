package it.unibo.smartcity.raspberry.json.models

import org.virtuslab.yaml.YamlCodec

case class Detection(
                      id: Int,
                      `class`: Int,
                      class_name: String,
                      centroid_x: Double,
                      centroid_y: Double,
                      width: Double,
                      height: Double,
                    )derives YamlCodec
