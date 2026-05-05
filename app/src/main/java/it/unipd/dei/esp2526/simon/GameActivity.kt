package it.unipd.dei.esp2526.simon

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import it.unipd.dei.esp2526.simon.model.simonColors

import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// tag per il logger di debug di GameActivity
const val mTAG = "GameActivity"

class GameActivity : ComponentActivity() {
    private val vm: ViewModel by viewModels() // inizializzo il ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimonTheme {
                // stato di GameActivity: la sequenza contenuta nell'area di testo multiriga non editabile (Instance State)
                var currentSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

                // stato per capire se il gioco è in corso o meno
                var isGameRunning by rememberSaveable { mutableStateOf(false) }

                // stato per capire se il computer ha il comando
                var isComputerPlaying by rememberSaveable { mutableStateOf(false) }

                // stato per il colore attualmente illuminato
                var activeColor by remember { mutableStateOf<String?>(null) }

                // scope per lanciare le coroutine legate al ciclo di vita della composizione
                // https://developer.android.com/kotlin/coroutines
                val coroutineScope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        // passo le variabili di stato
                        currentSequence = currentSequence,
                        isGameRunning = isGameRunning,
                        isComputerPlaying = isComputerPlaying,
                        activeColor = activeColor,

                        // funzione lambda per il click su un colore, riceve come parametro l'indice del colore premuto
                        onColorClick = { colorLabel ->
                            currentSequence += colorLabel // aggiunge la lettera alla sequenza
                            Log.v(mTAG, "$colorLabel Btn clicked")

                            // animazione del feedback visivo dell'utente
                            coroutineScope.launch {
                                activeColor = colorLabel // accende il colore
                                delay(250) // tiene acceso per 250ms
                                activeColor = null // spegne
                            }
                        },

                        // funzione lambda per il click sul tasto "Start Game"
                        onStartClick = {
                            isGameRunning = true
                            isComputerPlaying = true // il computer inizia a proporre
                            Log.v(mTAG, "Start Game Btn clicked")
                        },

                        // funzione lambda per il click sul tasto "Pause"
                        onPauseClick = {
                            Log.v(mTAG, "Pause Btn clicked")
                        },

                        // funzione lambda per il click sul tasto "End Game", aggiorna la lista di sequenze giocate prima di cancellare la sequenza appena terminata,
                        // poi lancia un intent verso HistoryActivity passando come dato la lista di sequenze giocate
                        onEndGameClick = {
                            isGameRunning = false
                            Log.v(mTAG, "End Game Btn clicked")

                            // calcolo la lunghezza massima (se c'è un errore, la sequenza salvata è n+1, quindi la max length è n)
                            val sequence = currentSequence.joinToString(", ")
                            val maxLength =
                                if (currentSequence.isNotEmpty()) currentSequence.size - 1 else 0

                            // salva nel database chiamando la fun insertGame() dal ViewModel
                            vm.insertGame(
                                maxLength = maxLength,
                                sequence = sequence
                            )

                            // svuoto la sequenza per la prossima partita
                            currentSequence = emptyList()

                            // utilizzo finish() per chiudere GameActivity (pop) e tornare indietro
                            this@GameActivity.finish()
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .displayCutoutPadding(), // https://developer.android.com/develop/ui/views/layout/display-cutout
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    currentSequence: List<String>,
    onColorClick: (String) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onEndGameClick: () -> Unit,
    isGameRunning: Boolean,
    isComputerPlaying: Boolean,
    activeColor: String?,
    modifier: Modifier = Modifier
) {
    // trasformiamo la lista in una stringa separata da virgole, come da specifiche
    val displayText = currentSequence.joinToString(", ")

    // configurazione schermo
    val orientation = LocalConfiguration.current.orientation

    // crea e "ricorda" un oggetto che mantiene traccia della posizione attuale dello scorrimento
    // https://developer.android.com/reference/kotlin/androidx/compose/foundation/rememberScrollState.composable
    val scrollState = rememberScrollState()

    // ogni volta che 'displayText' cambia, Compose esegue questo blocco
    LaunchedEffect(displayText) {
        // anima lo scroll fino al valore massimo (la fine del testo)
        scrollState.animateScrollTo(scrollState.maxValue)
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

        // linea guida orizzontale al 60% dell'altezza per il portrait
        val horizontalGuideline = createGuidelineFromTop(0.6f)

        // matrice 3 x 2 (chiamo la funzione @Composable)
        ColorGrid(
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
                        bottom.linkTo(horizontalGuideline, margin = 16.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                },
            onColorClick = onColorClick, // uso direttamente il parametro
            isGameRunning = isGameRunning,
            isComputerPlaying = isComputerPlaying,
            activeColor = activeColor
        )

        // area di testo multiriga non editabile
        Box(
            modifier = Modifier
                .constrainAs(textScrollArea) {
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.fillToConstraints

                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(parent.top, margin = 100.dp)
                        start.linkTo(centerGuideline, margin = 8.dp)
                    } else { // portrait
                        top.linkTo(horizontalGuideline, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                    }
                }
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp) // ritaglia la forma
                )
                .border(
                    width = 3.dp,
                    brush = rainbowBrush, // colore sfumato
                    // color = MaterialTheme.colorScheme.outline,
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
                        top.linkTo(textScrollArea.bottom, margin = 16.dp)
                        start.linkTo(centerGuideline, margin = 8.dp)
                    } else { // portrait
                        top.linkTo(textScrollArea.bottom, margin = 32.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                    }
                    // collega la fine all'inizio del bottone Pause
                    end.linkTo(btnPause.start, margin = 4.dp)
                    // divide uniformemente lo spazio disponibile
                    width = Dimension.fillToConstraints
                }
                .height(55.dp),
            onClick = onStartClick, // uso direttamente il parametro
            enabled = !isGameRunning // si disattiva appena il gioco inizia
        ) {
            Icon(
                Icons.Filled.PlayArrow,
                stringResource(R.string.start_str)
            )
        }

