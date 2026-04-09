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
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .displayCutoutPadding()
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        // implementazione della callback per inviare lo storico delle partite concluse
                        onEndGame = { history ->
                            // 1. creo l'intent esplicito per avviare HistoryActivity
                            val myIntent = Intent(this, HistoryActivity::class.java).apply {

                                // List<List<String>> -> List<String> -> ArrayList<String>
                                val stringHistory = history.map {
                                    it.joinToString(", ")
                                }.toCollection(ArrayList())

                                // inserisco l'ArrayList nell'intent
                                putStringArrayListExtra("GAMES_HISTORY", stringHistory)

                                // stampo i dati a scopo di test (v = verbose)
                                Log.v("GAMES_HISTORY", "$stringHistory")
                            }

                            // 2. lancio l'activity passandogli l'intent
                            startActivity(myIntent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onEndGame: (List<List<String>>) -> Unit // callback per passare lo storico alla HistoryActivity
) {
    // stato della sequenza (salvato anche per passaggio portrait <-> landscape)
    var currentSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

    // stato dello storico delle partite (salvato anche per passaggio portrait <-> landscape)
    var gamesHistory by rememberSaveable { mutableStateOf(listOf<List<String>>()) }

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
            onColorClick = { colorLabel -> // implementazione della callback
                currentSequence += colorLabel // aggiunge la lettera alla sequenza
            }
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
            onClick = {
                currentSequence = emptyList() // azzera la sequenza
            }
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
            onClick = {
                // salvo la sequenza finale per poterla inviare alla HistoryActivity
                val finalSequence = currentSequence.toList()
                currentSequence = emptyList() // svuota l'area di testo

                // aggiungo la partita conclusa allo storico
                gamesHistory += listOf(finalSequence)

                // invoco la callback passando i dati verso l'alto
                onEndGame(gamesHistory)
            }
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
    // matrice 3 x 2
    Column(
        modifier = modifier.padding(horizontal = 15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val rows =
            simonColors.chunked(2) // divido i 6 colori in 3 gruppi da 2 (3 righe x 2 colonne)

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
    MainScreen(onEndGame = {})
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