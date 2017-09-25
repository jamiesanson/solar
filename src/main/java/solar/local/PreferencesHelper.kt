@file:Suppress("UNCHECKED_CAST")

package solar.local

import com.google.gson.Gson
import java.util.prefs.Preferences

class PreferencesHelper {

    private companion object {
        val KEY_TARGETS = "targets"
    }

    fun getTemperatureTargets() : ArrayList<Double> = getKey(KEY_TARGETS, ArrayList())

    fun appendTemperatureTarget(toAppend: Double) {
        val targets: ArrayList<Double> = getKey(KEY_TARGETS, ArrayList())
        targets.add(toAppend)
        setKey(KEY_TARGETS, targets)
    }

    fun removeTemperatureTarget(toRemove: Double) {
        val targets: ArrayList<Double> = getKey(KEY_TARGETS, ArrayList())
        targets.remove(toRemove)
        setKey(KEY_TARGETS, targets)
    }

    private fun setKey(key: String, obj: Any) {
        getPrefs().put(key, Gson().toJson(obj))
    }

    private inline fun <reified T> getKey(key: String, default: T) : T {
        val defaultVal = Gson().toJson(default)
        val prefVal = getPrefs().get(key, defaultVal)
        return Gson().fromJson(prefVal, T::class.java)
    }

    private fun getPrefs() = Preferences.userNodeForPackage(this::class.java)!!
}