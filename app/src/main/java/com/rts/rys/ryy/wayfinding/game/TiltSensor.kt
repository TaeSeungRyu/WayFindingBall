package com.rts.rys.ryy.wayfinding.game

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Wraps the accelerometer and exposes a low-passed (x, y) tilt vector
 * normalized to roughly [-1, 1] suitable for use as ball acceleration input.
 *
 * Positive x = device tilted right (ball moves right).
 * Positive y = device tilted down/toward user (ball moves down).
 */
class TiltSensor(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile var tiltX: Float = 0f; private set
    @Volatile var tiltY: Float = 0f; private set

    private val alpha = 0.18f

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        tiltX = 0f
        tiltY = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        // Tilt direction matches ball movement direction:
        // tilt right -> ball right (+x), tilt left -> ball left (-x),
        // tilt down (bottom-edge down) -> ball down (+y), tilt up -> ball up (-y).
        val nx = (-event.values[0] / 9.81f).coerceIn(-1f, 1f)
        val ny = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
        tiltX = lerp(tiltX, nx, alpha)
        tiltY = lerp(tiltY, ny, alpha)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
