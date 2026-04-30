package it.unipd.dei.esp2526.simon

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import kotlin.getValue

class HistoryActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels() // inizializzo il ViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /**
         * nota sulla navigazione: non è necessario implementare alcun pulsante "Indietro" personalizzato.
         * il sistema Android inserisce questa activity in cima al Back Stack.
         * quando l'utente utilizza il tasto "Back" di sistema (fisico, virtuale o gesture),
         * questa activity viene distrutta automaticamente (pop) e l'utente
         * ritorna alla GameActivity sottostante.
         */
        setContent {
            SimonTheme {
                // raccoglie i dati dal database in tempo reale
                val historyList by gameViewModel.history.collectAsState()
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    // aggiungo il Floating Action Button in basso a destra
                    // https://developer.android.com/develop/ui/compose/components/fab
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                // creo l'intent esplicito per avviare GameActivity
                                val mainIntent =
                                    Intent(this, GameActivity::class.java)
                                this.startActivity(mainIntent)
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                stringResource(R.string.start_str)
                            )
                        }
                    }
                ) { innerPadding ->
                    HistoryScreen(
                        // funzione lambda per il click sulla Row della LazyColumn
                        onRowClick = { sequence ->
                            // creo l'intent esplicito per avviare detailActivity
                            val detailIntent =
                                Intent(this, DetailActivity::class.java).apply {
                                    // passo la sequenza come parametro extra
                                    putExtra("MATCH_DETAILS", sequence)
                                    // stampo i dati a scopo di test (v = verbose)
                                    Log.v("MATCH_DETAILS", sequence)
                                }

                            // lancio l'activity passandogli l'intent, this è il Context
                            this.startActivity(detailIntent)
                        },
                        modifier = Modifier
                            .padding(innerPadding)
                            .displayCutoutPadding(), // https://developer.android.com/develop/ui/views/layout/display-cutout
                        historyList = historyList // passo i dati ricevuti alla schermata
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    onRowClick: (String) -> Unit,
    historyList: List<GameRecord> // ora riceve GameRecord dal DB
) {
    /**
     * giustificazione layout: in questa schermata prediligo l'uso di Column e Row,
     * poiché l'interfaccia presenta una struttura lineare molto semplice (un titolo
     * sopra una lista verticale, e testi allineati orizzontalmente all'interno delle righe).
     * in Jetpack Compose, l'uso di Column e Row per layout di questo tipo rappresenta
     * la best practice: rende il codice molto più leggibile, leggero e idiomatico
     * rispetto all'uso inutilmente verboso di un ConstraintLayout.
     */
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // configurazione schermo
        val orientation = LocalConfiguration.current.orientation

        // percentuale di larghezza della colonna, in la modalità landscape viene frazionata al 90%
        val widthFraction = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 0.9f else 1f

        // titolo
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp, top = 8.dp)
        )

        // lista dinamica implementata con LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyList) { record ->
                GameHistoryRow(
                    record = record, // passo l'intero record
                    onClick = { onRowClick(record.sequence) } // passo la sequenza alla callback
                )
            }
        }
    }
}

@Composable
private fun GameHistoryRow(
    record: GameRecord, // usa il GameRecord
    onClick: () -> Unit
) {
    // riga che contiene le informazioni di una singola partita
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp)) // utile per contenere l'effetto "ripple" del click
            .clickable { onClick() } // chiamo la callback passata come parametro
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // numero di rettangoli premuti
        Text(
            text = "$record.maxLength", // campo dal db
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center, // centra la singola cifra nello spazio
            modifier = Modifier.widthIn(min = 30.dp) // larghezza minima per allineare 1 e 2 cifre
        )

        // sequenza di rettangoli premuti
        Text(
            text = record.sequence.ifEmpty { stringResource(R.string.none) }, // se vuota, testo "None"
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis, // se la sequenza è troppo lunga, viene troncata
            textAlign = TextAlign.End, // allineo la stringa a destra
            modifier = Modifier
                .weight(1f) // prende tutto lo spazio rimanente DOPO aver calcolato il testo a sx
                .padding(start = 16.dp) // tiene una distanza di sicurezza dal contatore
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    SimonTheme {
        // dati fittizi aggiornati al tipo GameRecord, servono solo alla preview
        val dummyData = listOf(
            GameRecord(id = 1, maxLength = 2, sequence = "R, G, B"),
            GameRecord(id = 2, maxLength = 4, sequence = "Y, C, B, C, R"),
            GameRecord(id = 3, maxLength = 0, sequence = "")
        )

        HistoryScreen(
            onRowClick = {},
            historyList = dummyData
        )
    }
}