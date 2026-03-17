package com.tqmane.filmsim.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * Utility for observing device tilt (pitch and roll) to drive parallax animations.
 * Emit updates mapped roughly to [-1f, 1f] for pitch and roll.
 */
object SensorUtils {

    data class TiltData(val pitch: Float, val roll: Float)

    /**
     * Observes device tilt using Gravity or Accelerometer sensors.
     */
    fun observeTilt(context: Context): Flow<TiltData> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            trySend(TiltData(0f, 0f))
            close()
            return@callbackFlow
        }

        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (gravitySensor == null) {
            trySend(TiltData(0f, 0f))
            close()
            return@callbackFlow
        }

        var lastEmitTime = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event ?: return
                val now = System.currentTimeMillis()
                // Throttle updates slightly to 60fps max to save battery
                if (now - lastEmitTime < 16L) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Rough normalization, max gravity is ~9.8
                // We map values so that slight tilts give a good [0..1] range
                val roll = (x / 9.81f).coerceIn(-1f, 1f)
                val pitch = (y / 9.81f).coerceIn(-1f, 1f)

                trySend(TiltData(pitch = pitch, roll = roll))
                lastEmitTime = now
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, gravitySensor, SensorManager.SENSOR_DELAY_UI)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
