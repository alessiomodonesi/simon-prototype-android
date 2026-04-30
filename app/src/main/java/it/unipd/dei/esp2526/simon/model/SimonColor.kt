package it.unipd.dei.esp2526.simon.model

import androidx.compose.ui.graphics.Color

/**
 * struttura dati di supporto per mappare il colore visivo alla sua etichetta di logica.
 *
 * @param name nome esteso del colore (es. "Red")
 * @param color oggetto Color di Compose per renderizzare lo sfondo del rettangolo
 * @param label la singola lettera identificativa in inglese da inserire nella sequenza (es. "R")
 */
data class SimonColor(val name: String, val color: Color, val label: String)

/** lista dei 6 colori specifici richiesti dalla consegna.
 * vengono istanziati qui staticamente per non dipendere dai file strings.xml
 * ed evitare traduzioni accidentali delle etichette (label).
 */
val simonColors = listOf(
    SimonColor("Red", Color.Red, "R"),
    SimonColor("Green", Color.Green, "G"),
    SimonColor("Blue", Color.Blue, "B"),
    SimonColor("Magenta", Color.Magenta, "M"),
    SimonColor("Yellow", Color.Yellow, "Y"),
    SimonColor("Cyan", Color.Cyan, "C")
)