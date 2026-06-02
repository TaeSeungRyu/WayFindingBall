package com.rts.rys.ryy.wayfinding.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
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
    private const val BGM_FILE = "bgm_loop.wav"
    private const val BGM_VERSION = 2
    private const val STAR_TONE_COUNT = 12
    private const val STAR_TONE_VERSION = 1

    /** C5부터 G6까지 C-major 음계 12음(흰건반만). 별 order-1을 인덱스로 사용. */
    private val STAR_TONE_HZ = doubleArrayOf(
        523.25, 587.33, 659.25, 698.46, 783.99, 880.00,
        987.77, 1046.50, 1174.66, 1318.51, 1396.91, 1567.98,
    )

    private var soundPool: SoundPool? = null
    private var bonkId: Int = 0
    private var goalId: Int = 0
    private var bonkLoaded = false
    private var goalLoaded = false

    private var bgmPlayer: MediaPlayer? = null
    private var bgmFile: File? = null

    // 별자리 모드용 — 별 순서마다 다른 음정. C major 음계 한 옥타브 반(12음).
    private val starToneIds = IntArray(STAR_TONE_COUNT)
    private val starToneLoaded = BooleanArray(STAR_TONE_COUNT)

    // 별자리 이름을 읽어주는 TTS — null이면 아직 초기화 안 됐거나 한국어 미지원.
    private var tts: TextToSpeech? = null
    private var ttsReady = false

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
                    for (i in starToneIds.indices) {
                        if (sampleId == starToneIds[i]) starToneLoaded[i] = true
                    }
                }
            }
            soundPool = pool

            val bonkFile = File(context.cacheDir, BONK_FILE)
            if (!bonkFile.exists()) writeWav(bonkFile, generateBonk())
            bonkId = pool.load(bonkFile.absolutePath, 1)

            val goalFile = File(context.cacheDir, GOAL_FILE)
            if (!goalFile.exists()) writeWav(goalFile, generateGoal())
            goalId = pool.load(goalFile.absolutePath, 1)

            val bgmFile = File(context.cacheDir, "bgm_loop_v${BGM_VERSION}.wav")
            if (!bgmFile.exists()) writeWav(bgmFile, generateBgm())
            this.bgmFile = bgmFile

            // 별 음정 12개 미리 생성/로드 — 종 소리 톤.
            for (i in 0 until STAR_TONE_COUNT) {
                val toneFile = File(context.cacheDir, "sfx_star_v${STAR_TONE_VERSION}_$i.wav")
                if (!toneFile.exists()) writeWav(toneFile, generateStarTone(STAR_TONE_HZ[i]))
                starToneIds[i] = pool.load(toneFile.absolutePath, 1)
            }
        }

        // TTS — 한국어 별자리 이름 안내. 초기화 비동기.
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.KOREAN)
                    ttsReady = result == TextToSpeech.LANG_AVAILABLE ||
                        result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                        result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
                    tts?.setSpeechRate(0.95f)
                    tts?.setPitch(1.05f)
                }
            }
        }
    }

    fun startBgm() {
        if (!AppSettings.bgmEnabled.value) return
        val file = bgmFile ?: return
        if (bgmPlayer?.isPlaying == true) return
        runCatching {
            val mp = bgmPlayer ?: MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                isLooping = true
                setVolume(0.45f, 0.45f)
                prepare()
            }
            bgmPlayer = mp
            mp.start()
        }
    }

    fun pauseBgm() {
        runCatching { bgmPlayer?.takeIf { it.isPlaying }?.pause() }
    }

    fun stopBgm() {
        runCatching {
            bgmPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        bgmPlayer = null
    }

    fun applyBgmEnabled() {
        if (AppSettings.bgmEnabled.value) startBgm() else pauseBgm()
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

    /** 별 잇기 시 [orderIndex] (0-based) 별의 음정을 재생. 범위 밖이면 가장 가까운 톤 사용. */
    fun playStarTone(orderIndex: Int) {
        if (!AppSettings.soundEnabled.value) return
        val pool = soundPool ?: return
        val i = orderIndex.coerceIn(0, STAR_TONE_COUNT - 1)
        if (!starToneLoaded[i]) return
        pool.play(starToneIds[i], 0.85f, 0.85f, 2, 0, 1f)
    }

    /** 별자리 이름 등 짧은 한국어 텍스트를 TTS로 읽어준다. 비활성/미준비 시 무시. */
    fun speak(text: String) {
        if (!AppSettings.soundEnabled.value) return
        if (!ttsReady) return
        runCatching {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "constellation")
        }
    }

    // ---- Sample generation ----

    /** 종 같은 별 음정 — 사인파 + 부드러운 어택/감쇠. ~280ms 짧고 또렷하게. */
    private fun generateStarTone(freq: Double): ShortArray {
        val durationS = 0.28
        val n = (SAMPLE_RATE * durationS).toInt()
        val out = ShortArray(n)
        val attack = 0.008
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = when {
                t < attack -> t / attack
                else -> exp(-(t - attack) * 7.0)
            }
            // 종 느낌: 기본 주파수 + 옥타브 + 약한 5도 배음
            val s = (
                sin(2 * PI * freq * t) * 0.55 +
                    sin(2 * PI * freq * 2 * t) * 0.18 +
                    sin(2 * PI * freq * 3 * t) * 0.08
                ) * env * 0.65
            out[i] = (s.coerceIn(-0.99, 0.99) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

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

    /**
     * Calm loopable BGM, ~16s. Chord progression I–vi–IV–V in C major with a
     * gentle arpeggio + soft bass. Pure sine waves with attack/release shaping
     * so the loop point doesn't click.
     */
    private fun generateBgm(): ShortArray {
        val durationS = 16.0
        val n = (SAMPLE_RATE * durationS).toInt()
        val out = DoubleArray(n)

        // 4 chords, 4초 each. C major - A minor - F major - G major.
        val chords = arrayOf(
            doubleArrayOf(261.63, 329.63, 392.00),  // C  E  G
            doubleArrayOf(220.00, 261.63, 329.63),  // A  C  E
            doubleArrayOf(174.61, 220.00, 261.63),  // F  A  C
            doubleArrayOf(196.00, 246.94, 293.66),  // G  B  D
        )
        val chordDurS = durationS / chords.size
        val crossfadeS = 0.6

        // Pad layer: sustained chord stack with soft crossfade between chords.
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val chordIdx = (t / chordDurS).toInt().coerceIn(0, chords.size - 1)
            val nextIdx = (chordIdx + 1) % chords.size
            val tIn = t - chordIdx * chordDurS
            val mainAmp = if (tIn > chordDurS - crossfadeS) {
                (chordDurS - tIn) / crossfadeS
            } else 1.0
            val nextAmp = 1.0 - mainAmp

            var s = 0.0
            for (f in chords[chordIdx]) s += sin(2 * PI * f * t) * 0.10 * mainAmp
            if (nextAmp > 0.0) {
                for (f in chords[nextIdx]) s += sin(2 * PI * f * t) * 0.10 * nextAmp
            }
            out[i] = s
        }

        // Bass layer: root of each chord, one octave down, soft attack.
        for (m in chords.indices) {
            val rootHz = chords[m][0] / 2.0
            val start = (m * chordDurS * SAMPLE_RATE).toInt()
            val len = (chordDurS * SAMPLE_RATE).toInt()
            val attack = 0.08
            val release = 0.4
            for (k in 0 until len) {
                val idx = start + k
                if (idx >= n) break
                val tk = k.toDouble() / SAMPLE_RATE
                val env = when {
                    tk < attack -> tk / attack
                    tk > chordDurS - release -> ((chordDurS - tk) / release).coerceAtLeast(0.0)
                    else -> 1.0
                }
                out[idx] += sin(2 * PI * rootHz * (start + k).toDouble() / SAMPLE_RATE) * 0.16 * env
            }
        }

        // Arpeggio layer: pluck the chord tones one octave up, 2 notes per second.
        val plucksPerSec = 2
        val pluckDurS = 1.0 / plucksPerSec
        val pluckLen = (pluckDurS * SAMPLE_RATE).toInt()
        val totalPlucks = (durationS * plucksPerSec).toInt()
        for (p in 0 until totalPlucks) {
            val tStart = p * pluckDurS
            val chordIdx = (tStart / chordDurS).toInt().coerceIn(0, chords.size - 1)
            val chord = chords[chordIdx]
            // pattern within each chord period: root, fifth, third, fifth (4 plucks per 2s chord = 4)
            val withinChord = p - chordIdx * plucksPerSec * (chordDurS).toInt()
            val pattern = intArrayOf(0, 2, 1, 2)
            val note = chord[pattern[withinChord.mod(pattern.size)]] * 2.0
            val startSample = (tStart * SAMPLE_RATE).toInt()
            val attack = 0.01
            for (k in 0 until pluckLen) {
                val idx = startSample + k
                if (idx >= n) break
                val tk = k.toDouble() / SAMPLE_RATE
                val env = when {
                    tk < attack -> tk / attack
                    else -> exp(-(tk - attack) * 4.0)
                }
                out[idx] += sin(2 * PI * note * (startSample + k).toDouble() / SAMPLE_RATE) * 0.09 * env
            }
        }

        // Edge taper to avoid loop click.
        val edgeS = 0.12
        val edgeN = (edgeS * SAMPLE_RATE).toInt()
        for (i in 0 until edgeN) {
            val gain = i.toDouble() / edgeN
            out[i] *= gain
            out[n - 1 - i] *= gain
        }

        // Normalize to safe peak.
        var peak = 0.0
        for (v in out) { val a = abs(v); if (a > peak) peak = a }
        val scale = if (peak > 0.0) 0.65 / peak else 1.0
        return ShortArray(n) { i ->
            val s = (out[i] * scale).coerceIn(-0.99, 0.99)
            (s * Short.MAX_VALUE).toInt().toShort()
        }
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
