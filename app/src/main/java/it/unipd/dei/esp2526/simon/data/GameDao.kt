package it.unipd.dei.esp2526.simon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameDao {
    // inserisce una nuova partita
    @Insert
    fun insertGame(game: GameRecord)

    // recupera tutte le partite, ordinate dall'ultima alla prima
    @Query("SELECT * FROM games_history ORDER BY id DESC")
    fun getAllGames(): List<GameRecord>
}