package it.unipd.dei.esp2526.simon.model

data class GameUiState(
    val computerSequence: List<String> = emptyList(), // stato per tenere traccia della sequenza generata dal computer
    val userSequence: List<String> = emptyList(), // stato per tenere traccia della sequenza riprodotta dall'utente
    val isGameRunning: Boolean = false, // stato per capire se il gioco è in corso o meno
    val isComputerPlaying: Boolean = false, // stato per capire se il computer ha il comando
    val isPaused: Boolean = false, // stato per gestire la pausa durante il turno del computer
    val isGameOver: Boolean = false, // stato per gestire la sconfitta dell'utente
    val activeColor: String? = null, // stato per il colore attualmente illuminato
    val computerPlaybackIndex: Int = 0 // stato per l'indice della sequenza del computer
)