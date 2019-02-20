package org.udoo.iot.simpsonhouse

import kotlinx.cinterop.*
import libpahomqtt.*
import platform.posix.NULL
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker


private const val URI  = "tcp://"
private const val CLIENTID = "house"
private const val TOPIC    = "sensors"
private const val QOS      = 1
private const val TIMEOUT  = 10000L
private const val PORT     = 1883

class PahoMqttClient {
    private val mqttClient: MQTTClientVar = nativeHeap.alloc()
    private val worker: Worker by lazy { Worker.start() }

    fun connect(ip: String) {
        memScoped {

            val serverURI = "$URI$ip:$PORT"

            var rc = MQTTClient_create(
                mqttClient.ptr,
                serverURI,
                CLIENTID,
                MQTTCLIENT_PERSISTENCE_NONE,
                NULL
            )
            println("===== MQTTClient_create result $rc =======")

            val connOpts : MQTTClient_connectOptions = mqttClient_connectOptions_initializer().ptr.pointed
            connOpts.MQTTVersion = 0
            connOpts.keepAliveInterval = 20
            connOpts.cleansession = 1

            println("===== MQTTClient_connecting to $serverURI =======")
            rc = MQTTClient_connect(mqttClient.value, connOpts.ptr)

            if (rc != MQTTCLIENT_SUCCESS) {
                println("Failed to connect, return code $rc")
            }else{
                println("===== MQTTClient_connected =======")
            }
        }
    }


    fun publish(futureSensorEvent: Future<ByteArray>?) {
        worker.execute(TransferMode.SAFE, {
            Event(mqttClient, futureSensorEvent)}) { event -> {
            event.futureSensor?.consume { sensors ->
                println("sensor ======== ${sensors.stringFromUtf8()}")
//                publish(event.mqttClient, sensors.stringFromUtf8())
            }
        }
        }
    }

    fun publish(message: String, topic: String){
        memScoped {
            val buffer = message.toUtf8().toCValues()

            val pubMsg : MQTTClient_message = mqttClient_message_initializer().ptr.pointed
            var token : MQTTClient_deliveryTokenVar = alloc()

            pubMsg.payload = buffer.ptr
            pubMsg.payloadlen = buffer.size
            pubMsg.qos = QOS
            pubMsg.retained = 0
            MQTTClient_publishMessage(mqttClient.ptr, topic, pubMsg.ptr, token.ptr)

            val rc = MQTTClient_waitForCompletion(mqttClient.ptr, token.value, TIMEOUT.toUInt())
            println("$rc: Message with delivery token %d delivered ${token.value}")
        }
    }

    fun close(){

        MQTTClient_disconnect(mqttClient.ptr, 10000)
        MQTTClient_destroy(mqttClient.ptr)
    }

    data class Event(val mqttClient: MQTTClientVar , val futureSensor: Future<ByteArray>?)
}

fun publish(mqttClient: MQTTClientVar, message: String){
    memScoped {
        val buffer = message.toUtf8().toCValues()

        val pubMsg : MQTTClient_message = mqttClient_message_initializer().ptr.pointed
        var token : MQTTClient_deliveryTokenVar = alloc()

        pubMsg.payload = buffer.ptr
        pubMsg.payloadlen = buffer.size
        pubMsg.qos = QOS
        pubMsg.retained = 0
        MQTTClient_publishMessage(mqttClient.ptr, TOPIC, pubMsg.ptr, token.ptr)

        val rc = MQTTClient_waitForCompletion(mqttClient.ptr, token.value, TIMEOUT.toUInt())
        println("$rc: Message with delivery token %d delivered ${token.value}")
    }
}