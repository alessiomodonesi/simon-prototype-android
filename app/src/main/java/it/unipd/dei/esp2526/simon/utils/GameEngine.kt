package it.unipd.dei.esp2526.simon.utils

import it.unipd.dei.esp2526.simon.model.simonColors
import kotlinx.coroutines.delay

object GameEngine {
    /**
     * genera la sequenza successiva aggiungendo un colore casuale alla fine.
     * se la sequenza passata è vuota (1o turno), restituirà una lista di 1 elemento.
     */
    fun generateNextSequence(currentSequence: List<String>): List<String> {
        // aggiunge, alla sequenza attuale, un colore casuale estratto dalla lista simonColors
        return currentSequence + simonColors.map { it.label }.random()
    }

    /**
     * riproduce la sequenza generata dal computer.
     * "suspend": non blocca il thread principale.
     */
    suspend fun playComputerSequence(
        sequence: List<String>,
        isPaused: () -> Boolean,
        onColorActive: (String?) -> Unit
    ) {
        delay(500) // pausa iniziale

        for (color in sequence) {
            // se il gioco è in pausa, il ciclo si sospende e controlla ogni 100ms finché non viene tolta la pausa
            while (isPaused()) delay(100)

            // chiama la fun playColorFeedback in GameUtils.kt
            playColorFeedback(colorLabel = color, durationMs = 500, onColorActive = onColorActive)
            delay(250) // pausa tra un colore e l'altro
        }
    }
}