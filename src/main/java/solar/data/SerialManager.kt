package solar.data

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING
import io.reactivex.Completable
import io.reactivex.Maybe
import java.util.*

class SerialManager {

    private companion object {
        val TEENSY_ANNOUNCE_REQUEST = "sst36vuw".toByteArray()
        val TEENSY_ANNOUNCE_RESPONSE = "active".toByteArray()
        val TEENSY_ACK = "ack".toByteArray()
        val TARGET_HEADER = "temp:"
        val THRESHOLD_HEADER = "t_thresh:"
        val BAUD_RATE = 115200
    }

    private var port: SerialPort? = null

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

    fun sendTemperatureTargets(targets: List<Double>): Completable {
        val maybeToMap: Maybe<SerialPort> = if (port == null) {
            getSolarPort()
        } else {
            Maybe.just(port)
        }

        return maybeToMap.flatMapCompletable { port -> Completable.fromCallable {
            var targetStr = TARGET_HEADER
            for ((index, target) in targets.withIndex()) {
                targetStr += "%.1f".format(target)
                targetStr += if (index < targets.size - 1) "," else "\n"
            }

            with (port) {
                baudRate = BAUD_RATE
                setComPortTimeouts(TIMEOUT_READ_BLOCKING, 100, 100)
                openPort()
                outputStream.write(targetStr.toByteArray())
                outputStream.close()

                val input = ByteArray(TEENSY_ACK.size)
                inputStream.read(input)

                if (Arrays.equals(input, TEENSY_ACK)) {
                    logAck("Temperature Targets")
                } else {
                    logNotAck("Temperature Targets")
                }

                closePort()
            }
        }
        }
    }

    private fun logAck(what: String) {
        System.out.println("Device acknowledged: " + what)
    }

    private fun logNotAck(what: String) {
        System.out.println("Device didn't acknowledge: " + what)
    }

    class DeviceNotFoundException(msg: String = "Device was not found in port scan") : Exception(msg)
}