package it.unipd.dei.esp2526.simon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.unipd.dei.esp2526.simon.ui.theme.SimonTheme
import kotlin.text.ifEmpty

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recupero la stringa dall'intent usando la stessa chiave. se è null, assegna una stringa vuota
        val detailsData = intent.getStringExtra("MATCH_DETAILS") ?: ""

        setContent {
            SimonTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ThirdScreen(
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
fun ThirdScreen(
    modifier: Modifier = Modifier,
    matchDetails: String
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
            modifier = Modifier
                .weight(.5f)
                .padding(bottom = 20.dp, top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(.5f))

        // dettagli della partita selezionata
        Text(
            text = matchDetails.ifEmpty { stringResource(R.string.none) }, // se vuota, testo "None"
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center, // allineo la stringa al centro
            modifier = Modifier
                .weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ThirdScreenPreview() {
    SimonTheme {
        // dati fittizi, servono solo alla preview di android studio
        ThirdScreen(matchDetails = "R, G, B")
    }
}