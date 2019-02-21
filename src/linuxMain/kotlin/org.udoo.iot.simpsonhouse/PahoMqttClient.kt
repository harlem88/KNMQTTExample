package org.udoo.iot.simpsonhouse

import kotlinx.cinterop.*
import libpahomqtt.*
import platform.posix.NULL


private const val URI  = "tcp://"
private const val CLIENTID = "house"
private const val TOPIC    = "sensors"
private const val QOS      = 1
private const val TIMEOUT  = 10000L
private const val PORT     = 1883

class PahoMqttClient {
    private val mqttClient: MQTTClientVar = nativeHeap.alloc()

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

//            MQTTClient_setCallbacks(mqttClient.value, NULL, staticCFunction(::connectionLost), staticCFunction(::msgarrvd), staticCFunction(::delivered))

            println("===== MQTTClient_connecting to $serverURI =======")
            rc = MQTTClient_connect(mqttClient.value, connOpts.ptr)

            if (rc != MQTTCLIENT_SUCCESS) {
                println("Failed to connect, return code $rc")
            }else{
                println("===== MQTTClient_connected =======")
            }
        }
    }


    fun publish(topic: String, message: String){

        val connected = MQTTClient_isConnected(mqttClient.value) > 0
        if( connected ){
            memScoped {

                val buffer = message.cstr

                val pubMsg : MQTTClient_message = mqttClient_message_initializer().ptr.pointed
                val token : MQTTClient_deliveryTokenVar = alloc()

                pubMsg.payload = buffer.ptr
                pubMsg.payloadlen = buffer.size
                pubMsg.qos = QOS
                pubMsg.retained = 0

                MQTTClient_publishMessage(mqttClient.value, topic, pubMsg.ptr, token.ptr)
                println("to publish on $topic message: $message")
            }
        }
    }

    fun close(){

        MQTTClient_disconnect(mqttClient.ptr, 10000)
        MQTTClient_destroy(mqttClient.ptr)
    }
}