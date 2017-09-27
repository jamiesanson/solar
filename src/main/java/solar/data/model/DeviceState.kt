package solar.data.model

data class DeviceState(
        val thermTemp: Double,
        val rtd1Temp: Double,
        val rtd2Temp: Double,
        val volt: Double,
        val curr: Double,
        val testResult: TestResult?)

data class TestResult(
        val success: Boolean,
        val error: String?
)