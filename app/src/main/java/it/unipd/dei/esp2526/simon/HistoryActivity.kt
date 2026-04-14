package it.unipd.dei.esp2526.simon

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recupero l'ArrayList<String> dall'intent usando la stessa chiave
        val historyData = intent.getStringArrayListExtra("GAMES_HISTORY") ?: arrayListOf()
        setContent {
            SimonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SecondScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .displayCutoutPadding(),
                        historyList = historyData // passo i dati ricevuti alla schermata
                    )
                }
            }
        }
    }
}

@Composable
fun SecondScreen(
    modifier: Modifier = Modifier,
    historyList: List<String> // parametro per ricevere la lista
) {
    // GIUSTIFICAZIONE LAYOUT: in questa schermata prediligo l'uso di Column e Row
    // poiché l'interfaccia presenta una struttura lineare molto semplice (un titolo
    // sopra una lista verticale, e testi allineati orizzontalmente all'interno delle righe).
    // in Jetpack Compose, l'uso di Column e Row per layout di questo tipo rappresenta
    // la best practice: rende il codice molto più leggibile, leggero e idiomatico
    // rispetto all'uso inutilmente verboso di un ConstraintLayout.

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // configurazione schermo
        val orientation = LocalConfiguration.current.orientation

        // percentuale di larghezza della colonna
        val widthFraction = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 0.9f else 1f

        // titolo
        Text(
            text = stringResource(R.string.history_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)
        )

        // lista dinamica
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyList) { sequence ->
                GameHistoryRow(sequence = sequence)
            }
        }
    }
}

@Composable
fun GameHistoryRow(sequence: String) { // riceve una stringa (es. "R, G, B")
    // calcolo della dimensione: se la stringa è vuota metto 0, altrimenti conto gli elementi divisi da virgola
    val count = if (sequence.isBlank()) 0 else sequence.split(",").size

    // riga che contiene le informazioni di una singola partita
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // numero di rettangoli premuti
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // sequenza di rettangoli premuti
        Text(
            text = sequence.ifEmpty { stringResource(R.string.none) },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f) // prende tutto lo spazio rimanente DOPO aver calcolato il testo a sx
                .padding(start = 16.dp) // tiene una distanza di sicurezza dal contatore
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SecondScreenPreview() {
    SimonTheme {
        // dati fittizi, servono solo alla preview di android studio
        val dummyData = listOf(
            "R, G, B",
            "Y, C, B, C, R",
            ""
        )

        SecondScreen(historyList = dummyData)
    }
}