        // bottone "Pause"
        Button(
            modifier = Modifier
                .constrainAs(btnPause) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(textScrollArea.bottom, margin = 16.dp)
                    } else { // portrait
                        top.linkTo(textScrollArea.bottom, margin = 32.dp)
                    }
                    // incatenato tra Start e End Game
                    start.linkTo(btnStart.end, margin = 4.dp)
                    end.linkTo(btnEndGame.start, margin = 4.dp)
                    width = Dimension.fillToConstraints
                }
                .height(55.dp),
            onClick = onPauseClick, // uso direttamente il parametro
            enabled = isComputerPlaying // si attiva SOLO durante il turno del computer
        ) {
            Icon(
                Icons.Filled.Pause,
                stringResource(R.string.pause_str)
            )
        }

        // bottone "End Game"
        Button(
            modifier = Modifier
                .constrainAs(btnEndGame) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(textScrollArea.bottom, margin = 16.dp)
                    } else { // portrait
                        top.linkTo(textScrollArea.bottom, margin = 32.dp)
                    }
                    // inizia dove finisce Pause e termina a fine schermo
                    start.linkTo(btnPause.end, margin = 4.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.fillToConstraints
                }
                .height(55.dp),
            onClick = onEndGameClick, // uso direttamente il parametro
            enabled = isGameRunning // rimane attivo per tutta la durata della partita
        ) {
            Icon(
                Icons.Filled.Stop,
                stringResource(R.string.end_str)
            )
        }
    }
}

@Composable
private fun ColorGrid(
    modifier: Modifier = Modifier,
    onColorClick: (String) -> Unit,
    isGameRunning: Boolean,
    isComputerPlaying: Boolean,
    activeColor: String?
) {
    /**
     * faccio uno shuffle sui colori e salvo la disposizione (remember).
     * in questo modo i colori sono random, ma non cambiano posizione ad ogni click.
     * inoltre divido (chunked) i 6 colori in 3 gruppi da 2 (3 righe x 2 colonne).
     */
    val rows = remember {
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
                                enabled = isGameRunning, // provvisorio
                                // enabled = isGameRunning && !isComputerPlaying,
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
        currentSequence = listOf("R, G, B"), // dati fittizi, servono solo alla preview di android studio
        onColorClick = {},
        onStartClick = {},
        onPauseClick = {},
        onEndGameClick = {},
        isGameRunning = false,
        isComputerPlaying = false,
        activeColor = null
    )
}