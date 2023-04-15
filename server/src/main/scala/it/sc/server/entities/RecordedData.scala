package it.sc.server.entities

import it.pps.ddos.device.DeviceProtocol.DeviceMessage

case class RecordedData(idCamera: String, timeStamp: Long, data: Map[Int, Int]) extends DeviceMessage