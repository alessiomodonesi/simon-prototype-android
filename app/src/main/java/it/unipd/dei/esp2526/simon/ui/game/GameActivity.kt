package it.unipd.dei.esp2526.simon.ui.game

import android.content.res.Configuration
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.unipd.dei.esp2526.simon.R
import it.unipd.dei.esp2526.simon.core.audio.SoundManager
import it.unipd.dei.esp2526.simon.domain.model.simonColors
import it.unipd.dei.esp2526.simon.data.*

import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme

class GameActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels { // inizializzo il View Model
        val database =
            AppDatabase.getDatabase(this.applicationContext) // utilizzo il singleton getDatabase() invece di chiamare Room.databaseBuilder
        val repository =
            GameRepository(database.gameDao()) // inizializzo il Repository con il DAO
        GameVMFactory(repository) // chiamo il costruttore
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inizializzo i suoni in memoria
        SoundManager.initialize(durationMs = 250)

        setContent {
            SimonTheme {
                // iscrizione reattiva allo UI State del ViewModel
                val state by vm.uiState.collectAsStateWithLifecycle()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // grandezza del cutout
                    val layoutDirection = LocalLayoutDirection.current
                    val cutout = WindowInsets.displayCutout.asPaddingValues()

                    // valore massimo tra lato destro e sinistro
                    val symmetricCutout = max(
                        cutout.calculateLeftPadding(layoutDirection),
                        cutout.calculateRightPadding(layoutDirection)
                    )

                    GameScreen(
                        // semplice lettura dello stato centralizzato
                        userSequence = state.userSequence,
                        isGameRunning = (state.gameState != GameState.IDLE) && (state.gameState != GameState.GAME_OVER),
                        isComputerPlaying = (state.gameState == GameState.COMPUTER_TURN) || (state.gameState == GameState.PAUSED),
                        isPaused = (state.gameState == GameState.PAUSED),
                        isGameOver = (state.gameState == GameState.GAME_OVER),
                        activeColor = state.activeColor,

                        // inoltro delle interazioni dell'utente al ViewModel
                        onColorClick = { label -> vm.colorClick(label) },
                        onStartClick = { vm.startGame() },
                        onPauseClick = { vm.togglePause() },
                        onEndGameClick = {
                            vm.endGame {
                                // utilizzo finish() per chiudere GameActivity (pop) e tornare indietro
                                this@GameActivity.finish()
                            }
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = symmetricCutout)
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // mette in pause il gioco se l'activity perde il foreground, ma NON se sta ruotando lo schermo
        if (!isChangingConfigurations)
            vm.pausePlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

/** schermata principale di gioco che gestisce la griglia dei colori, l'output visivo e i controlli */
@Composable
fun GameScreen(
    // variabili di stato
    userSequence: List<String>, // lista dei colori attualmente premuti dall'utente
    isGameRunning: Boolean, // stato che indica se la partita è attualmente in corso
    isComputerPlaying: Boolean, // stato che indica se è il turno del computer (disabilita l'input utente)
    isPaused: Boolean, // stato che indica se la sequenza del computer è temporaneamente in pausa
    isGameOver: Boolean, // stato che innesca l'AlertDialog di sconfitta
    activeColor: String?, // l'etichetta del colore attualmente illuminato (es. "R", "G"), null se nessuno

    // callback
    onColorClick: (String) -> Unit, // callback invocata quando l'utente preme un colore valido sulla griglia
    onStartClick: () -> Unit, // callback invocata per inizializzare e avviare una nuova partita
    onPauseClick: () -> Unit, // callback invocata per mettere in pausa l'esecuzione automatica del computer
    onEndGameClick: () -> Unit, // callback invocata per terminare volontariamente la partita o chiudere il dialog di Game Over
    modifier: Modifier = Modifier // modificatore per gestire layout e insets esterni
) {
    // segnalazione di errore che blocca il gioco
    // https://developer.android.com/develop/ui/views/components/dialogs
    if (isGameOver) {
        AlertDialog(
            confirmButton = {},
            onDismissRequest = {
                onEndGameClick() // questo scatta quando l'utente preme il tasto "Back" di sistema mentre il Dialog è aperto
            },
            title = {
                Text(
                    text = stringResource(R.string.game_over_title),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.game_over_description))
            },
            properties = DialogProperties(
                dismissOnClickOutside = false, // impedisce di chiudere il modal cliccando fuori
                dismissOnBackPress = true // permette al tasto Back di invocare onDismissRequest
            ),
        )
    }

    // trasformiamo la lista in una stringa separata da virgole, come da specifiche
    val displayText = if (isComputerPlaying) "" else userSequence.joinToString(", ")

    // configurazione schermo
    val orientation = LocalConfiguration.current.orientation

    // crea e "ricorda" un oggetto che mantiene traccia della posizione attuale dello scorrimento
    // https://developer.android.com/reference/kotlin/androidx/compose/foundation/rememberScrollState.composable
    val scrollState = rememberScrollState()

    // ogni volta che 'displayText' cambia, Compose esegue questo blocco:
    // funzione suspend che sfrutta il frame-clock di Compose per interpolare fluidamente l'offset fino a fine layout
    LaunchedEffect(displayText) {
        // anima lo scroll fino al valore massimo (la fine del testo)
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // intercetta l'evento di sistema OnBackPressedDispatcher, sovrascrivendo la navigazione standard
    // https://developer.android.com/guide/navigation/custom-back
    BackHandler {
        onEndGameClick()
    }

    /*
     * giustificazione layout: in questa schermata utilizzo ConstraintLayout per gestire
     * in modo efficiente ed elegante il cambio di orientamento (Portrait vs Landscape).
     * invece di duplicare il codice UI o annidare complesse gerarchie di Column e Row,
     * ConstraintLayout mi permette di riposizionare gli elementi dinamicamente,
     * modificando semplicemente i loro vincoli (anchor) in base all'orientamento attuale.
     */
    ConstraintLayout(modifier = modifier.fillMaxSize()) { // interfaccia utente
        // creare le reference <=> creare gli ID nella classe View
        val (matrix, textScrollArea, btnStart, btnPause, btnEndGame) = createRefs()

        // linea guida per dividere lo schermo a metà in orizzontale
        val centerGuideline = createGuidelineFromStart(0.5f)

        // catena verticale compatta per la modalità landscape
        // https://developer.android.com/develop/ui/views/layout/constraint-layout#constrain-chain
        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            createVerticalChain(
                textScrollArea,
                btnStart,
                btnPause,
                // il layout a catena 'Packed' compatta i nodi al centro, raggruppando i bottoni senza spazi intermedi
                chainStyle = ChainStyle.Packed
            )

        // matrice 3 x 2 (chiamo la funzione @Composable)
        ColorGrid(
            onColorClick = onColorClick, // uso direttamente il parametro
            isGameRunning = isGameRunning,
            isComputerPlaying = isComputerPlaying,
            activeColor = activeColor,
            modifier = Modifier
                .constrainAs(matrix) {
                    top.linkTo(parent.top, margin = 16.dp)
                    height = Dimension.fillToConstraints
                    width = Dimension.fillToConstraints

                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(centerGuideline, margin = 8.dp)
                    } else { // portrait
                        bottom.linkTo(textScrollArea.top, margin = 16.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                }
        )

        // area di testo multiriga non editabile
        Box(
            modifier = Modifier
                .constrainAs(textScrollArea) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(parent.top)
                        bottom.linkTo(btnStart.top)
                        start.linkTo(centerGuideline, margin = 8.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                    } else { // portrait
                        bottom.linkTo(btnStart.top, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                    }
                    width = Dimension.fillToConstraints
                }
                .padding(bottom = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 24.dp else 0.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(12.dp) // ritaglia la forma
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(12.dp)
                )
                .height(120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center // centra il contenuto
        ) {
            Text(
                text = displayText,
                modifier = Modifier.verticalScroll(scrollState), // scroll verticale
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // bottone "Start Game"
        Button(
            modifier = Modifier
                .constrainAs(btnStart) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(textScrollArea.bottom)
                        bottom.linkTo(btnPause.top)
                        start.linkTo(centerGuideline, margin = 8.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                    } else { // portrait
                        bottom.linkTo(btnPause.top, margin = 8.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                    }
                    // divide uniformemente lo spazio disponibile
                    width = Dimension.fillToConstraints
                }
                .padding(bottom = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 16.dp else 0.dp)
                .height(55.dp),
            onClick = onStartClick, // uso direttamente il parametro
            enabled = !isGameRunning // si disattiva appena il gioco inizia
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.start_str)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.start_str),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // bottone "Pause"
        Button(
            modifier = Modifier
                .constrainAs(btnPause) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(btnStart.bottom)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(centerGuideline, margin = 8.dp)
                    } else { // portrait
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                    }
                    end.linkTo(btnEndGame.start, margin = 4.dp)
                    width = Dimension.fillToConstraints
                }
                .height(55.dp),
            onClick = onPauseClick, // uso direttamente il parametro
            enabled = isComputerPlaying // si attiva SOLO durante il turno del computer
        ) {
            val text =
                if (isPaused) stringResource(R.string.resume_str) else stringResource(R.string.pause_str)

            Icon(
                imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = text
            )
            Spacer(modifier = Modifier.width(8.dp))
            // cambia testo in base allo stato
            Text(
                text = text,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        // bottone "End Game"
        Button(
            modifier = Modifier
                .constrainAs(btnEndGame) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(btnPause.top)
                        bottom.linkTo(btnPause.bottom)
                        start.linkTo(btnPause.end, margin = 4.dp)
                    } else { // portrait
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                        start.linkTo(btnPause.end, margin = 4.dp)
                    }
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.fillToConstraints
                }
                .height(55.dp),
            onClick = onEndGameClick, // uso direttamente il parametro
            enabled = isGameRunning // rimane attivo per tutta la durata della partita
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.end_str)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.end_str),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ColorGrid(
    onColorClick: (String) -> Unit,
    isGameRunning: Boolean,
    isComputerPlaying: Boolean,
    activeColor: String?,
    modifier: Modifier = Modifier
) {
    /*
     * trasformazione in catena: randomizza l'array e lo partiziona in List annidate di dimensione 2 per formare le righe.
     * faccio uno shuffle sui colori e salvo la disposizione (rememberSaveable).
     * per evitare il crash dovuto alla serializzazione di SimonColor (e del relativo Color di Compose)
     * all'interno del Bundle di rememberSaveable, memorizza solo le label di tipo String.
     * in questo modo i colori sono random, non cambiano posizione ad ogni click e la griglia è sicura contro i crash da rotazione.
     */
    val savedColorLabels = rememberSaveable {
        simonColors.shuffled().map { it.label }
    }

    // ricostruisce le righe associando a ciascuna etichetta l'oggetto SimonColor originale
    val rows = remember(savedColorLabels) {
        savedColorLabels.map { label ->
            simonColors.first { it.label == label }
        }.chunked(2)
    }

    // matrice 3 x 2
    Column(
        modifier = modifier.padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        rows.forEach { rowColors ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // peso verticale: ogni riga prende esattamente 1/3 dell'altezza
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowColors.forEach { simonColor ->
                    // calcola l'opacità (1f se è il colore attivo, altrimenti 0.4f)
                    val backgroundAlpha = if (simonColor.label == activeColor) 1f else 0.4f

                    Box(
                        modifier = Modifier
                            .weight(1f) // peso orizzontale: ogni colore prende esattamente 1/2 della larghezza
                            .fillMaxHeight() // deve riempire l'altezza della riga
                            .clip(RoundedCornerShape(10.dp)) // ritaglia la forma
                            .background(simonColor.color.copy(alpha = backgroundAlpha)) // sfondo a scelta tra i 6 colori
                            // disabilita il click se il gioco non è partito o se è il turno del computer
                            .clickable(
                                enabled = isGameRunning && !isComputerPlaying,
                                onClick = { onColorClick(simonColor.label) }
                            )
                    )
                }
            }
        }
    }
}