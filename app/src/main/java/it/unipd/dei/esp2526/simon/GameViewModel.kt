package it.unipd.dei.esp2526.simon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unipd.dei.esp2526.simon.data.AppDatabase
import it.unipd.dei.esp2526.simon.data.GameRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * funge da ponte tra l'interfaccia utente (UI) e il database (DAO), separando la logica visiva dai dati.
 * ereditando da "AndroidViewModel", ottiene il context globale ("Application") necessario per inizializzare Room.
 * inoltre, sopravvive nativamente ai cambi di configurazione (es. rotazione dello schermo),
 * proteggendo i dati complessi in modo più robusto rispetto a `rememberSaveable`.
 *
 * gestione reattiva (Room + Flow -> StateFlow):
 * Room restituisce un "Flow" (un "tubo" asincrono e freddo) che emette aggiornamenti ad ogni modifica
 * della tabella. tramite l'operatore `.stateIn()`, converto questo flusso in uno "StateFlow" ("caldo").
 * questo funge da serbatoio di stato: memorizza sempre l'ultimo valore noto, permettendo a Compose
 * di ridisegnare la UI istantaneamente non appena i dati cambiano, senza query manuali.
 *
 * ottimizzazione delle risorse (WhileSubscribed):
 * grazie a "SharingStarted.WhileSubscribed(5000L)", la connessione reattiva al database si attiva solo
 * se la UI è visibile (in ascolto). il buffer di 5 secondi (5000L) impedisce la distruzione del flusso
 * durante i rapidi cambi di orientamento del display, ma lo spegne se l'app rimane in background,
 * risparmiando drasticamente batteria e memoria.
 *
 * @see "https://developer.android.com/topic/libraries/architecture/viewmodel"
 * @see "https://developer.android.com/kotlin/flow/stateflow-and-sharedflow"
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    // utilizzo il singleton getDatabase() invece di chiamare Room.databaseBuilder qui
    private val dao = AppDatabase.getDatabase(application).gameDao()

    // crea lo StateFlow direttamente dalla query di Room.
    // stato reattivo che contiene la cronologia per la HistoryActivity
    val history: StateFlow<List<GameRecord>> = dao.getAllGames()
        .stateIn(
            scope = viewModelScope, // dice al flusso di vivere esattamente finché vive il ViewModel
            started = SharingStarted.WhileSubscribed(5000L), // ottimizzazione per la batteria
            initialValue = emptyList() // StateFlow ha sempre un valore!
        )

    // inserisce una nuova partita:
    // "fire-and-forget" -> la UI non aspetta il risultato
    fun insertGame(maxLength: Int, sequence: String) {
        // inizializza una coroutine sul Dispatchers.Main,
        // subordinata al ciclo di vita del ViewModel per prevenire memory leak
        viewModelScope.launch {
            val newRecord = GameRecord(maxLength = maxLength, sequence = sequence)
            dao.insertGame(newRecord) // chiamo la fun insertGame() nel Dao
        }
    }

    // recupera una partita:
    // "suspend" -> la UI deve chiamarla da una coroutine (es. LaunchedEffect) e aspettare il valore.
    // inoltre Room è Main-safe di default: sposterà autonomamente l'esecuzione di questa suspend function su un thread di I/O
    suspend fun getGameById(id: Int): GameRecord? {
        return dao.getGameById(id)
    }
}