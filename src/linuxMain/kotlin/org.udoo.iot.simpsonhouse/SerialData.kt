package org.udoo.iot.simpsonhouse

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

private const val TAG = "SerialData"

class SerialData(private val port: String) {

    private var worker: Worker ?= null
    private var fd: Int = -1

    fun start(){
        worker = Worker.start()

        fd = open(port, O_RDWR or O_NOCTTY or O_NDELAY)

        println("$TAG try to open $port")

        if (fd == -1) {
            println("$TAG open_port: Unable to open $port")
        } else {
            println("$TAG opened $port")

            memScoped {
                val options: termios = alloc()
                val optionsPtr = options.ptr

                tcgetattr(fd, optionsPtr)
                cfsetispeed(optionsPtr, B115200)
                cfsetospeed(optionsPtr, B115200)

                options.c_cflag = options.c_cflag and PARENB.toUInt().inv()
                options.c_cflag = options.c_cflag and CSTOPB.toUInt().inv()
                options.c_cflag = options.c_cflag and CSIZE.toUInt().inv()
                options.c_cflag = options.c_cflag or CS8.toUInt()
                options.c_cflag = options.c_cflag or (CREAD.toUInt() or CLOCAL.toUInt())  // turn on READ & ignore ctrl lines

                options.c_lflag = options.c_lflag and (ICANON.toUInt() or ECHO.toUInt() or ECHOE.toUInt() or ISIG.toUInt()).inv()

                options.c_iflag = options.c_iflag and (IXON.toUInt() or IXOFF.toUInt() or IXANY.toUInt()).inv()

                tcsetattr(fd, TCSANOW, optionsPtr)

                fcntl(fd, F_SETFL, 0)
            }

            sleep(2)
        }
    }

    fun read() : Future<ByteArray> ?{
        if (fd == -1) return null

        return worker?.execute(TransferMode.UNSAFE, {fd}) {

            val bufferByteArray = ArrayList<Byte>()

            memScoped {

                var foundEnd = false
                val size = 1
                val buffer = allocArray<ByteVar>(size)
                var reads = read(it, buffer, size.convert()).toInt()

                while(!foundEnd){

                    val i = 0

                    if( reads >= 0 && !foundEnd ){
                        if (buffer[i].toInt() == 10) {
                            foundEnd = true
                        } else {
                            bufferByteArray.add(buffer[i])
                        }
                    }

                    reads = read(it, buffer, size.convert()).toInt()
                }
                println("reads ${bufferByteArray.toByteArray().stringFromUtf8()}")
                bufferByteArray.toByteArray()
            }
        }
    }

    fun stop() {
        if(fd != -1) close(fd)
        worker?.requestTermination(true)
    }

}