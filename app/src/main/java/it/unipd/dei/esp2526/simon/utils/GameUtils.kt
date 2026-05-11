package it.unipd.dei.esp2526.simon.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import it.unipd.dei.esp2526.simon.R
import it.unipd.dei.esp2526.simon.data.GameRecord
import kotlinx.coroutines.delay

/**
 * gestisce il feedback visivo e uditivo di un singolo colore.
 * centralizza la logica per evitare duplicazioni tra turno PC e click Utente.
 */
suspend fun playColorFeedback(
    colorLabel: String,
    durationMs: Long,
    onColorActive: (String?) -> Unit
) {
    onColorActive(colorLabel) // accende visivamente il colore

    // lancia la riproduzione audio
    val freq = SoundManager.getFrequencyForColor(colorLabel)
    SoundManager.playTone(frequency = freq)

    delay(durationMs) // mantiene acceso il colore per la durata del suono
    onColorActive(null) // spegne il colore
}

/**
 * sequenza divisa in corretta/errata e colorata, come da specifiche
 * @see "https://developer.android.com/reference/kotlin/androidx/compose/ui/text/AnnotatedString"
 */
@Composable
fun getColoredSequence(record: GameRecord): AnnotatedString {
    return buildAnnotatedString {
        if (record.sequence.isBlank()) {
            append(stringResource(R.string.none)) // se vuota, testo "None"
        } else {
            // divido la sequenza in una lista di stringhe
            val items = record.sequence.split(", ")

            // i primi 'maxLength' elementi sono corretti
            val correctItems = items.take(record.maxLength).joinToString(", ")
            // i restanti elementi sono sbagliati
            val wrongItems = items.drop(record.maxLength).joinToString(", ")

            // aggiungo la parte corretta con il colore di default
            append(correctItems)

            // aggiungo la virgola separatrice se entrambe le parti esistono
            if (correctItems.isNotEmpty() && wrongItems.isNotEmpty())
                append(", ")

            // aggiungo la parte sbagliata con un colore diverso
            if (wrongItems.isNotEmpty()) {
                withStyle(style = SpanStyle(color = Color.Red)) {
                    append(wrongItems)
                }
            }
        }
    }
}