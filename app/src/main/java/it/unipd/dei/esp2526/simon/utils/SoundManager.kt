package it.unipd.dei.esp2526.simon.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

object SoundManager {
    /**
     * riproduce un tono di durata e frequenza specificate utilizzando AudioTrack.
     * come indicato nelle slide, AudioTrack è utilizzato per il playback di PCM audio verso memory buffers.
     * il metodo viene eseguito in un thread separato per non bloccare il thread principale (UI).
     */
    suspend fun playTone(frequency: Double, durationMs: Int) = withContext(Dispatchers.IO) {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate / 1000.0).toInt()
        val generatedSnd = ByteArray(2 * numSamples)

        // fade-in e fade-out per ammorbidire l'attacco e il rilascio del suono
        val fadeDurationMs = 10
        val fadeSamples = (fadeDurationMs * sampleRate / 1000.0).toInt()

        // generazione dell'onda sinusoidale (PCM audio)
        for (i in 0 until numSamples) {
            var dVal = sin(2 * Math.PI * i / (sampleRate / frequency))

            if (i < fadeSamples)
                dVal *= (i.toDouble() / fadeSamples)
            else if (i > numSamples - fadeSamples)
                dVal *= ((numSamples - i).toDouble() / fadeSamples)

            val valShort = (dVal * 32767).toInt().toShort()
            generatedSnd[i * 2] = (valShort.toInt() and 0x00ff).toByte()
            generatedSnd[i * 2 + 1] = (valShort.toInt() and 0xff00 ushr 8).toByte()
        }

        // inizializzazione di AudioTrack
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(generatedSnd.size)

            // utilizzo la static mode per garantire la latenza più bassa possibile (low-latency playback),
            // ideale per suoni brevi che entrano interamente nel buffer
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        // scrive i dati audio nel memory buffer
        audioTrack.write(generatedSnd, 0, generatedSnd.size)

        // avvia il playback
        audioTrack.play()

        // attesa del completamento del suono
        Thread.sleep(durationMs.toLong())

        // rilascio delle risorse associate all'istanza
        audioTrack.release()
    }

    /** associa ad ogni etichetta colore una frequenza in Hz */
    fun getFrequencyForColor(label: String): Double {
        return when (label) {
            "R" -> 310.0 // Re#4
            "G" -> 415.0 // Sol#4
            "B" -> 209.0 // Sol#3
            "M" -> 466.0 // La#4
            "Y" -> 252.0 // Si3
            "C" -> 155.0 // Re#3
            else -> 440.0 // Default (La4)
        }
    }
}