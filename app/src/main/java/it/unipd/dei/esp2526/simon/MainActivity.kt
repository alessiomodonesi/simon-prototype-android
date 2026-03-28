package it.unipd.dei.esp2526.simon

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
        enableEdgeToEdge()
        setContent {
            SimonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        onEndGame = { history -> // implementazione della callback
                            // creo l'intent esplicito per avviare HistoryActivity
                            val myIntent = Intent(this, HistoryActivity::class.java).apply {

                                // List<List<String>> -> List<String> -> ArrayList<String>
                                val stringHistory = ArrayList(history.map { it.joinToString(", ") })

                                // inserisco l'ArrayList nell'intent
                                putStringArrayListExtra("GAMES_HISTORY", stringHistory)

                                // stampo i dati a scopo di test
                                Log.d("GAMES_HISTORY", "$stringHistory")
                            }
                            startActivity(myIntent) // avvio la 2a activity
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
    // stato della sequenza (salvato)
    var currentSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

    // stato dello storico delle partite (salvato)
    var gamesHistory by rememberSaveable { mutableStateOf(listOf<List<String>>()) }

    // trasformiamo la lista in una stringa separata da virgole
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

        // matrice 3 x 2
        ColorGrid(
            modifier = Modifier
                .constrainAs(matrix) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(parent.top, margin = 16.dp)
                        bottom.linkTo(parent.bottom, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(centerGuideline, margin = 8.dp)
                        width = Dimension.fillToConstraints
                    } else {
                        top.linkTo(parent.top, margin = 30.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                },
            onColorClick = { colorLabel -> // implementazione della callback
                // aggiunge la lettera alla sequenza
                currentSequence += colorLabel
            }
        )

        // area di testo multiriga
        Text(
            text = displayText,
            modifier = Modifier
                .constrainAs(textScrollArea) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        top.linkTo(parent.top, margin = 100.dp)
                        start.linkTo(centerGuideline, margin = 8.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                        width = Dimension.fillToConstraints
                    } else {
                        top.linkTo(matrix.bottom, margin = 16.dp)
                        start.linkTo(parent.start, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                        width = Dimension.fillToConstraints
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
                .verticalScroll(scrollState)
                .padding(16.dp)
                .heightIn(min = 100.dp),

            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        // bottone "Cancella"
        Button(
            modifier = Modifier.constrainAs(btnCancel) {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    top.linkTo(textScrollArea.bottom, margin = 16.dp)
                    start.linkTo(centerGuideline, margin = 8.dp)
                } else {
                    top.linkTo(textScrollArea.bottom, margin = 16.dp)
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
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    top.linkTo(textScrollArea.bottom, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                } else {
                    top.linkTo(textScrollArea.bottom, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
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
fun ColorGrid(
    modifier: Modifier = Modifier,
    onColorClick: (String) -> Unit // callback invocata al click su un rettangolo
) {
    // matrice 3 x 2
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // numero di colonne
        modifier = modifier.fillMaxWidth(), // larghezza della griglia
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // gli elementi della matrice sono contenuti nella lista simonColors
        items(simonColors) { simonColor ->
            Box(
                modifier = Modifier
                    .aspectRatio(1.5f) // rettangoli
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

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(onEndGame = {})
}

data class SimonColor(val name: String, val color: Color, val label: String)

// lista di oggetti che mappano il colore UI alla lettera identificativa richiesta
val simonColors = listOf(
    SimonColor("Red", Color.Red, "R"),
    SimonColor("Green", Color.Green, "G"),
    SimonColor("Blue", Color.Blue, "B"),
    SimonColor("Magenta", Color.Magenta, "M"),
    SimonColor("Yellow", Color.Yellow, "Y"),
    SimonColor("Cyan", Color.Cyan, "C")
)