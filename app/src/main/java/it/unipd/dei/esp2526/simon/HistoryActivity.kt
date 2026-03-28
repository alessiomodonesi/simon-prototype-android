package it.unipd.dei.esp2526.simon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SecondScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun SecondScreen(modifier: Modifier = Modifier) {
    // dati fittizi usati solo per visualizzare l'anteprima grafica
    val tmp = listOf(
        listOf("R", "G", "B"),
        listOf(
            "M",
            "Y",
            "C",
            "R",
            "G",
            "B",
            "M",
            "Y",
            "C",
            "R",
            "G",
            "B",
            "M",
            "Y",
            "C"
        ),
        emptyList()
    )

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // creare le reference <=> creare gli ID nella classe View
        val (titleText, historyList) = createRefs()

        // titolo
        Text(
            text = stringResource(R.string.history_title),
            modifier = Modifier.constrainAs(titleText) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        )

        // lista dinamica
        LazyColumn(
            modifier = Modifier.constrainAs(historyList) {
                top.linkTo(titleText.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
            },
        ) {
            items(tmp) { sequence ->
                GameHistoryRow(sequence = sequence)
            }
        }
    }
}

@Composable
fun GameHistoryRow(sequence: List<String>) {
    // riga che contiene le informazioni di una singola partita
    ConstraintLayout(modifier = Modifier) {
        // creare le reference <=> creare gli ID nella classe View
        val (sizeText, sequenceText) = createRefs()

        // numero di rettangoli premuti
        Text(
            text = "${sequence.size}",
            modifier = Modifier.constrainAs(sizeText) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
        )

        // sequenza di rettangoli premuti
        Text(
            text = sequence.joinToString(", "),
            modifier = Modifier.constrainAs(sequenceText) {
                end.linkTo(parent.end)
                start.linkTo(sizeText.end, margin = 16.dp)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SecondScreenPreview() {
    SimonTheme {
        SecondScreen()
    }
}