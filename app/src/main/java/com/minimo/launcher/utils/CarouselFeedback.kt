package com.minimo.launcher.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Sound while the favourites carousel passes a row. */
enum class CarouselSound { Off, Tick, Click }

private const val MAX_SOUND_VOLUME = 0.2f

/**
 * A short pulse of the vibration motor per carousel row. The system "touch haptics" (e.g. the
 * clock tick) are silently ignored on some phones (Samsung), a direct pulse works everywhere.
 */
@Singleton
class CarouselHapticPlayer @Inject constructor(
    @ApplicationContext context: Context
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * @param strength 0..100 %, 0 = off. Both the force and the length of the pulse grow with it:
     *  a soft short tick at the bottom of the scale, a firm knock at the top. Motors without
     *  force control only get the length.
     */
    fun play(strength: Int) {
        val vibrator = vibrator ?: return
        if (strength <= 0 || !vibrator.hasVibrator()) return
        val fraction = strength.coerceIn(1, 100) / 100f
        try {
            val effect = if (vibrator.hasAmplitudeControl()) {
                VibrationEffect.createOneShot(
                    (6 + 14 * fraction).toLong(),
                    (1 + 254 * fraction).toInt().coerceIn(1, 255)
                )
            } else {
                VibrationEffect.createOneShot(
                    (4 + 18 * fraction).toLong(),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            }
            vibrator.vibrate(effect)
        } catch (exception: Exception) {
            Timber.e(exception)
        }
    }
}

/**
 * Short "wheel" sounds for the carousel. They are generated once into small WAV files
 * (no sound assets in the app) and played with a SoundPool, which starts them instantly.
 */
@Singleton
class CarouselSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val soundPool: SoundPool by lazy {
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }

    private val soundIds: Map<CarouselSound, Int> by lazy {
        try {
            mapOf(
                CarouselSound.Tick to soundPool.load(
                    writeWav("carousel_tick.wav", frequency = 2300.0, durationMs = 14, decayMs = 3.0),
                    1
                ),
                CarouselSound.Click to soundPool.load(
                    writeWav("carousel_click.wav", frequency = 950.0, durationMs = 28, decayMs = 6.0),
                    1
                )
            )
        } catch (exception: Exception) {
            Timber.e(exception)
            emptyMap()
        }
    }

    /** Loads the sounds ahead of time, so the first tick is not late. */
    fun prepare() {
        soundIds
    }

    /**
     * @param volume 0..1 from the setting. The ticks are meant to be barely there, so the whole
     *  scale is 5× quieter: 50% plays at 0.1, 100% at 0.2.
     */
    fun play(sound: CarouselSound, volume: Float) {
        if (sound == CarouselSound.Off) return
        // Respect the phone's silent / vibrate mode
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val id = soundIds[sound] ?: return
        val v = volume.coerceIn(0f, 1f) * MAX_SOUND_VOLUME
        soundPool.play(id, v, v, 1, 0, 1f)
    }

    /** A sine "tick" with a very fast fade-out, as a 16-bit mono WAV in the app cache. */
    private fun writeWav(name: String, frequency: Double, durationMs: Int, decayMs: Double): String {
        val file = File(context.cacheDir, name)
        if (file.exists()) return file.absolutePath

        val sampleRate = 44_100
        val samples = sampleRate * durationMs / 1000
        val pcm = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.0005).coerceAtMost(1.0) // 0.5 ms, avoids a pop at the start
            val envelope = attack * exp(-t * 1000.0 / decayMs)
            val value = sin(2 * PI * frequency * t) * envelope * 0.9
            pcm.putShort((value * Short.MAX_VALUE).toInt().toShort())
        }

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples * 2); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(sampleRate); putInt(sampleRate * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(samples * 2)
        }
        FileOutputStream(file).use {
            it.write(header.array())
            it.write(pcm.array())
        }
        return file.absolutePath
    }
}
