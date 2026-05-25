package it.unipd.dei.esp2526.simon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.emptyList
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.data.GameRepository
import it.unipd.dei.esp2526.simon.model.GameUiState
import it.unipd.dei.esp2526.simon.utils.GameEngine
import it.unipd.dei.esp2526.simon.utils.playColorFeedback

/**
 * funge da ponte tra l'interfaccia utente (UI) e il Repository, separando la logica visiva dai dati.
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
class GameViewModel(
    application: Application,
    private val repository: GameRepository
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(GameUiState()) // stato reattivo per la partita in corso
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()
    private var playbackJob: Job? =
        null // lavoro coroutine per tracciare la riproduzione del computer

    // stato reattivo che contiene la cronologia per la HistoryActivity (aggancio lo StateFlow al Flow del Repository)
    val history: StateFlow<List<GameRecord>> = repository.allGames
        .stateIn(
            scope = viewModelScope, // dice al flusso di vivere esattamente finché vive il ViewModel
            started = SharingStarted.WhileSubscribed(5000L), // ottimizzazione per la batteria
            initialValue = emptyList() // StateFlow ha sempre un valore!
        )

    /** funzione per il click su un colore, riceve come parametro l'etichetta (label) del colore premuto */
    fun colorClick(colorLabel: String) {
        val currentState = _uiState.value

        // ignora i click se: il gioco è finito || se NON è avviato || se tocca al computer
        if (currentState.isGameOver || !currentState.isGameRunning || currentState.isComputerPlaying)
            return

        // aggiunge la lettera alla sequenza utente
        val newUserSequence = currentState.userSequence + colorLabel
        _uiState.update { it.copy(userSequence = newUserSequence) }

        // animazione del feedback visivo e uditivo dell'utente
        // chiama la funzione dentro GameUtils.kt
        viewModelScope.launch {
            playColorFeedback(
                colorLabel = colorLabel,
                durationMs = 250,
                onColorActive = { active ->
                    // aggiorna lo StateFlow con il colore attivo
                    _uiState.update { it.copy(activeColor = active) }
                }
            )
        }

        // indice per la validazione della mossa
        val i = newUserSequence.size - 1

        // verifica se l'indice esiste nella sequenza del computer e se il colore coincide
        if (i < currentState.computerSequence.size && colorLabel == currentState.computerSequence[i]) {  // mossa corretta
            if (newUserSequence.size == currentState.computerSequence.size) { // l'utente ha completato l'intera sequenza correttamente
                _uiState.update {
                    it.copy(
                        userSequence = emptyList(), // reset della sequenza utente prima del turno del computer
                        computerPlaybackIndex = 0, // reset dell'indice
                        computerSequence = GameEngine.generateNextSequence(it.computerSequence), // aggiunge un colore
                        isComputerPlaying = true // ora è il turno al computer
                    )
                }
                playComputerSequence() // passa il turno al computer
            }
        } else { // mossa errata
            // ferma il gioco e disabilita ulteriori input
            _uiState.update { it.copy(isGameOver = true, isGameRunning = false) }
        }
    }

    /** funzione per il click sul tasto "Start Game": avvia una nuova partita */
    fun startGame() {
        playbackJob?.cancel() // ferma eventuali riproduzioni precedenti
        _uiState.value = GameUiState(
            isGameRunning = true,
            computerSequence = GameEngine.generateNextSequence(emptyList()), // genera la prima mossa
            isComputerPlaying = true
        )
        playComputerSequence()
    }

    /** funzione per il click sul tasto "Pause/Resume" */
    fun togglePause() {
        // inverte lo stato di pausa ad ogni click
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    /** funzione per il click sul tasto "End Game" */
    fun endGame(onFinished: () -> Unit) {
        playbackJob?.cancel()
        val state = _uiState.value

        // se non c'è stato un vero game over && (il gioco non è partito || siamo al 1o turno),
        // l'app si comporta come se non fosse mai iniziata e non salva nulla
        if (!state.isGameOver && (!state.isGameRunning || state.computerSequence.size <= 1)) {
            _uiState.value = GameUiState() // reset
            onFinished() // chiamo la callback
            return  // esce immediatamente senza eseguire il resto
        }

        // il punteggio è la lunghezza dell'ultimo round completato correttamente.
        // se l'utente sbaglia, il round corrente (computerSequence.size) è fallito,
        // quindi il punteggio è la lunghezza della sequenza al turno precedente.
        val maxLength = (state.computerSequence.size - 1).coerceAtLeast(0)

        // inserimento asincrono nel DB chiamando la fun insertGame() qui sotto
        insertGame(
            maxLength = maxLength,
            // la sequenza da salvare è quella COMPLETA proposta dal computer in questo turno
            sequence = state.computerSequence.joinToString(", ")
        )

        _uiState.value = GameUiState() // reset dello stato
        onFinished() // chiamo la callback
    }

    /** riproduzione sequenza computer tramite coroutine agganciata al ViewModel */
    private fun playComputerSequence() {
        playbackJob = viewModelScope.launch {
            val state = _uiState.value

            // se parto da zero = non è un ripristino da rotazione, facciamo una pausa iniziale
            if (state.computerPlaybackIndex == 0) delay(1000)

            // chiamata diretta all'oggetto Singleton GameEngine
            GameEngine.playComputerSequence(
                sequence = state.computerSequence,
                startIndex = state.computerPlaybackIndex,
                // verifica lo stato di pausa leggendo dallo StateFlow in tempo reale
                isPaused = {
                    _uiState.value.isPaused
                },

                // accende/spegne il colore aggiornando lo StateFlow
                onColorActive = { active ->
                    _uiState.update { it.copy(activeColor = active) }
                },

                // aggiorna l'indice di avanzamento nello StateFlow man mano che riproduce i toni
                onIndexUpdate = { index ->
                    _uiState.update { it.copy(computerPlaybackIndex = index) }
                }
            )


            // quando la sequenza finisce regolarmente, restituisce il comando all'utente
            if (_uiState.value.computerPlaybackIndex >= _uiState.value.computerSequence.size) {
                _uiState.update { it.copy(isComputerPlaying = false) }
            }
        }
    }

    /** inserisce una nuova partita: "fire-and-forget" -> la UI non aspetta il risultato */
    private fun insertGame(maxLength: Int, sequence: String) {
        // inizializza una coroutine sul Dispatchers.IO,
        // subordinata al ciclo di vita del ViewModel per prevenire memory leak
        viewModelScope.launch(Dispatchers.IO) {
            val newRecord = GameRecord(maxLength = maxLength, sequence = sequence)
            repository.insertGame(newRecord) // chiamo la fun insertGame() nel Repository
        }
    }

    /**
     * recupera una partita:
     * "suspend" -> la UI deve chiamarla da una coroutine (es. LaunchedEffect) e aspettare il valore.
     * inoltre Room è Main-safe di default: sposterà autonomamente l'esecuzione di questa suspend function su un thread di I/O
     */
    suspend fun getGameById(id: Int): GameRecord? {
        return repository.getGameById(id)
    }
}

/**
 * factory personalizzata per implementare la dependency injection manuale del ViewModel.
 *
 * poiché il delegato nativo "viewModels()" non sa come passare parametri custom al costruttore
 * (come "Application" e "GameRepository"), questa factory si occupa di istanziare
 * correttamente il "GameViewModel" fornendogli le dipendenze necessarie.
 *
 * @param application il context globale dell'applicazione necessario per Room.
 * @param repository l'astrazione per l'accesso ai dati del database.
 */
class GameViewModelFactory(
    private val application: Application,
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}