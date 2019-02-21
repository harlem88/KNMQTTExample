package org.udoo.iot.simpsonhouse

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

private const val path = "/sys/class/i2c-dev/i2c-1/device/1-0040"
private const val HumidityFileName = "$path/humidity1_input"
private const val TemperatureFileName = "$path/temp1_input"

object HumidityI2CSensor{

    fun read() : Array<Float?>{
        val values = Array<Float?>(2) {null}
        val humidityFile = fopen(HumidityFileName, "r")

        if (humidityFile == null) {
            println("cannot open input file $HumidityFileName")
        }else{

            try {
                values[0] = memScoped {
                    val bufferLength = 1024
                    val buffer = allocArray<ByteVar>(bufferLength)
                    val line = fgets(buffer, bufferLength, humidityFile)?.toKString()

                    toScaledNumber(line, 1000f)
                }
            } finally {
                fclose(humidityFile)
            }
        }

        val fileTemperature = fopen(TemperatureFileName, "r")
        if (fileTemperature == null) {
            println("cannot open input file $TemperatureFileName")
        }else{
            try {
                values[1] = memScoped {
                    val bufferLength = 1024
                    val buffer = allocArray<ByteVar>(bufferLength)
                    val line = fgets(buffer, bufferLength, fileTemperature)?.toKString()

                    toScaledNumber(line, 1000f)
                }
            } finally {
                fclose(fileTemperature)
            }

        }

        return values
    }
}

fun toScaledNumber(number: String?, scale: Float) : Float? {
    return if (number != null && number.isNotEmpty()) {
        if (number.toFloat() != 0f) {
            number.toFloat() / scale
        } else { 0f }
    } else { null }
}