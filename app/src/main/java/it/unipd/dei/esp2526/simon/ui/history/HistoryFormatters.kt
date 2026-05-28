package it.unipd.dei.esp2526.simon.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import it.unipd.dei.esp2526.simon.R
import it.unipd.dei.esp2526.simon.data.GameRecord

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

            // cerco l'indice del primo elemento che inizia con "*"
            // utilizzo coerceAtLeast(0) come salvaguardia minimale di sicurezza per evitare crash
            val errorIndex = items.indexOfFirst { it.startsWith("*") }.coerceAtLeast(0)

            // separo i colori a partire dal punto dell'errore
            val correctItems = items.take(errorIndex).joinToString(", ")
            val wrongItems = items.drop(errorIndex).joinToString(", ") { it.removePrefix("*") }

            // aggiungo la parte corretta con il colore di default
            append(correctItems)

            // aggiungo la virgola separatrice se entrambe le parti esistono
            if (correctItems.isNotEmpty() && wrongItems.isNotEmpty())
                append(", ")

            // aggiungo la parte errata/non raggiunta in colore rosso
            withStyle(style = SpanStyle(color = Color.Red)) {
                append(wrongItems)
            }
        }
    }
}