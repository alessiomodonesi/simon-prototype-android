package it.unipd.dei.esp2526.simon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import it.unipd.dei.esp2526.simon.utils.getColoredSequence
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
                            record = loadedRecord, // passo l'intero record
                            modifier = Modifier
                                .padding(innerPadding)
                                .displayCutoutPadding() // https://developer.android.com/develop/ui/views/layout/display-cutout

                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailScreen(
    record: GameRecord,
    modifier: Modifier = Modifier
) {
    // calcola i dati extra partendo dalla stringa del db
    val sequenceItems = if (record.sequence.isBlank()) emptyList() else record.sequence.split(", ")
    val totalLength = sequenceItems.size

    // gli errori sono i colori che il computer ha proposto ma che l'utente non ha indovinato
    val errorCount = if (totalLength > 0) totalLength - record.maxLength else 0

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
                // permette lo scorrimento verticale
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp) // spazio uniforme tra le sezioni
        ) {
            // statistiche: 3 x StatItem()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(title = stringResource(R.string.round_str), value = "${record.maxLength}")
                StatItem(title = stringResource(R.string.total_colors_str), value = "$totalLength")
                StatItem(
                    title = stringResource(R.string.errors_str),
                    value = "$errorCount",
                    isError = errorCount > 0 // colora di rosso se c'è un errore
                )
            }

            // linea di separazione
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // sequenza visiva
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.computer_sequence_str),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = getColoredSequence(record), // chiamo la funzione implementata in GameUtils.kt
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    isError: Boolean = false
) {
    // componente riutilizzabile per mostrare una singola statistica
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            // se è un errore, usa il colore rosso, altrimenti il colore primario
            color = if (isError) Color.Red else MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    SimonTheme {
        // dati fittizi aggiornati al tipo GameRecord, servono solo alla preview
        val dummyData = GameRecord(id = 1, maxLength = 5, sequence = "R, G, B, Y, C, M")
        DetailScreen(record = dummyData)
    }
}