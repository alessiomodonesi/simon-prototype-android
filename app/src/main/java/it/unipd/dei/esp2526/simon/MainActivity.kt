package it.unipd.dei.esp2526.simon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    // onEndGame: (List<String>) -> Unit // callback per passare alla Schermata 2
) {
    // stato della sequenza (salvato)
    var currentSequence by rememberSaveable { mutableStateOf(listOf<String>()) }

    // trasformiamo la lista in una stringa separata da virgole
    val displayText = currentSequence.joinToString(", ")

    ConstraintLayout(modifier = modifier.fillMaxSize()) { // interfaccia utente
        val (textScrollArea, btnCancel, btnEndGame) = createRefs()

        // bottone "Cancella"
        Button(
            modifier = Modifier.constrainAs(btnCancel) {
                top.linkTo(textScrollArea.bottom, margin = 16.dp)
                start.linkTo(parent.start, margin = 16.dp)
            },onClick = {
                currentSequence = emptyList() // azzera la sequenza
            }
        ) {
            Text(text = stringResource(R.string.cancel_str))
        }

        // bottone "Fine partita"
        Button(
            modifier = Modifier.constrainAs(btnEndGame) {
                top.linkTo(textScrollArea.bottom, margin = 16.dp)
                end.linkTo(parent.end, margin = 16.dp)
            },
            onClick = {
                // salvo la sequenza finale per poterla inviare alla seconda schermata
                val finalSequence = currentSequence.toList()
                currentSequence = emptyList() // svuota l'area di testo
                // onEndGame(finalSequence) // invia i dati e cambia schermata
            }
        ) {
            Text(text = stringResource(R.string.endgame_str))
        }

        // area di testo multiriga
        Text(
            text = displayText,
            modifier = Modifier
                .constrainAs(textScrollArea) {
                    top.linkTo(parent.top, margin = 16.dp)
                    start.linkTo(parent.start, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                    width = Dimension.fillToConstraints
                }
                .heightIn(min = 100.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
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