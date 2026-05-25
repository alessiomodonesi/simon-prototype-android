package it.unipd.dei.esp2526.simon.domain

import it.unipd.dei.esp2526.simon.domain.model.simonColors
import it.unipd.dei.esp2526.simon.ui.game.playColorFeedback
import kotlinx.coroutines.delay

/**
 * motore logico del gioco.
 * un oggetto Singleton stateless che centralizza le funzioni core del Simon Game,
 * separando la logica di avanzamento (generazione ed esecuzione della sequenza del computer)
 * dai componenti puramente legati alla UI o ai dati.
 */
object GameEngine {
    /**
     * genera la sequenza successiva aggiungendo un colore casuale alla fine.
     * se la sequenza passata è vuota (1o turno), restituirà una lista di 1 elemento.
     */
    fun generateNextSequence(currentSequence: List<String>): List<String> {
        // aggiunge, alla sequenza attuale, un colore casuale estratto dalla lista simonColors.
        // overloading dell'operatore '+': crea una nuova lista allocando in memoria
        // currentSequence unita al nuovo elemento, preservando l'immutabilità dello stato.
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
            // se il gioco è in pausa, il ciclo si sospende e controlla ogni 100ms finché non viene tolta la pausa.
            // polling asincrono: cede il thread al dispatcher (non bloccante) e controlla ciclicamente il flag ogni 100ms senza saturare la CPU.
            while (isPaused()) delay(100)

            // chiama la fun playColorFeedback in GameUtils.kt
            playColorFeedback(
                colorLabel = sequence[i],
                durationMs = 250,
                onColorActive = onColorActive
            )

            // aggiorno l'indice al tono successivo solo dopo che è stato riprodotto correttamente
            onIndexUpdate(i + 1)

            // pausa tra un colore e l'altro
            delay(250)
        }
    }
}