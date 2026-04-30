package it.unipd.dei.esp2526.simon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import kotlin.text.ifEmpty
import kotlin.text.isBlank
import kotlin.text.split

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recupero la stringa dall'intent usando la stessa chiave. se è null, assegna una stringa vuota
        val detailsData = intent.getStringExtra("MATCH_DETAILS") ?: ""

        setContent {
            SimonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DetailScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .displayCutoutPadding(), // https://developer.android.com/develop/ui/views/layout/display-cutout
                        matchDetails = detailsData // passo i dati ricevuti alla schermata
                    )
                }
            }
        }
    }
}

@Composable
fun DetailScreen(
    modifier: Modifier = Modifier,
    matchDetails: String
) {
    // calcolo della dimensione: se la stringa è vuota metto 0, altrimenti conto gli elementi divisi da virgola
    val count = if (matchDetails.isBlank()) 0 else matchDetails.split(",").size

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // titolo
        Text(
            text = stringResource(R.string.details_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
        )

        // contenitore principale per i dettagli (gestisce sfondo e spazio)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // prende tutto lo spazio sotto il titolo
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                // permette lo scorrimento verticale se la stringa è molto lunga (si scorre l'intera area)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // riga che contiene i dettagli della partita selezionata
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // numero di rettangoli premuti
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center, // centra la singola cifra nello spazio
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .widthIn(min = 40.dp) // larghezza minima per allineare 1 e 2 cifre
                )

                // Sequenza di rettangoli premuti
                Text(
                    text = matchDetails.ifEmpty { stringResource(R.string.none) },  // se vuota, testo "None"
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start, // allineo la stringa a sinistra
                    lineHeight = 28.sp,
                    modifier = Modifier.weight(1f) // usa tutto lo spazio a destra del numero
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    SimonTheme {
        // dati fittizi, servono solo alla preview di android studio
        DetailScreen(matchDetails = "R, G, B")
    }
}