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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimonTheme {
                // STATE HOISTING: lo stato ora vive a livello di Activity
                // stato della sequenza (salvato anche per passaggio portrait <-> landscape)
                var currentSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

                // stato dello storico delle partite (salvato anche per passaggio portrait <-> landscape)
                var gamesHistory by rememberSaveable { mutableStateOf(listOf<List<String>>()) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        // stato della sequenza corrente
                        currentSequence = currentSequence,

                        // implementazione della callback per il click su un colore
                        onColorClick = { colorLabel ->
                            currentSequence += colorLabel // aggiunge la lettera alla sequenza
                        },

                        // implementazione della callback per il click su "cancella"
                        onCancelClick = { currentSequence = emptyList() }, // azzera la sequenza

                        // implementazione della callback per inviare lo storico delle partite concluse
                        onEndGameClick = {
                            // aggiorno lo storico aggiungendo la sequenza corrente
                            gamesHistory += listOf(currentSequence)

                            // svuoto la sequenza per la prossima partita
                            currentSequence = emptyList()

                            // creo l'intent esplicito per avviare HistoryActivity
                            val myIntent = Intent(this, HistoryActivity::class.java).apply {

                                // List<List<String>> -> List<String> -> ArrayList<String>
                                // mappo la List<List<String>> in una List<String> e la passo direttamente al costruttore di ArrayList
                                val stringHistory =
                                    ArrayList(gamesHistory.map { it.joinToString(", ") })

                                // inserisco l'ArrayList nell'intent
                                putStringArrayListExtra("GAMES_HISTORY", stringHistory)

                                // stampo i dati a scopo di test (v = verbose)
                                Log.v("GAMES_HISTORY", "$stringHistory")
                            }

                            // lancio l'activity passandogli l'intent
                            startActivity(myIntent)
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .displayCutoutPadding(),
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
    onCancelClick: () -> Unit,
    onEndGameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // trasformiamo la lista in una stringa separata da virgole, come da specifiche
    val displayText = currentSequence.joinToString(", ")

    // configurazione schermo
    val orientation = LocalConfiguration.current.orientation

    // stato dello scroll per l'area di testo
    val scrollState = rememberScrollState()

    // ogni volta che 'displayText' cambia, compose esegue questo blocco
    LaunchedEffect(displayText) {
        // anima lo scroll fino al valore massimo (la fine del testo)
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    /**
     * GIUSTIFICAZIONE LAYOUT: in questa schermata utilizzo ConstraintLayout per gestire
     * in modo efficiente ed elegante il cambio di orientamento (Portrait vs Landscape).
     * invece di duplicare il codice UI o annidare complesse gerarchie di Column e Row,
     * ConstraintLayout mi permette di riposizionare gli elementi dinamicamente
     * modificando semplicemente i loro vincoli (anchor) in base all'orientamento attuale.
     */
    ConstraintLayout(modifier = modifier.fillMaxSize()) { // interfaccia utente
        // creare le reference <=> creare gli ID nella classe View
        val (matrix, textScrollArea, btnCancel, btnEndGame) = createRefs()

        // linea guida per dividere lo schermo a metà in orizzontale
        val centerGuideline = createGuidelineFromStart(0.5f)

        // lLinea guida orizzontale al 60% dell'altezza per il portrait
        val horizontalGuideline = createGuidelineFromTop(0.60f)

        // matrice 3 x 2
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

        // area di testo multiriga
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
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp)
                )
                .height(120.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                modifier = Modifier.verticalScroll(scrollState),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // bottone "Cancella"
        Button(
            modifier = Modifier.constrainAs(btnCancel) {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    top.linkTo(textScrollArea.bottom, margin = 16.dp)
                    start.linkTo(centerGuideline, margin = 8.dp)
                } else { // portrait
                    top.linkTo(textScrollArea.bottom, margin = 32.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                }
            },
            onClick = onCancelClick
        ) {
            Text(text = stringResource(R.string.cancel_str))
        }

        // bottone "Fine partita"
        Button(
            modifier = Modifier.constrainAs(btnEndGame) {
                end.linkTo(parent.end, margin = 16.dp)

                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    top.linkTo(textScrollArea.bottom, margin = 16.dp)
                } else { // portrait
                    top.linkTo(textScrollArea.bottom, margin = 32.dp)
                }
            },
            onClick = onEndGameClick
        ) {
            Text(text = stringResource(R.string.endgame_str))
        }
    }
}

@Composable
private fun ColorGrid(
    modifier: Modifier = Modifier,
    onColorClick: (String) -> Unit // callback invocata al click su un rettangolo
) {
    // faccio uno shuffle sui colori e salvo la disposizione
    // in questo modo i colori sono random, ma non cambiano posizione ad ogni click
    // inoltre divido i 6 colori in 3 gruppi da 2 (3 righe x 2 colonne)
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
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(10.dp)
                            )
                            // rende i box cliccabili e passa la lettera alla callback
                            .clickable { onColorClick(simonColor.label) }
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
        currentSequence = listOf("R, G, B"),
        onColorClick = {},
        onCancelClick = {},
        onEndGameClick = {}
    )
}

data class SimonColor(val name: String, val color: Color, val label: String)

// lista di oggetti che mappano il colore UI alla lettera identificativa richiesta
private val simonColors = listOf(
    SimonColor("Red", Color.Red, "R"),
    SimonColor("Green", Color.Green, "G"),
    SimonColor("Blue", Color.Blue, "B"),
    SimonColor("Magenta", Color.Magenta, "M"),
    SimonColor("Yellow", Color.Yellow, "Y"),
    SimonColor("Cyan", Color.Cyan, "C")
)
