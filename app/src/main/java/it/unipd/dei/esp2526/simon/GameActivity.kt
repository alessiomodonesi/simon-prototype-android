package it.unipd.dei.esp2526.simon

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import it.unipd.dei.esp2526.simon.model.simonColors
import it.unipd.dei.esp2526.simon.utils.*

import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameActivity : ComponentActivity() {
    private val vm: ViewModel by viewModels() // inizializzo il ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inizializzo i suoni in memoria
        SoundManager.initialize(durationMs = 250)

        setContent {
            SimonTheme {
                // stato per tenere traccia della sequenza generata dal computer:
                // rememberSaveable serializza il dato in un Bundle di sistema, sopravvivendo alla distruzione dell'Activity (es. rotazione)
                var computerSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

                // stato per tenere traccia della sequenza riprodotta dall'utente
                var userSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

                // stato per capire se il gioco è in corso o meno
                var isGameRunning by rememberSaveable { mutableStateOf(false) }

                // stato per capire se il computer ha il comando
                var isComputerPlaying by rememberSaveable { mutableStateOf(false) }

                // stato per gestire la pausa durante il turno del computer
                var isPaused by rememberSaveable { mutableStateOf(false) }

                // stato per gestire la sconfitta dell'utente
                var isGameOver by rememberSaveable { mutableStateOf(false) }

                // stato per il colore attualmente illuminato
                var activeColor by rememberSaveable { mutableStateOf<String?>(null) }

                // stato per l'indice della sequenza del computer
                var computerPlaybackIndex by rememberSaveable { mutableIntStateOf(0) }

                // scope per lanciare le coroutine legate al ciclo di vita della composizione
                // https://developer.android.com/kotlin/coroutines
                val coroutineScope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // grandezza del cutout
                    val layoutDirection = LocalLayoutDirection.current
                    val cutout = WindowInsets.displayCutout.asPaddingValues()

                    // valore massimo tra lato destro e sinistro
                    val symmetricCutout = max(
                        cutout.calculateLeftPadding(layoutDirection),
                        cutout.calculateRightPadding(layoutDirection)
                    )

                    // scatta ogni volta che isComputerPlaying diventa true o l'activity viene ricreata
                    LaunchedEffect(isComputerPlaying) {
                        if (isComputerPlaying && computerSequence.isNotEmpty()) {
                            // se parto da zero = non è un ripristino da rotazione, facciamo una pausa iniziale
                            if (computerPlaybackIndex == 0) delay(1000)

                            // faccio continuare la sequenza da dove si era fermata
                            GameEngine.playComputerSequence(
                                sequence = computerSequence,
                                startIndex = computerPlaybackIndex,
                                isPaused = { isPaused },
                                onColorActive = { activeColor = it },
                                onIndexUpdate = { computerPlaybackIndex = it }
                            )

                            // quando la sequenza finisce regolarmente, passo il turno all'utente
                            if (computerPlaybackIndex >= computerSequence.size)
                                isComputerPlaying = false
                        }
                    }

                    GameScreen(
                        // passo le variabili di stato
                        userSequence = userSequence,
                        isGameRunning = isGameRunning,
                        isComputerPlaying = isComputerPlaying,
                        isPaused = isPaused,
                        isGameOver = isGameOver,
                        activeColor = activeColor,

                        // funzione lambda per il click su un colore, riceve come parametro l'indice del colore premuto
                        onColorClick = { colorLabel ->
                            // ignora i click se: il gioco è finito || se NON è avviato || se tocca al computer
                            if (isGameOver || !isGameRunning || isComputerPlaying) return@GameScreen

                            userSequence += colorLabel // aggiunge la lettera alla sequenza utente
                            Log.v(mTAG, "$colorLabel Btn clicked")

                            // animazione del feedback visivo e uditivo dell'utente
                            // chiama la funzione dentro GameUtils.kt
                            coroutineScope.launch {
                                playColorFeedback(
                                    colorLabel = colorLabel,
                                    durationMs = 250,
                                    onColorActive = { activeColor = it }
                                )
                            }

                            // indice per la validazione della mossa
                            val i = userSequence.size - 1

                            // verifica se l'indice esiste nella sequenza del computer e se il colore coincide
                            if (i < computerSequence.size && colorLabel == computerSequence[i]) { // mossa corretta
                                if (userSequence.size == computerSequence.size) { // l'utente ha completato l'intera sequenza correttamente
                                    // reset della sequenza utente prima del turno del computer
                                    userSequence = emptyList()

                                    // reset dell'indice
                                    computerPlaybackIndex = 0

                                    // aggiunge un colore
                                    computerSequence =
                                        GameEngine.generateNextSequence(computerSequence)

                                    // innesca il LaunchedEffect in automatico per il turno successivo
                                    isComputerPlaying = true
                                }
                            } else { // mossa errata
                                isGameOver = true
                                isGameRunning = false // ferma il gioco e disabilita ulteriori input
                            }
                        },

                        // funzione lambda per il click sul tasto "Start Game"
                        onStartClick = {
                            isGameRunning = true
                            userSequence = emptyList()
                            computerPlaybackIndex = 0 // resetta l'indice
                            computerSequence =
                                GameEngine.generateNextSequence(emptyList()) // genera la prima mossa
                            isComputerPlaying =
                                true // questo farà scattare il LaunchedEffect da solo
                            Log.v(mTAG, "Start Game Btn clicked")
                        },

                        // funzione lambda per il click sul tasto "Pause"
                        onPauseClick = {
                            isPaused = !isPaused // inverte lo stato di pausa ad ogni click
                            Log.v(mTAG, "Pause Btn clicked")
                        },

                        // funzione lambda per il click sul tasto "End Game", aggiorna la lista di sequenze giocate prima di cancellare la sequenza appena terminata,
                        // poi lancia un intent verso HistoryActivity passando come dato la lista di sequenze giocate
                        onEndGameClick = {
                            Log.v(mTAG, "End Game Btn clicked")

                            // se non c'è stato un vero game over && (il gioco non è partito || siamo al 1o turno),
                            // l'app si comporta come se non fosse mai iniziata e non salva nulla[cite: 9].
                            if (!isGameOver && (!isGameRunning || computerSequence.size <= 1)) {
                                isGameRunning = false
                                isPaused = false // resetto la pausa a fine partita
                                userSequence = emptyList()
                                this@GameActivity.finish()
                                return@GameScreen // esce immediatamente dalla lambda senza eseguire il resto
                            }

                            // la sequenza da salvare è quella COMPLETA proposta dal computer in questo turno
                            val sequence = computerSequence.joinToString(", ")

                            // if: l'utente ha sbagliato un colore, else: uscita volontaria
                            val maxLength =
                                if (isGameOver) userSequence.size - 1 else userSequence.size

                            // salva nel database chiamando la fun insertGame() dal ViewModel
                            vm.insertGame(
                                maxLength = maxLength,
                                sequence = sequence
                            )

                            // svuoto la sequenza per la prossima partita
                            userSequence = emptyList()

                            // utilizzo finish() per chiudere GameActivity (pop) e tornare indietro
                            this@GameActivity.finish()
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = symmetricCutout)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SoundManager.release()
    }
}

@Composable
fun GameScreen(
    userSequence: List<String>,
    onColorClick: (String) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onEndGameClick: () -> Unit,
    isGameRunning: Boolean,
    isComputerPlaying: Boolean,
    isPaused: Boolean,
    isGameOver: Boolean,
    activeColor: String?,
    modifier: Modifier = Modifier
) {
    // segnalazione di errore che blocca il gioco
    // https://developer.android.com/develop/ui/views/components/dialogs
    if (isGameOver) {
        androidx.compose.material3.AlertDialog(
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

    // brush sfumato con i colori dell'arcobaleno (gradiente lineare)
    val rainbowBrush = Brush.linearGradient(
        colors = listOf(
            Color.Red,
            Color(0xFFFF9800),
            Color.Yellow,
            Color.Green,
            Color.Blue,
            Color(0xFF3F51B5),
            Color(0xFF9C27B0)
        )
    )

    /**
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
                chainStyle = androidx.constraintlayout.compose.ChainStyle.Packed
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp) // ritaglia la forma
                )
                .border(
                    width = 3.dp,
                    brush = rainbowBrush, // colore sfumato
                    shape = RoundedCornerShape(10.dp)
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
    /**
     * trasformazione in catena: randomizza l'array e lo partiziona in List annidate di dimensione 2 per formare le righe.
     * faccio uno shuffle sui colori e salvo la disposizione (remember).
     * in questo modo i colori sono random, ma non cambiano posizione ad ogni click.
     * inoltre divido (chunked) i 6 colori in 3 gruppi da 2 (3 righe x 2 colonne).
     */
    val rows = rememberSaveable() {
        simonColors.shuffled().chunked(2)
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

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    GameScreen(
        userSequence = listOf("R, G, B"), // dati fittizi, servono solo alla preview di android studio
        onColorClick = {},
        onStartClick = {},
        onPauseClick = {},
        onEndGameClick = {},
        isGameRunning = false,
        isComputerPlaying = false,
        isPaused = false,
        isGameOver = false,
        activeColor = null
    )
}

// tag per il logger di debug di GameActivity
const val mTAG = "GameActivity"