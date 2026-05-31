package it.unipd.dei.esp2526.simon.ui.game

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * GameState = (isGameRunning, isComputerPlaying, isPaused, isGameOver)
 *
 * IDLE = (false, false, false, false)
 *
 * COMPUTER_TURN = (true, true, false, false)
 *
 * PLAYER_TURN = (true, false, false, false)
 *
 * COMPUTER_PAUSED = (true, true, true, false)
 *
 * GAME_OVER = (false, false, false, true)
 */
enum class GameState {
    IDLE, // stato di default, prima della partita
    COMPUTER_TURN, // stato per capire se il computer ha il comando
    PLAYER_TURN, // stato per capire se il giocatore ha il comando
    
    COMPUTER_PAUSED, // stato per gestire la pausa durante il turno del computer
    GAME_OVER // stato per gestire la sconfitta dell'utente
}

/**
 * struttura dati di supporto per gestire gli stati della partita.
 * utilizza Parcelable (tramite l'annotazione @Parcelize) per garantire
 * una serializzazione estremamente efficiente e a bassa latenza all'interno
 * dei Bundle di sistema.
 */
@Parcelize
data class GameUiState(
    val gameState: GameState = GameState.IDLE,
    val computerSequence: List<String> = emptyList(), // stato per tenere traccia della sequenza generata dal computer
    val userSequence: List<String> = emptyList(), // stato per tenere traccia della sequenza riprodotta dall'utente
    val activeColor: String? = null, // stato per il colore attualmente illuminato
    val computerPlaybackIndex: Int = 0 // stato per l'indice della sequenza del computer
) : Parcelable