package solar.data

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import solar.data.model.DeviceState
import java.util.*

class SerialManager {

    private companion object {
        val TEENSY_ANNOUNCE_REQUEST = "sst36vuw".toByteArray()
        val TEENSY_ANNOUNCE_RESPONSE = "active".toByteArray()
        val TEENSY_ACK = "ack".toByteArray()
        val SWEEP_REQUEST = "runsweep"
        val BAUD_RATE = 115200
        val RETRY_CONN_COUNT = 3
    }

    private var port: SerialPort? = null

    private fun getSolarPort() : Maybe<SerialPort> {
        return Maybe.create({ subscriber ->
            for (i in 1..RETRY_CONN_COUNT) {
                for (port in SerialPort.getCommPorts()) {
                    with(port) {
                        baudRate = BAUD_RATE
                        setComPortTimeouts(TIMEOUT_READ_BLOCKING, 500, 100)
                        openPort()
                        outputStream.write(TEENSY_ANNOUNCE_REQUEST)
                        val input = ByteArray(TEENSY_ANNOUNCE_RESPONSE.size)
                        inputStream.read(input)
                        outputStream.close()
                        inputStream.close()

                        closePort()
                        if (Arrays.equals(input, TEENSY_ANNOUNCE_RESPONSE)) {
                            try {
                                System.out.println("Found device at " + port.systemPortName)
                                subscriber.onSuccess(port)
                                subscriber.onComplete()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return@create
                        }
                    }
                }

                // Sleep for a little bit to see if Serial sorts itself
                Thread.sleep(500L)
            }

            subscriber.onError(DeviceNotFoundException())
            subscriber.onComplete()
        })
    }

    fun sendSweepRequest(): Completable {
        return sendRequest(SWEEP_REQUEST, "Sweep Request")
    }

    private fun sendRequest(toSend: String, desc: String): Completable {
        val maybeToMap: Maybe<SerialPort> = if (port == null) {
            getSolarPort()
        } else {
            Maybe.just(port)
        }

        return maybeToMap.flatMapCompletable { port -> Completable.fromCallable {
            with (port) {
                baudRate = BAUD_RATE
                setComPortTimeouts(TIMEOUT_READ_BLOCKING, 100, 100)
                openPort()
                outputStream.write(toSend.toByteArray())
                outputStream.close()

                val input = ByteArray(TEENSY_ACK.size)
                inputStream.read(input)

                if (Arrays.equals(input, TEENSY_ACK)) {
                    logAck(desc)
                } else {
                    logNotAck(desc)
                }

                closePort()
            }
        }
        }
    }

    fun getStateRx(): Observable<DeviceState> {
        val maybeToMap: Maybe<SerialPort> = if (port == null) {
            getSolarPort()
        } else {
            Maybe.just(port)
        }


        return maybeToMap.flatMapObservable {
            port ->
            Observable.create<DeviceState> { s ->
                while (true) {
                    try {
                        val state = readState(port)
                        if (state != null) {
                            s.onNext(state)
                        }
                    } catch (e: DeviceNotFoundException) {
                        s.onError(e)
                        break
                    }
                }

                s.onComplete()
            }
        }
    }

    @Throws(DeviceNotFoundException::class)
    private fun readState(port: SerialPort): DeviceState? {
        var stateString = ""

        with (port) {
            baudRate = BAUD_RATE
            setComPortTimeouts(TIMEOUT_READ_BLOCKING, 100, 100)
            openPort()

            var byteRead = inputStream.read().toChar()
            while (byteRead != '\n') {
                stateString += byteRead
                byteRead = inputStream.read().toChar()
            }

            closePort()
        }

        val jsonString = sanitiseStateString(stateString)
        return if (jsonString.isNotEmpty()) {
            try {
                Gson().fromJson(jsonString, DeviceState::class.java)
            } catch (e: Exception) {
                System.err.println("Malformed JSON String recieved")
                return null
            }
        } else null
    }

    private fun sanitiseStateString(string: String): String {
        // Basic JSON validation
        if (string.contains("active")
                || (string.contains("{") && !string.contains("}"))
                || (string.contains("}") && !string.contains("{"))) {

            return ""
        }

        return string
    }

    private fun logAck(what: String) {
        System.out.println("Device acknowledged: " + what)
    }

    private fun logNotAck(what: String) {
        System.out.println("Device didn't acknowledge: " + what)
    }

    class DeviceNotFoundException(msg: String = "Device was not found in port scan") : Exception(msg)
}