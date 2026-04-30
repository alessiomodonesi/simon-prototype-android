package it.unipd.dei.esp2526.simon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import it.unipd.dei.esp2526.simon.data.AppDatabase
import it.unipd.dei.esp2526.simon.data.GameRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * aggiunge un ulteriore livello di astrazione necessario,
 * fungendo da ponte tra la UI (Activity/Screen) e il DAO di Room, mantenendo il codice pulito.
 * è progettato nativamente in Android per sopravvivere alla distruzione e ricreazione dell'Activity,
 * risultando più robusto del solo rememberSaveable per dati complessi.
 * si occupa di esporre i dati persistenti alla UI non appena l'app viene riaperta.
 * https://developer.android.com/topic/libraries/architecture/viewmodel
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    // utilizzo il singleton getDatabase() invece di chiamare Room.databaseBuilder qui
    private val dao = AppDatabase.getDatabase(application).gameDao()

    // stato reattivo che contiene la cronologia per la HistoryActivity
    // https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
    private val _history = MutableStateFlow<List<GameRecord>>(emptyList())
    val history: StateFlow<List<GameRecord>> = _history.asStateFlow()

    // appena il ViewModel viene creato, carica lo storico dal DB
    init {
        loadHistory()
    }

    // recupera i dati usando un thread secondario (Dispatchers.IO)
    // Dispatchers.IO indicates that this coroutine should be executed on a thread reserved for I/O operations.
    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _history.value = dao.getAllGames() // chiamo la fun getAllGames() nel Dao
        }
    }

    // inserisce una nuova partita e aggiorna la lista
    fun insertGame(maxLength: Int, sequence: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newRecord = GameRecord(maxLength = maxLength, sequence = sequence)
            dao.insertGame(newRecord) // chiamo la fun insertGame() nel Dao
            loadHistory() // poi aggiorno la lista
        }
    }
}