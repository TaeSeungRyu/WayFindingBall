package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Lightweight SFX with no shipped assets: generates a percussive bonk and an ascending
 * arpeggio chime as PCM at startup, writes them as WAV into cacheDir, then plays via SoundPool.
 */
object SoundManager {
    private const val SAMPLE_RATE = 44100
    private const val BONK_FILE = "sfx_bonk.wav"
    private const val GOAL_FILE = "sfx_goal.wav"

    private var soundPool: SoundPool? = null
    private var bonkId: Int = 0
    private var goalId: Int = 0
    private var bonkLoaded = false
    private var goalLoaded = false

    fun init(context: Context) {
        if (soundPool != null) return
        runCatching {
            val pool = SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    if (sampleId == bonkId) bonkLoaded = true
                    if (sampleId == goalId) goalLoaded = true
                }
            }
            soundPool = pool

            val bonkFile = File(context.cacheDir, BONK_FILE)
            if (!bonkFile.exists()) writeWav(bonkFile, generateBonk())
            bonkId = pool.load(bonkFile.absolutePath, 1)

            val goalFile = File(context.cacheDir, GOAL_FILE)
            if (!goalFile.exists()) writeWav(goalFile, generateGoal())
            goalId = pool.load(goalFile.absolutePath, 1)
        }
    }

    fun playBonk() {
        if (!AppSettings.soundEnabled.value) return
        val pool = soundPool ?: return
        if (!bonkLoaded) return
        pool.play(bonkId, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun playGoal() {
        if (!AppSettings.soundEnabled.value) return
        val pool = soundPool ?: return
        if (!goalLoaded) return
        pool.play(goalId, 1f, 1f, 1, 0, 1f)
    }

    // ---- Sample generation ----

    private fun generateBonk(): ShortArray {
        // 90ms percussive thump around 180Hz with snappy decay.
        val durationS = 0.09f
        val baseFreq = 180.0
        val n = (SAMPLE_RATE * durationS).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            // exponential decay envelope
            val env = exp(-t * 35.0)
            // pitch drops slightly for a more "thumpy" feel
            val freq = baseFreq * (1.0 - t * 1.4)
            val s = sin(2 * PI * freq * t) * env * 0.85
            out[i] = (s * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun generateGoal(): ShortArray {
        // C5-E5-G5-C6 arpeggio across ~0.55s, each note has soft attack and decay.
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
        val noteDur = 0.16
        val noteSamples = (SAMPLE_RATE * noteDur).toInt()
        val totalSamples = noteSamples * notes.size
        val out = ShortArray(totalSamples)
        for (n in notes.indices) {
            val freq = notes[n]
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = 0.012
                val env = when {
                    t < attack -> t / attack
                    else -> exp(-(t - attack) * 5.0)
                }
                // add a soft second harmonic for warmth
                val s = (sin(2 * PI * freq * t) + 0.25 * sin(4 * PI * freq * t)) * env * 0.5
                val idx = n * noteSamples + i
                out[idx] = (s.coerceIn(-0.99, 0.99) * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out
    }

    private fun writeWav(file: File, samples: ShortArray) {
        FileOutputStream(file).use { out ->
            val byteRate = SAMPLE_RATE * 2
            val dataSize = samples.size * 2

            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16)
            header.putShort(1)
            header.putShort(1)
            header.putInt(SAMPLE_RATE)
            header.putInt(byteRate)
            header.putShort(2)
            header.putShort(16)
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())

            val body = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (s in samples) body.putShort(s)
            out.write(body.array())
        }
    }
}
