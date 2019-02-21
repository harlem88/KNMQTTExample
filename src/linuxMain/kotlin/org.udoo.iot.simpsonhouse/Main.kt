package org.udoo.iot.simpsonhouse

import platform.posix.usleep
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

private var temperature1 : Float = 0F
private var temperature2 : Float = 0F
private var lux : Int = 0
private var humidity : Float = 0F

fun main(args: Array<String>) {
    val ip =  parseIP(args)
    println("::::: START ON :::::")

    val serialData = SerialData("/dev/ttyMCC")
    serialData.start()

    val pahoMqttClient = PahoMqttClient()
    pahoMqttClient.connect(ip)

    var i = 0
    while (true) {

        val newEvents = serialData.read()
        write(pahoMqttClient, newEvents)

        if(i == 10){
            readSensors(pahoMqttClient)
            i = 0
        }else{
            i++
        }
        usleep(100 * 1000)
    }
}


private fun parseIP(args: Array<String>) : String{
    return if (args.isNotEmpty()) {
        args[0]
    }else{
        "localhost"
    }
}

private fun write(pahoMqttClient: PahoMqttClient?, futureSensorEvent: Future<ByteArray>?) {
    futureSensorEvent?.consume {
        if(it.size > 2) pahoMqttClient?.publish("/sensors", it.stringFromUtf8())
    }
}

private fun readSensors(pahoMqttClient: PahoMqttClient){
    Worker.start().execute(TransferMode.UNSAFE, {pahoMqttClient}){
        val temp = TemperatureI2CSensor.read()
        val light = LightI2CSensor.read()
        val humTempsValues = HumidityI2CSensor.read()

        var json = ""

        if(temp != null){
            temperature1 = temp
            json +="\"TEMP_1\":$temp"
        }

        if(light != null){
            lux = light

            if(json.isNotEmpty()) json+= ","

            json +="\"LIGHT_1\":$light"
        }

        if(humTempsValues[0] != null){
            humidity = humTempsValues[0]!!

            if(json.isNotEmpty()) json+= ","

            json +="\"HUM_1\":$humidity"
        }

        if(humTempsValues[1] != null){
            temperature2 = humTempsValues[1]!!

            if(json.isNotEmpty()) json+= ","

            json +="\"TEMP_2\":$temperature2"
        }

        if(json.isNotEmpty()){
            json = "{$json}"
            it.publish("/sensors", json)
        }
    }
}