package it.unipd.dei.esp2526.simon.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import it.unipd.dei.esp2526.simon.model.simonColors
import kotlin.math.sin

object SoundManager {
    // mappa per salvare le tracce audio pre-generate in memoria
    private val tracks = mutableMapOf<Double, AudioTrack>()

    /** da chiamare all'avvio dell'app o dell'Activity per pre-caricare i suoni */
    fun initialize(durationMs: Int = 500) {
        // estrae tutte le label dinamicamente dalla data class
        val labels = simonColors.map { it.label } + "DEFAULT"

        for (label in labels) {
            val freq = getFrequencyForColor(label)

            // genera la traccia solo se non è già presente nella mappa
            if (tracks[freq] == null)
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

        // generazione dell'onda (PCM audio)
        for (i in 0 until numSamples) {
            // calcolo della sinusoide per capire in che fase del periodo ci troviamo
            val sineValue = sin(2 * Math.PI * i / (sampleRate / frequency))

            // onda quadra: trasforma la curva in uno scalino netto (+1.0 o -1.0)
            var dVal = if (sineValue > 0) 1.0 else -1.0

            // riduce il volume al 30% (l'onda quadra "spacca" le orecchie se lasciata a 1.0)
            dVal *= 0.3

            // applica il fade in/out (anti-click)
            if (i < fadeSamples)
                dVal *= (i.toDouble() / fadeSamples)
            else if (i > numSamples - fadeSamples)
                dVal *= ((numSamples - i).toDouble() / fadeSamples)

            // conversione in 16-bit PCM
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
            "R" -> 261.63 // Do4 (C4)
            "G" -> 293.66 // Re4 (D4)
            "B" -> 329.63 // Mi4 (E4)
            "M" -> 392.00 // Sol4 (G4)
            "Y" -> 440.00 // La4 (A4)
            "C" -> 523.25 // Do5 (C5)
            else -> 440.00 // Default
        }
    }

    /** chiama questa funzione per rilasciare le risorse associate alle istanze di AudioTrack */
    fun release() {
        tracks.values.forEach { it.release() }
        tracks.clear()
    }
}