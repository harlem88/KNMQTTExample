package org.udoo.iot.simpsonhouse

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen

private const val fileName = "/sys/class/i2c-dev/i2c-1/device/1-0029/iio:device0/in_intensity_ir_raw"

object LightI2CSensor{

    fun read() : Int?{

        val file = fopen(fileName, "r")
        if (file == null) {
            println("cannot open input file $fileName")
            return null
        }

        var value: Int?

        try {
            value = memScoped {
                val bufferLength = 1024
                val buffer = allocArray<ByteVar>(bufferLength)
                val line = fgets(buffer, bufferLength, file)?.toKString()

                if (line != null && line.isNotEmpty()) {
                    return line.trim().toInt()
                }else null
            }
        } finally {
            fclose(file)
        }

        return value
    }
}