package it.unipd.dei.esp2526.simon.ui.detail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.unipd.dei.esp2526.simon.R
import it.unipd.dei.esp2526.simon.data.*
import it.unipd.dei.esp2526.simon.ui.history.getColoredSequence
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import kotlin.getValue

class DetailActivity : ComponentActivity() {
    private val vm: DetailViewModel by viewModels { // inizializzo il View Model
        val database =
            AppDatabase.getDatabase(this.applicationContext) // utilizzo il singleton getDatabase() invece di chiamare Room.databaseBuilder
        val repository =
            GameRepository(database.gameDao()) // inizializzo il Repository con il DAO
        DetailVMFactory(repository) // chiamo il costruttore
    }

    companion object {
        // chiave costante usata per passare l'ID della partita tramite intent in modo sicuro
        const val EXTRA_MATCH_ID = "MATCH_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // stato per contenere il record caricato dal DB
            val record by vm.uiState.collectAsStateWithLifecycle()

            SimonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // se il record è stato caricato, mostro la schermata
                    // record?.let { ... }, azione eseguita se record non è null
                    record?.let { loadedRecord ->
                        // grandezza del cutout:
                        // recupera la direzione (LTR o RTL) dal CompositionLocal,
                        // essenziale per mappare correttamente gli insets asimmetrici del notch
                        val layoutDirection = LocalLayoutDirection.current
                        val cutout = WindowInsets.displayCutout.asPaddingValues()

                        // valore massimo tra lato destro e sinistro
                        val symmetricCutout = max(
                            cutout.calculateLeftPadding(layoutDirection),
                            cutout.calculateRightPadding(layoutDirection)
                        )

                        DetailScreen(
                            record = loadedRecord, // passo l'intero record
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(horizontal = symmetricCutout)
                        )
                    }
                }
            }
        }
    }
}

/**
 * schermata di dettaglio che mostra le statistiche e la sequenza visiva di una partita specifica.
 *
 * @param record l'oggetto GameRecord contenente i dati della partita da visualizzare.
 * @param modifier modificatore per gestire layout, padding e insets del notch.
 */
@Composable
fun DetailScreen(
    record: GameRecord,
    modifier: Modifier = Modifier
) {
    // calcola i dati extra partendo dalla stringa del DB:
    // deserializza la stringa flat del DB allocando dinamicamente una List in base al delimitatore testuale
    val sequenceItems = if (record.sequence.isBlank()) emptyList() else record.sequence.split(", ")
    val totalLength = sequenceItems.size
    
    // gli errori sono i colori dal punto dell'errore (identificato dall'asterisco "*") fino alla fine della sequenza
    val errorIndex = sequenceItems.indexOfFirst { it.startsWith("*") }.coerceAtLeast(0)
    val errorCount = if (totalLength > 0) totalLength - errorIndex else 0

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

                // sequenza di rettangoli premuti
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