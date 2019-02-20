package org.udoo.iot.simpsonhouse

import platform.posix.usleep
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

private var temperature : Float = 0F
private var lux : Int = 0

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
        pahoMqttClient.publish(newEvents)

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

private fun readSensors(pahoMqttClient: PahoMqttClient){
    Worker.start().execute(TransferMode.UNSAFE, {}){
        val temp = TemperatureI2CSensor.read()
        val light = LightI2CSensor.read()
        var json = ""

        if(temp != null && temp != temperature){
            temperature = temp
            json +="\"temperature\":$temp"
        }

        if(light != null && lux != light){
            lux = light

            if(json.isNotEmpty()) json+= ","

            json +="\"light\":$light"
        }

        if(json.isNotEmpty()){
            pahoMqttClient.publish(json, "/sensors-i2c")
        }
    }
}