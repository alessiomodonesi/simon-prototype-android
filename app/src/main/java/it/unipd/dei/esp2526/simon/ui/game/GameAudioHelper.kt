package it.unipd.dei.esp2526.simon.ui.game

import it.unipd.dei.esp2526.simon.core.audio.SoundManager
import kotlinx.coroutines.delay

/**
 * gestisce il feedback visivo e uditivo di un singolo colore.
 * centralizza la logica per evitare duplicazioni tra turno PC e click Utente.
 */
suspend fun playColorFeedback(
    colorLabel: String,
    durationMs: Long,
    onColorActive: (String?) -> Unit
) {
    onColorActive(colorLabel) // accende visivamente il colore

    // lancia la riproduzione audio
    val freq = SoundManager.getFrequencyForColor(colorLabel)
    SoundManager.playTone(frequency = freq)

    delay(durationMs) // mantiene acceso il colore per la durata del suono
    onColorActive(null) // spegne il colore
}