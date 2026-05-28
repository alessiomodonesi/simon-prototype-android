package it.unipd.dei.esp2526.simon.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.data.GameRepository

/**
 * funge da ponte tra l'interfaccia utente di dettaglio (DetailActivity) e il Repository.
 * gestisce il caricamento asincrono e la persistenza dello stato di una singola partita conclusa.
 * Gestione dello Stato e Process Death:
 * sfrutta "SavedStateHandle" per recuperare in modo automatico e sicuro l'ID della partita
 * passato tramite Intent, garantendo che lo stato sopravviva in caso di Process Death.
 * lo stato è esposto reattivamente alla UI tramite uno "StateFlow" per consentire composizioni pulite.
 */
class DetailViewModel(
    private val repository: GameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    // recupera l'ID passato come parametro extra nell'Intent.
    // in caso di Process Death, SavedStateHandle lo conserva automaticamente
    private val matchId: Int = savedStateHandle[DetailActivity.EXTRA_MATCH_ID] ?: -1

    // stato reattivo privato mutabile contenente il record della partita (inizialmente null)
    private val _uiState = MutableStateFlow<GameRecord?>(null)

    // flusso pubblico immutabile esposto alla composizione
    val uiState: StateFlow<GameRecord?> = _uiState.asStateFlow()

    /*
     * blocco di inizializzazione eseguito alla creazione del ViewModel.
     * gestisce il ripristino automatico in caso di Process Death: se il sistema operativo
     * aveva terminato l'app mentre il computer stava riproducendo una sequenza (e non era in pausa),
     * la coroutine viene riavviata dal punto esatto salvato nel SavedStateHandle.
     */
    init {
        loadGameRecord()
    }


    /**
     * esegue il recupero asincrono della partita dal database Room tramite il Repository.
     * trattandosi di un'operazione di I/O su database, viene eseguita all'interno di una coroutine
     * legata al ciclo di vita del ViewModel (viewModelScope) per prevenire memory leak.
     */
    private fun loadGameRecord() {
        if (matchId != -1) {
            viewModelScope.launch {
                // Room garantisce l'esecuzione sicura sul thread Dispatchers.IO in modalità main-safe
                _uiState.value = repository.getGameById(matchId)
            }
        }
    }
}

/**
 * factory personalizzata per creare istanze di DetailViewModel iniettando le dipendenze richieste.
 * estrae autonomamente il SavedStateHandle dalle CreationExtras fornite dal sistema operativo.
 */
class DetailVMFactory(
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            return DetailViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}