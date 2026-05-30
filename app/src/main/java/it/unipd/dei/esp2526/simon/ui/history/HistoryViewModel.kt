package it.unipd.dei.esp2526.simon.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlin.collections.emptyList
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.data.GameRepository

/**
 * funge da ponte tra l'interfaccia di visualizzazione della cronologia (HistoryActivity) e il Repository.
 * gestisce l'osservazione asincrona e reattiva delle partite memorizzate nel database SQL.
 * Gestione reattiva ottimizzata (Flow -> StateFlow):
 * trasforma il flusso freddo del database in uno StateFlow "caldo" agganciato al ciclo di vita (viewModelScope).
 * l'uso dell'operatore "SharingStarted.WhileSubscribed(5000L)" ottimizza il consumo energetico:
 * mantiene attiva la connessione al DB solo se la UI è in ascolto, tollerando brevi interruzioni dovute alla rotazione.
 */
class HistoryViewModel(
    repository: GameRepository,
) : ViewModel() {

    /*
    * stato reattivo pubblico che contiene l'intera cronologia ordinata delle partite salvate.
    * Room monitora in modo continuo le modifiche alla tabella "games_history" ed emette
    * nuovi dati in modo asincrono sul thread di I/O, evitando di rallentare o bloccare la UI.
    */
    val uiState: StateFlow<List<GameRecord>> = repository.allGames
        .stateIn(
            scope = viewModelScope,  // vincola la vita del flusso al ciclo di vita del ViewModel
            started = SharingStarted.WhileSubscribed(5000L), // ottimizzazione della batteria in background
            initialValue = emptyList() // valore iniziale vuoto, obbligatorio per StateFlow
        )
}


/**
 * factory personalizzata per implementare la dependency injection di HistoryViewModel.
 * consente al delegato "viewModels()" dell'Activity di istanziare correttamente il ViewModel.
 */
class HistoryVMFactory(
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            return HistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}