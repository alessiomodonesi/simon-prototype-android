package it.unipd.dei.esp2526.simon.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyList
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.data.GameRepository
import it.unipd.dei.esp2526.simon.domain.GameEngine
import kotlinx.coroutines.flow.update

/**
 * funge da ponte tra l'interfaccia utente (UI) e il Repository, separando la logica visiva dai dati.
 * ereditando da "ViewModel", sopravvive nativamente ai cambi di configurazione (es. rotazione dello schermo),
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
    private val repository: GameRepository,
    private val savedStateHandle: SavedStateHandle // https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate
) : ViewModel() {
    companion object {
        private const val KEY_UI_STATE = "game_ui_state"
    }

    // stato reattivo per la partita in corso
    private val _uiState = MutableStateFlow(
        savedStateHandle.get<GameUiState>(KEY_UI_STATE) ?: GameUiState()
    )

    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // lavoro coroutine per tracciare la riproduzione del computer
    private var playbackJob: Job? = null

    /*
     * blocco di inizializzazione eseguito alla creazione del ViewModel.
     * gestisce il ripristino automatico in caso di Process Death: se il sistema operativo
     * aveva terminato l'app mentre il computer stava riproducendo una sequenza (e non era in pausa),
     * la coroutine viene riavviata dal punto esatto salvato nel SavedStateHandle.
     */
    init {
        val restoredState = _uiState.value
        if (restoredState.isGameRunning && restoredState.isComputerPlaying && !restoredState.isPaused && !restoredState.isGameOver)
            playComputerSequence()
    }

    /** funzione per il click su un colore, riceve come parametro l'etichetta (label) del colore premuto */
    fun colorClick(colorLabel: String) {
        val currentState = _uiState.value

        // ignora i click se: il gioco è finito || se NON è avviato || se tocca al computer
        if (currentState.isGameOver || !currentState.isGameRunning || currentState.isComputerPlaying)
            return

        // aggiunge la lettera alla sequenza utente
        val newUserSequence = currentState.userSequence + colorLabel
        updateState { it.copy(userSequence = newUserSequence) }

        // animazione del feedback visivo e uditivo dell'utente
        // chiama la funzione dentro GameAudioHelper.kt
        viewModelScope.launch {
            playColorFeedback(
                colorLabel = colorLabel,
                durationMs = 250,
                onColorActive = { active ->
                    // aggiorna lo StateFlow con il colore attivo
                    updateState { it.copy(activeColor = active) }
                }
            )
        }

        // indice per la validazione della mossa
        val i = newUserSequence.size - 1

        // verifica se l'indice esiste nella sequenza del computer e se il colore coincide
        if (i < currentState.computerSequence.size && colorLabel == currentState.computerSequence[i]) {  // mossa corretta
            if (newUserSequence.size == currentState.computerSequence.size) { // l'utente ha completato l'intera sequenza correttamente
                updateState {
                    it.copy(
                        userSequence = emptyList(), // reset della sequenza utente prima del turno del computer
                        computerPlaybackIndex = 0, // reset dell'indice
                        computerSequence = GameEngine.generateNextSequence(it.computerSequence), // aggiunge un colore
                        isComputerPlaying = true // ora è il turno del computer
                    )
                }
                playComputerSequence() // passa il turno al computer
            }
        } else { // mossa errata
            // ferma il gioco e disabilita ulteriori input
            updateState { it.copy(isGameOver = true, isGameRunning = false) }
        }
    }

    /** funzione per il click sul tasto "Start Game": avvia una nuova partita */
    fun startGame() {
        playbackJob?.cancel() // ferma eventuali riproduzioni precedenti
        updateState {
            GameUiState(
                isGameRunning = true,
                computerSequence = GameEngine.generateNextSequence(emptyList()), // genera la prima mossa
                isComputerPlaying = true
            )
        }
        playComputerSequence()
    }

    /** funzione per il click sul tasto "Pause/Resume" */
    fun togglePause() {
        val currentState = _uiState.value

        // invertiamo lo stato di isPaused
        val willBePaused = !currentState.isPaused
        updateState { it.copy(isPaused = willBePaused) }

        if (!willBePaused && currentState.isComputerPlaying) {
            // avvia la coroutine SOLO se non è già attiva (es. dopo ripristino da process death)
            if (playbackJob == null || playbackJob?.isActive == false)
                playComputerSequence()
        }
    }

    /**
     * mette in pausa il gioco in modo sicuro quando l'app va in background.
     * funzione chiamata nell'onStop() di GameActivity
     */
    fun pausePlayback() {
        val currentState = _uiState.value

        // se il gioco è in corso && il computer è in esecuzione && il computer non è in pausa
        if (currentState.isGameRunning && currentState.isComputerPlaying && !currentState.isPaused) {
            updateState { it.copy(isPaused = true) } // metto in pausa
            playbackJob?.cancel()
            updateState { it.copy(activeColor = null) }
        }
    }

    /** funzione per il click sul tasto "End Game" */
    fun endGame(onFinished: () -> Unit) {
        playbackJob?.cancel()
        val state = _uiState.value

        // se l'utente esce senza aver commesso un vero game over, valutiamo lo stato iniziale:
        // se il gioco non è partito, oppure se è in corso l'animazione della primissima sequenza
        // del computer (lunghezza <= 1), l'app si comporta come se la partita non fosse mai iniziata.
        if (!state.isGameOver && (!state.isGameRunning || (state.computerSequence.size <= 1 && state.isComputerPlaying))) {
            updateState { GameUiState() } // reset
            onFinished() // chiamo la callback
            return  // esce immediatamente senza eseguire il resto
        }

        // calcolo del record della partita (maxLength):
        // il punteggio è il numero di round completati correttamente.
        // se il gioco si interrompe durante il round K (computerSequence.size = K),
        // significa che il giocatore ha completato con successo K - 1 round in precedenza.
        val maxLength = (state.computerSequence.size - 1).coerceAtLeast(0)

        // calcolo dell'indice del colore errato nella sequenza finale:
        // se c'è un vero GameOver, il giocatore ha premuto un colore errato in coda a userSequence,
        // quindi le mosse corrette in questo round sono userSequence.size - 1.
        // se invcece esce premendo "Fine partita" o "Back" volontariamente, le mosse corrette sono esattamente userSequence.size.
        val errorIndex = if (state.isGameOver)
            (state.userSequence.size - 1).coerceAtLeast(0) else state.userSequence.size

        // costruzione della sequenza marchiando il colore errato con l'asterisco "*" prima del salvataggio nel DB
        val formattedSequence = state.computerSequence.mapIndexed { index, color ->
            if (index == errorIndex) "*$color" else color
        }.joinToString(", ")

        // inserimento asincrono nel DB chiamando la fun insertGame() qui sotto
        insertGame(
            maxLength = maxLength,
            sequence = formattedSequence
        )

        updateState { GameUiState() } // reset dello stato
        onFinished() // chiamo la callback
    }

    /** riproduzione sequenza computer tramite coroutine agganciata al ViewModel */
    private fun playComputerSequence() {
        // cancella eventuali job attivi rimasti per sicurezza concorrenziale
        playbackJob?.cancel()

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
                    updateState { it.copy(activeColor = active) }
                },

                // aggiorna l'indice di avanzamento nello StateFlow man mano che riproduce i toni
                onIndexUpdate = { index ->
                    updateState { it.copy(computerPlaybackIndex = index) }
                }
            )

            // quando la sequenza finisce regolarmente, restituisce il comando all'utente
            if (_uiState.value.computerPlaybackIndex >= _uiState.value.computerSequence.size) {
                updateState { it.copy(isComputerPlaying = false) }
            }
        }
    }

    /**
     * inserisce una nuova partita nel database in modalità "fire-and-forget".
     * regola fondamentale di Android: non bloccare mai il thread della UI.
     * per evitare blocchi, questa funzione "spawna" un thread aggiuntivo (worker thread)
     * passando l'esecuzione al Dispatchers.IO tramite le coroutine.
     * la coroutine è subordinata al ciclo di vita del ViewModel (`viewModelScope`) per prevenire memory leak.
     */
    private fun insertGame(maxLength: Int, sequence: String) {
        viewModelScope.launch {
            val newRecord = GameRecord(maxLength = maxLength, sequence = sequence)
            repository.insertGame(newRecord) // chiamo la fun insertGame() nel Repository
        }
    }

    /**
     * funzione di utilità per l'aggiornamento atomico dello stato.
     * centralizza la logica di mutazione garantendo che ogni singolo cambiamento
     * venga sincronizzato contemporaneamente nel flusso reattivo (_uiState)
     * e nella memoria di sistema (SavedStateHandle), prevenendo desincronizzazioni.
     */
    private fun updateState(update: (GameUiState) -> GameUiState) {
        _uiState.update { current ->
            val next = update(current)
            savedStateHandle[KEY_UI_STATE] = next
            next
        }
    }
}

/**
 * factory personalizzata per implementare la dependency injection del ViewModel.
 *
 * il delegato nativo "viewModels()" non sa come istanziare un ViewModel con parametri custom
 * (come "GameRepository" e "SavedStateHandle"). questa factory risolve il problema estraendo
 * nativamente il SavedStateHandle dalle CreationExtras di sistema e iniettando le dipendenze richieste.
 *
 * @param repository l'astrazione per l'accesso ai dati del database.
 */
class GameVMFactory(
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            val savedStateHandle = extras.createSavedStateHandle()
            return GameViewModel(repository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Classe ViewModel sconosciuta")
    }
}