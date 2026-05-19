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
 * Positive x = device tilted right, positive y = device tilted forward (top edge
 * lowered) — both push the ball in the visually intuitive direction.
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
        // Accelerometer reading equals the reaction force, i.e. opposite of gravity.
        // Right-edge down -> values[0] > 0 -> ball should roll right (game +x).
        // Top-edge down  -> values[1] < 0 -> ball should roll up   (game -y).
        // Game y axis grows downward, so ay = -values[1].
        val nx = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
        val ny = (-event.values[1] / 9.81f).coerceIn(-1f, 1f)
        tiltX = lerp(tiltX, nx, alpha)
        tiltY = lerp(tiltY, ny, alpha)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
