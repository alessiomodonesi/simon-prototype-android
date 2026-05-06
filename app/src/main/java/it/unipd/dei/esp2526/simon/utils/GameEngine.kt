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
        startIndex: Int = 0, // da dove partire, utile se si vuole riprodurre solo una parte della sequenza
        isPaused: () -> Boolean,
        onColorActive: (String?) -> Unit,
        onIndexUpdate: (Int) -> Unit // callback per aggiornare l'indice dal quale ripartire
    ) {
        for (i in startIndex until sequence.size) {
            // aggiorno subito l'indice al tono successivo (i + 1)
            onIndexUpdate(i + 1)

            // se il gioco è in pausa, il ciclo si sospende e controlla ogni 100ms finché non viene tolta la pausa
            while (isPaused()) delay(100)

            // chiama la fun playColorFeedback in GameUtils.kt
            playColorFeedback(
                colorLabel = sequence[i],
                durationMs = 250,
                onColorActive = onColorActive
            )

            // pausa tra un colore e l'altro
            delay(250)
        }
    }
}