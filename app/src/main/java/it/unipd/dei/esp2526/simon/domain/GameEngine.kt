package it.unipd.dei.esp2526.simon.domain

import it.unipd.dei.esp2526.simon.domain.model.simonColors
import it.unipd.dei.esp2526.simon.ui.game.playColorFeedback
import kotlinx.coroutines.delay

/**
 * enum che modella l'esito di una singola mossa.
 * Permette di isolare le regole del Simon Game dal ViewModel,
 * che riceverà solo il verdetto finale senza dover manipolare gli indici degli array.
 */
enum class MoveResult {
    CORRECT_INCOMPLETE, // colore giusto, ma mancano altri colori per finire il round
    ROUND_COMPLETED,    // colore giusto e l'utente ha completato tutta la sequenza
    WRONG               // l'utente ha premuto il colore sbagliato
}

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
        isPaused: () -> Boolean, // callback per verificare se il gioco è in pausa, utile per interrompere la riproduzione se necessario
        onColorActive: (String?) -> Unit,
        onIndexUpdate: (Int) -> Unit // callback per aggiornare l'indice dal quale ripartire
    ) {
        for (i in startIndex until sequence.size) {
            // se il gioco è in pausa, il ciclo si sospende e controlla ogni 100ms finché non viene tolta la pausa.
            // polling asincrono: cede il thread al dispatcher (non bloccante) e controlla ciclicamente il flag ogni 100ms senza saturare la CPU.
            while (isPaused()) delay(100)

            // chiama la fun playColorFeedback in GameAudioHelper.kt
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

    /** valuta se la mossa dell'utente è corretta */
    fun validateMove(
        newUserSequence: List<String>,
        computerSequence: List<String>
    ): MoveResult {
        // indice per la validazione della mossa
        val i = newUserSequence.size - 1
        val colorLabel = newUserSequence.last()

        // verifica se l'indice esiste nella sequenza del computer e se il colore coincide
        if (i < computerSequence.size && colorLabel == computerSequence[i]) { // mossa corretta
            // coincide, ha anche finito l'intera sequenza?
            return if (newUserSequence.size == computerSequence.size)
                MoveResult.ROUND_COMPLETED // l'utente ha completato l'intera sequenza correttamente
            else
                MoveResult.CORRECT_INCOMPLETE // l'utente ha ancora colori da completare
        }

        // mossa errata
        return MoveResult.WRONG
    }
}