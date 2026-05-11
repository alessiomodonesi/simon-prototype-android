package it.unipd.dei.esp2526.simon.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

object SoundManager {
    // mappa per salvare le tracce audio pre-generate in memoria
    private val tracks = mutableMapOf<Double, AudioTrack>()

    /** da chiamare all'avvio dell'app o dell'Activity per pre-caricare i suoni */
    fun initialize(durationMs: Int = 500) {
        val frequencies = listOf(310.0, 415.0, 209.0, 466.0, 252.0, 155.0, 440.0)
        for (freq in frequencies) {
            tracks[freq] = generateToneTrack(freq, durationMs)
        }
    }

    /**
     * riproduce un tono di durata e frequenza specificate utilizzando AudioTrack.
     * come indicato nelle slide, AudioTrack è utilizzato per il playback di PCM audio verso memory buffers.
     * il metodo viene eseguito in un thread separato per non bloccare il thread principale (UI).
     */
    private fun generateToneTrack(frequency: Double, durationMs: Int): AudioTrack {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate / 1000.0).toInt()
        val generatedSnd = ByteArray(2 * numSamples)
        val fadeSamples = (10 * sampleRate / 1000.0).toInt()

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

        /* * INIZIALIZZAZIONE AUDIOTRACK
         * la classe AudioTrack fa parte della famiglia di package android.media
         * consente esclusivamente la riproduzione (playback only) di audio in formato PCM,
         * leggendo direttamente da un memory buffer.
         * è la scelta ideale per questo gioco in quanto offre un playback a bassa latenza (low-latency).
         */
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

            /*
             * MODALITÀ STATICA
             * utilizziamo la Static mode, la quale garantisce la latenza più bassa possibile.
             * questa modalità richiede che il suono entri interamente all'interno del memory buffer,
             * il che è perfetto per i brevi toni generati dal Simon.
             */
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        // il metodo write scrive i dati audio all'interno del memory buffer
        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        return audioTrack
    }

    /** riproduce il suono in modo istantaneo */
    fun playTone(frequency: Double) {
        val track = tracks[frequency] ?: return

        // se il suono è già in riproduzione, lo ferma
        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
            // il metodo stop attende che il contenuto del buffer di memoria venga consumato completamente per poi fermarsi
            track.stop()
        }

        // riavvolge il buffer di memoria all'inizio per poterlo riutilizzare
        track.reloadStaticData()

        // il metodo play avvia la riproduzione
        track.play()
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

    /** chiama questa funzione per rilasciare le risorse associate alle istanze di AudioTrack */
    fun release() {
        tracks.values.forEach { it.release() }
        tracks.clear()
    }
}