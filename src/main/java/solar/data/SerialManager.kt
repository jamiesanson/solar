package solar.data

import com.fazecast.jSerialComm.SerialPort
import io.reactivex.Maybe
import java.util.*

class SerialManager {

    private companion object {
        val TEENSY_ANNOUNCE_REQUEST = "sst36vuw".toByteArray()
        val TEENSY_ANNOUNCE_RESPONSE = "active".toByteArray()
        val BAUD_RATE = 115200
    }

    fun getSolarPort() : Maybe<SerialPort> {
        return Maybe.create({ subscriber ->
            for (port in SerialPort.getCommPorts()) {
                with(port) {
                    baudRate = BAUD_RATE
                    openPort()
                    outputStream.write(TEENSY_ANNOUNCE_REQUEST)
                    outputStream.close()

                    val input = ByteArray(TEENSY_ANNOUNCE_RESPONSE.size)
                    inputStream.read(input)

                    closePort()
                    if (Arrays.equals(input, TEENSY_ANNOUNCE_RESPONSE)) {
                        subscriber.onSuccess(port)
                        subscriber.onComplete()
                        return@create
                    }
                }
            }

            subscriber.onError(DeviceNotFoundException())
            subscriber.onComplete()
        })
    }

    class DeviceNotFoundException(msg: String = "Device was not found in port scan") : Exception(msg)
}