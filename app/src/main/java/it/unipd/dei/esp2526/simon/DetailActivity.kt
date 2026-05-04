package it.unipd.dei.esp2526.simon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import kotlin.getValue

class DetailActivity : ComponentActivity() {
    private val vm: ViewModel by viewModels() // inizializzo il ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recupero l'ID (di default metto -1 se non lo trova)
        val matchId = intent.getIntExtra("MATCH_ID", -1)

        setContent {
            SimonTheme {
                // stato per contenere il record caricato dal DB
                var record by remember { mutableStateOf<GameRecord?>(null) }

                // ogni volta che 'matchId' cambia, Compose esegue questo blocco
                // LaunchedEffect avvia una coroutine non appena la composizione inizia
                LaunchedEffect(matchId) {
                    if (matchId != -1)
                        record =
                            vm.getGameById(matchId) // chiamo la funzione dal ViewModel
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // se il record è stato caricato, mostro la schermata
                    // k?.let{…}, azione tra {} eseguita if k != null
                    record?.let { loadedRecord ->
                        DetailScreen(
                            modifier = Modifier
                                .padding(innerPadding)
                                .displayCutoutPadding(), // https://developer.android.com/develop/ui/views/layout/display-cutout
                            record = loadedRecord // passo l'intero record
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(
    modifier: Modifier = Modifier,
    record: GameRecord
) {
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
                    text = "${record.maxLength}", // campo dal db
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center, // centra la singola cifra nello spazio
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .widthIn(min = 40.dp) // larghezza minima per allineare 1 e 2 cifre
                )

                // sequenza di rettangoli premuti
                Text(
                    text = getColoredSequence(record), // chiamo la funzione implementata in HistoryActivity.kt
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
        // dati fittizi aggiornati al tipo GameRecord, servono solo alla preview
        val dummyData = GameRecord(id = 1, maxLength = 2, sequence = "R, G, B")
        DetailScreen(record = dummyData)
    }
}