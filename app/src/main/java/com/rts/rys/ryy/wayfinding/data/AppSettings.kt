package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

object AppSettings {
    private const val PREFS = "maze_settings"
    private const val KEY_SENSOR_ENABLED = "sensor_enabled"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_BGM_ENABLED = "bgm_enabled"
    private const val KEY_SENSOR_SENSITIVITY = "sensor_sensitivity"
    private const val KEY_SENSOR_OFFSET_X = "sensor_offset_x"
    private const val KEY_SENSOR_OFFSET_Y = "sensor_offset_y"
    private const val KEY_TUTORIAL_SEEN = "tutorial_seen"

    private var prefs: SharedPreferences? = null

    private val _sensorEnabled: MutableState<Boolean> = mutableStateOf(true)
    val sensorEnabled: MutableState<Boolean> get() = _sensorEnabled

    private val _soundEnabled: MutableState<Boolean> = mutableStateOf(true)
    val soundEnabled: MutableState<Boolean> get() = _soundEnabled

    private val _bgmEnabled: MutableState<Boolean> = mutableStateOf(true)
    val bgmEnabled: MutableState<Boolean> get() = _bgmEnabled

    private val _sensorSensitivity = mutableFloatStateOf(1.0f)
    val sensorSensitivity: MutableState<Float> get() = _sensorSensitivity

    private val _sensorOffsetX = mutableFloatStateOf(0f)
    val sensorOffsetX: MutableState<Float> get() = _sensorOffsetX

    private val _sensorOffsetY = mutableFloatStateOf(0f)
    val sensorOffsetY: MutableState<Float> get() = _sensorOffsetY

    private val _tutorialSeen: MutableState<Boolean> = mutableStateOf(false)
    val tutorialSeen: MutableState<Boolean> get() = _tutorialSeen

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs = p
            _sensorEnabled.value = p.getBoolean(KEY_SENSOR_ENABLED, true)
            _soundEnabled.value = p.getBoolean(KEY_SOUND_ENABLED, true)
            _bgmEnabled.value = p.getBoolean(KEY_BGM_ENABLED, true)
            _sensorSensitivity.value = p.getFloat(KEY_SENSOR_SENSITIVITY, 1.0f)
            _sensorOffsetX.value = p.getFloat(KEY_SENSOR_OFFSET_X, 0f)
            _sensorOffsetY.value = p.getFloat(KEY_SENSOR_OFFSET_Y, 0f)
            _tutorialSeen.value = p.getBoolean(KEY_TUTORIAL_SEEN, false)
        }
    }

    fun setTutorialSeen(value: Boolean) {
        _tutorialSeen.value = value
        prefs?.edit()?.putBoolean(KEY_TUTORIAL_SEEN, value)?.apply()
    }

    fun setSensorEnabled(value: Boolean) {
        _sensorEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_SENSOR_ENABLED, value)?.apply()
    }

    fun setSoundEnabled(value: Boolean) {
        _soundEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_SOUND_ENABLED, value)?.apply()
    }

    fun setBgmEnabled(value: Boolean) {
        _bgmEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_BGM_ENABLED, value)?.apply()
    }

    fun setSensorSensitivity(value: Float) {
        val clamped = value.coerceIn(0.4f, 2.0f)
        _sensorSensitivity.value = clamped
        prefs?.edit()?.putFloat(KEY_SENSOR_SENSITIVITY, clamped)?.apply()
    }

    fun setSensorOffset(x: Float, y: Float) {
        _sensorOffsetX.value = x
        _sensorOffsetY.value = y
        prefs?.edit()
            ?.putFloat(KEY_SENSOR_OFFSET_X, x)
            ?.putFloat(KEY_SENSOR_OFFSET_Y, y)
            ?.apply()
    }

    fun resetSensorOffset() = setSensorOffset(0f, 0f)
}
