package it.unipd.dei.esp2526.simon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.unipd.dei.esp2526.simon.data.AppDatabase
import it.unipd.dei.esp2526.simon.data.GameRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Il ViewModel aggiunge un fondamentale livello di astrazione, fungendo da ponte
 * tra la UI (Activity/Screen) e il livello di accesso ai dati (DAO di Room),
 * mantenendo il codice pulito e separando le responsabilità.
 *
 * È progettato nativamente in Android per sopravvivere ai cambi di configurazione
 * (come la distruzione e ricreazione dell'Activity durante la rotazione dello schermo),
 * risultando molto più robusto del solo `rememberSaveable` per la gestione di dati complessi.
 * Si occupa inoltre di esporre i dati persistenti alla UI non appena l'app viene riaperta.
 *
 * Gestione reattiva dei dati (Room + Flow):
 * Interrogando il database Room che restituisce un `Flow`, otteniamo un canale di dati
 * reattivo. Qualsiasi modifica alla tabella (es. un nuovo inserimento) genera automaticamente
 * un'emissione aggiornata della lista, permettendo alla UI di aggiornarsi in tempo reale
 * senza ulteriori query manuali.
 *
 * Ottimizzazione delle risorse (StateFlow & WhileSubscribed):
 * Il `Flow` viene convertito in uno `StateFlow` tramite `.stateIn()`.
 * Utilizzando `SharingStarted.WhileSubscribed(5000L)`, ottimizziamo pesantemente
 * l'uso delle risorse del dispositivo. La connessione al database e l'emissione di dati
 * avvengono *solo* se c'è almeno un "osservatore" attivo (es. l'interfaccia utente visibile).
 * Il ritardo di 5 secondi (5000L) serve come "cuscinetto" (buffer): garantisce che il
 * flusso non venga cancellato e ricreato inutilmente durante i rapidi cambi di configurazione
 * (es. rotazione del display), ma permette di interrompere l'operazione in background
 * se l'utente riduce l'app a icona per più di 5 secondi, risparmiando batteria e memoria.
 *
 * https://developer.android.com/topic/libraries/architecture/viewmodel
 * https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
 */
class ViewModel(application: Application) : AndroidViewModel(application) {
    // utilizzo il singleton getDatabase() invece di chiamare Room.databaseBuilder qui
    private val dao = AppDatabase.getDatabase(application).gameDao()

    // crea lo StateFlow direttamente dalla query di Room
    // stato reattivo che contiene la cronologia per la HistoryActivity
    val history: StateFlow<List<GameRecord>> = dao.getAllGames()
        .stateIn(
            scope = viewModelScope,
            // interroga il DB solo se c'è almeno un'Activity in ascolto
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // inserisce una nuova partita e aggiorna la lista
    // Dispatchers.IO indicates that this coroutine should be executed on a thread reserved for I/O operations.
    fun insertGame(maxLength: Int, sequence: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newRecord = GameRecord(maxLength = maxLength, sequence = sequence)
            dao.insertGame(newRecord) // chiamo la fun insertGame() nel Dao
        }
    }

    // chiamo la fun getGameById() nel Dao
    suspend fun getGameById(id: Int): GameRecord? {
        return dao.getGameById(id)
    }
}