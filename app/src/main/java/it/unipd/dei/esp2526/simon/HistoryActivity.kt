package it.unipd.dei.esp2526.simon

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import it.unipd.dei.esp2526.simon.data.GameRecord
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import it.unipd.dei.esp2526.simon.utils.getColoredSequence
import kotlin.getValue

class HistoryActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels() // inizializzo il View Model

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimonTheme {
                /**
                 * si iscrive allo StateFlow del ViewModel convertendolo in uno stato di Compose.
                 * quando il database Room si aggiorna, il flusso emette i nuovi dati e Compose
                 * "ricompone" (ridisegna) automaticamente l'interfaccia utente in tempo reale.
                 * l'uso del delegato "by" estrae comodamente il valore in una List<GameRecord>
                 */
                val historyList by vm.history.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // grandezza del cutout
                    val layoutDirection = LocalLayoutDirection.current
                    val cutout = WindowInsets.displayCutout.asPaddingValues()

                    // valore massimo tra lato destro e sinistro
                    val symmetricCutout = max(
                        cutout.calculateLeftPadding(layoutDirection),
                        cutout.calculateRightPadding(layoutDirection)
                    )

                    HistoryScreen(
                        // funzione lambda per il click sulla Row della LazyColumn
                        onRowClick = { record ->
                            // creo l'intent esplicito per avviare detailActivity:
                            // la scope function 'apply' agisce sul ricevitore (l'Intent),
                            // permettendo di configurare i bundle extra senza riassegnare variabili
                            val detailIntent =
                                Intent(this, DetailActivity::class.java).apply {
                                    // passo la sequenza come parametro extra
                                    putExtra("MATCH_ID", record.id)
                                    // stampo i dati a scopo di test (v = verbose)
                                    Log.v("MATCH_ID", "id: ${record.id}")
                                }

                            // lancio l'activity passandogli l'intent, this è il Context
                            this.startActivity(detailIntent)
                        },
                        // funzione lambda per il click sul FAB "New Game"
                        onNewGameClick = {
                            // creo l'intent esplicito per avviare GameActivity
                            val mainIntent = Intent(this, GameActivity::class.java)
                            this.startActivity(mainIntent)
                        },
                        historyList = historyList, // passo i dati ricevuti alla schermata
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(horizontal = symmetricCutout)
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    onNewGameClick: () -> Unit,
    onRowClick: (GameRecord) -> Unit,
    historyList: List<GameRecord>, // ora riceve GameRecord dal DB
    modifier: Modifier = Modifier
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp, top = 8.dp),
            contentAlignment = Alignment.Center // centra il contenuto principale (il titolo)
        ) {
            // titolo
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.15.sp,
                textAlign = TextAlign.Center
            )

            // FAB "New Game"
            SmallFloatingActionButton(
                onClick = onNewGameClick,
                modifier = Modifier.align(Alignment.CenterEnd),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.new_game_str)
                )
            }
        }

        // lista dinamica implementata con LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(historyList) { record ->
                GameHistoryRow(
                    record = record, // passo l'intero record
                    onClick = { onRowClick(record) } // passo la sequenza alla callback
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
            text = "${record.maxLength}", // campo dal db
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center, // centra la singola cifra nello spazio
            // vincolo intrinseco che stabilizza l'allineamento orizzontale dei nodi adiacenti, indipendentemente dal numero di cifre
            modifier = Modifier.widthIn(min = 30.dp) // larghezza minima per allineare 1 e 2 cifre
        )

        // sequenza di rettangoli premuti
        Text(
            text = getColoredSequence(record),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            lineHeight = 24.sp,
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
            onNewGameClick = {},
            onRowClick = {},
            historyList = dummyData
        )
    }
}