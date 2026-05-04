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
 * aggiunge un ulteriore livello di astrazione necessario,
 * fungendo da ponte tra la UI (Activity/Screen) e il DAO di Room, mantenendo il codice pulito.
 * è progettato nativamente in Android per sopravvivere alla distruzione e ricreazione dell'Activity,
 * risultando più robusto del solo rememberSaveable per dati complessi.
 * si occupa di esporre i dati persistenti alla UI non appena l'app viene riaperta.
 * https://developer.android.com/topic/libraries/architecture/viewmodel
 */
class ViewModel(application: Application) : AndroidViewModel(application) {
    // utilizzo il singleton getDatabase() invece di chiamare Room.databaseBuilder qui
    private val dao = AppDatabase.getDatabase(application).gameDao()

    // crea lo StateFlow direttamente dalla query di Room
    // stato reattivo che contiene la cronologia per la HistoryActivity
    // https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
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