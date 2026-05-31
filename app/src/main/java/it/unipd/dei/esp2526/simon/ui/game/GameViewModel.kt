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
import it.unipd.dei.esp2526.simon.domain.MoveResult
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
    * intercetta e gestisce il ripristino automatico in caso di Process Death:
    * se il sistema operativo aveva terminato l'app mentre era il turno del computer,
    * il ViewModel ricarica lo stato dal SavedStateHandle e fa ripartire la sequenza
    * automaticamente dal punto esatto in cui si era interrotta.
    */
    init {
        val restoredState = _uiState.value
        if (restoredState.gameState == GameState.COMPUTER_TURN)
            playComputerSequence()
        else if (restoredState.gameState == GameState.WAITING_NEXT_ROUND) {
            // se ero in attesa del prossimo round
            updateState {
                it.copy(
                    gameState = GameState.COMPUTER_TURN, // ed inizio a riprodurre
                    userSequence = emptyList()
                )
            }
            playComputerSequence() // passo al turno del computer
        }
    }

    /** funzione per il click su un colore, riceve come parametro l'etichetta (label) del colore premuto */
    fun colorClick(colorLabel: String) {
        val currentState = _uiState.value

        // ignora i click se non è il turno del giocatore
        if (currentState.gameState != GameState.PLAYER_TURN)
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

        // passo la mossa al GameEngine e valuto l'enum restituita
        when (GameEngine.validateMove(newUserSequence, currentState.computerSequence)) {
            // mossa corretta, aspettiamo il prossimo click
            MoveResult.CORRECT_INCOMPLETE -> {}

            // l'utente ha completato l'intera sequenza correttamente
            MoveResult.ROUND_COMPLETED -> {
                // aggiunge un colore alla sequenza del computer per il prossimo round
                val nextComputerSequence =
                    GameEngine.generateNextSequence(currentState.computerSequence)

                updateState {
                    it.copy(
                        gameState = GameState.WAITING_NEXT_ROUND, // attiva lo stato di attesa temporaneo
                        computerPlaybackIndex = 0, // reset dell'indice
                        // lascio userSequence per visualizzarla nell'area di testo per 750ms
                        computerSequence = nextComputerSequence
                    )
                }

                viewModelScope.launch {
                    delay(750)
                    // prima di procedere, verifica che il gioco sia ancora in attesa del turno del computer
                    if (_uiState.value.gameState == GameState.WAITING_NEXT_ROUND) {
                        updateState {
                            it.copy(
                                gameState = GameState.COMPUTER_TURN, // ora è il turno del computer
                                userSequence = emptyList() // reset della sequenza utente
                            )
                        }
                        playComputerSequence() // passa il turno al computer
                    }
                }
            }

            // mossa errata
            MoveResult.WRONG -> {
                // ferma il gioco e disabilita ulteriori input
                updateState { it.copy(gameState = GameState.GAME_OVER) }
            }
        }
    }

    /** funzione per il click sul tasto "Start Game": avvia una nuova partita */
    fun startGame() {
        playbackJob?.cancel() // ferma eventuali riproduzioni precedenti
        updateState {
            GameUiState(
                gameState = GameState.COMPUTER_TURN,
                computerSequence = GameEngine.generateNextSequence(emptyList()) // genera la prima mossa
            )
        }
        playComputerSequence()
    }

    /** funzione per il click sul tasto "Pause/Resume" */
    fun togglePause() {
        val currentState = _uiState.value

        when (currentState.gameState) {
            GameState.COMPUTER_TURN -> {
                updateState { it.copy(gameState = GameState.COMPUTER_PAUSED) }
            }

            GameState.COMPUTER_PAUSED -> {
                updateState { it.copy(gameState = GameState.COMPUTER_TURN) }
                // avvia la coroutine SOLO se non è già attiva (es. dopo ripristino da process death)
                if (playbackJob == null || playbackJob?.isActive == false)
                    playComputerSequence()
            }

            else -> {} // ignora la pausa in altri stati
        }
    }

    /**
     * mette in pausa il gioco in modo sicuro quando l'app va in background.
     * funzione chiamata nell'onStop() di GameActivity
     */
    fun pausePlayback() {
        val currentState = _uiState.value

        // se il computer sta riproducendo la sequenza e non è già in pausa
        if (currentState.gameState == GameState.COMPUTER_TURN) {
            updateState { it.copy(gameState = GameState.COMPUTER_PAUSED, activeColor = null) }
            playbackJob?.cancel()
        } else if (currentState.gameState == GameState.WAITING_NEXT_ROUND) {
            // se l'app va in background durante l'attesa del turno del computer,
            // mette in pausa direttamente in COMPUTER_PAUSED
            updateState {
                it.copy(
                    gameState = GameState.COMPUTER_PAUSED,
                    userSequence = emptyList(), // pulisco la sequenza utente per quando riprenderà
                    activeColor = null
                )
            }
            playbackJob?.cancel()
        }
    }

    /** funzione per il click sul tasto "End Game" */
    fun endGame(onFinished: () -> Unit) {
        playbackJob?.cancel()
        val state = _uiState.value

        // valuto se sono nello stato iniziale o se l'utente esce all'inizio della prima sequenza
        val isInitialState = state.gameState == GameState.IDLE ||
                (state.computerSequence.size <= 1 && state.gameState == GameState.COMPUTER_TURN)

        if (state.gameState != GameState.GAME_OVER && isInitialState) {
            updateState { GameUiState() } // reset dello stato a IDLE
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
        val errorIndex = if (state.gameState == GameState.GAME_OVER)
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

            /*
             * inserisce un buffer di 1 secondo per dare all'utente il tempo di prepararsi all'inizio di un nuovo round.
             * viene bypassato se computerPlaybackIndex > 0 per garantire una ripresa istantanea dopo un ripristino da Process Death o una pausa.
             */
            if (state.computerPlaybackIndex == 0) delay(1000)

            // chiamata diretta all'oggetto Singleton GameEngine
            GameEngine.playComputerSequence(
                sequence = state.computerSequence,
                startIndex = state.computerPlaybackIndex,
                // verifica lo stato di pausa leggendo dallo StateFlow in tempo reale
                isPaused = {
                    _uiState.value.gameState == GameState.COMPUTER_PAUSED
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
            if (_uiState.value.computerPlaybackIndex >= _uiState.value.computerSequence.size)
                updateState { it.copy(gameState = GameState.PLAYER_TURN) }
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
        // inizializza una coroutine sul Dispatchers.IO,
        // subordinata al ciclo di vita del ViewModel per prevenire memory leak
        viewModelScope.launch(Dispatchers.IO) {
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