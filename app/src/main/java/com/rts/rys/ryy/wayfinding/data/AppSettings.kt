package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

object AppSettings {
    private const val PREFS = "maze_settings"
    private const val KEY_SENSOR_ENABLED = "sensor_enabled"

    private var prefs: SharedPreferences? = null

    private val _sensorEnabled: MutableState<Boolean> = mutableStateOf(true)
    val sensorEnabled: MutableState<Boolean> get() = _sensorEnabled

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs = p
            _sensorEnabled.value = p.getBoolean(KEY_SENSOR_ENABLED, true)
        }
    }

    fun setSensorEnabled(value: Boolean) {
        _sensorEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_SENSOR_ENABLED, value)?.apply()
    }
}
