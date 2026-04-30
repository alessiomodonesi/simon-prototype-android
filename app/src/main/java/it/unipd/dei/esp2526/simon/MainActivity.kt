package it.unipd.dei.esp2526.simon

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme

// tag per il logger di debug di MainActivity
const val mTAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimonTheme {
                /**
                 * stato di MainActivity : la lista di sequenze giocate (Instance State).
                 * questa lista viene passata tramite intent ad HistoryActivity per visualizzare lo storico delle partite.
                 * sopravvive ai cambi di orientamento grazie a rememberSaveable,
                 * ma verrà persa alla terminazione dell'app (non c'è stato persistente),
                 * rispettando esattamente le specifiche richieste.
                 */
                var gamesHistory by rememberSaveable { mutableStateOf(listOf<List<String>>()) }

                // stato di MainActivity : la sequenza contenuta nell'area di testo multiriga non editabile (Instance State)
                var currentSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        // stato della sequenza corrente
                        currentSequence = currentSequence,

                        // funzione lambda per il click su un colore, riceve come parametro l'indice del colore premuto
                        onColorClick = { colorLabel ->
                            currentSequence += colorLabel // aggiunge la lettera alla sequenza
                            Log.v(mTAG, "$colorLabel Btn clicked")
                        },

                        // funzione lambda per il click sul tasto "Start Game"
                        onStartClick = {
                            Log.v(mTAG, "Start Game Btn clicked")
                        },

                        // funzione lambda per il click sul tasto "Pause"
                        onPauseClick = {
                            Log.v(mTAG, "Pause Btn clicked")
                        },

                        // funzione lambda per il click sul tasto "End Game", aggiorna la lista di sequenze giocate prima di cancellare la sequenza appena terminata,
                        // poi lancia un intent verso HistoryActivity passando come dato la lista di sequenze giocate
                        onEndGameClick = {
                            Log.v(mTAG, "End Game Btn clicked")

                            // aggiorno lo storico aggiungendo la sequenza corrente
                            gamesHistory += listOf(currentSequence)

                            /**
                             * nota sul ritorno alla 1a activity (tasto back): svuotando lo stato qui, mi assicuro che
                             * quando l'utente premerà il tasto "back" da HistoryActivity, MainActivity
                             * (che è rimasta in pausa sotto nello stack) si presenterà già pulita
                             * e pronta per una nuova partita, come richiesto dalle specifiche.
                             */
                            currentSequence =
                                emptyList() // svuoto la sequenza per la prossima partita

                            // creo l'intent esplicito per avviare HistoryActivity
                            val historyIntent = Intent(this, HistoryActivity::class.java).apply {

                                // List<List<String>> -> List<String> -> ArrayList<String>
                                // mappo la List<List<String>> in una List<String> e la passo direttamente al costruttore di ArrayList
                                val stringHistory =
                                    ArrayList(gamesHistory.map { it.joinToString(", ") })

                                // inserisco l'ArrayList nell'intent
                                putStringArrayListExtra("GAMES_HISTORY", stringHistory)

                                // stampo i dati a scopo di test (v = verbose)
                                Log.v("GAMES_HISTORY", "$stringHistory")
                            }

                            // lancio l'activity passandogli l'intent, this è il Context
                            this.startActivity(historyIntent)
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
fun MainScreen(
    currentSequence: List<String>,
    onColorClick: (String) -> Unit,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onEndGameClick: () -> Unit,
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
            onColorClick = onColorClick // uso direttamente il parametro
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
            onClick = onStartClick // uso direttamente il parametro
        ) {
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
            onClick = onPauseClick // uso direttamente il parametro
        ) {
            Text(
                text = stringResource(R.string.pause_str),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
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
            onClick = onEndGameClick // uso direttamente il parametro
        ) {
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
    modifier: Modifier = Modifier,
    onColorClick: (String) -> Unit
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
                    Box(
                        modifier = Modifier
                            .weight(1f) // peso orizzontale: ogni colore prende esattamente 1/2 della larghezza
                            .fillMaxHeight() // deve riempire l'altezza della riga
                            .clip(RoundedCornerShape(10.dp)) // ritaglia la forma
                            .background(simonColor.color) // sfondo a scelta tra i 6 colori
                            .clickable { onColorClick(simonColor.label) } // rende il box cliccabile e passa la lettera alla callback
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(
        currentSequence = listOf("R, G, B"), // dati fittizi, servono solo alla preview di android studio
        onColorClick = {},
        onStartClick = {},
        onPauseClick = {},
        onEndGameClick = {}
    )
}

/**
 * struttura dati di supporto per mappare il colore visivo alla sua etichetta di logica.
 *
 * @param name nome esteso del colore (es. "Red")
 * @param color oggetto Color di Compose per renderizzare lo sfondo del rettangolo
 * @param label la singola lettera identificativa in inglese da inserire nella sequenza (es. "R")
 */
private data class SimonColor(val name: String, val color: Color, val label: String)

/** lista dei 6 colori specifici richiesti dalla consegna.
 * vengono istanziati qui staticamente per non dipendere dai file strings.xml
 * ed evitare traduzioni accidentali delle etichette (label).
 */
private val simonColors = listOf(
    SimonColor("Red", Color.Red, "R"),
    SimonColor("Green", Color.Green, "G"),
    SimonColor("Blue", Color.Blue, "B"),
    SimonColor("Magenta", Color.Magenta, "M"),
    SimonColor("Yellow", Color.Yellow, "Y"),
    SimonColor("Cyan", Color.Cyan, "C")
)
