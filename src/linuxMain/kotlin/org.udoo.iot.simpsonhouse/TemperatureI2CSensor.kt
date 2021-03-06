package org.udoo.iot.simpsonhouse

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

private const val fileName = "/sensors/temperature/temp1_input"

object TemperatureI2CSensor{

    fun read() : Float?{

        val file = fopen(fileName, "r")
        if (file == null) {
            println("cannot open input file $fileName")
            return null
        }

        var value: Float?

        try {
            value = memScoped {
                val bufferLength = 1024
                val buffer = allocArray<ByteVar>(bufferLength)
                val line = fgets(buffer, bufferLength, file)?.toKString()

                if (line != null && line.isNotEmpty()) {
                    return line.trim().toFloat() / 1000f
                }else null
            }
        } finally {
            fclose(file)
        }

        return value
    }
}