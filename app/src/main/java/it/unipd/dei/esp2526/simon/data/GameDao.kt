package it.unipd.dei.esp2526.simon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GameDao {
    // inserisce una nuova partita
    // utilizzo "suspend" per eseguire l'operazione in modo asincrono
    // questo delega il lavoro di I/O ad un thread in background,
    // evitando di bloccare il Main Thread (UI).
    @Insert
    suspend fun insertGame(game: GameRecord)

    // recupera tutte le partite, ordinate dall'ultima alla prima
    @Query("SELECT * FROM games_history ORDER BY id DESC")
    suspend fun getAllGames(): List<GameRecord>

    // recupera una singola partita, utile per recupere i dettagli nella DetailActivity
    @Query("SELECT * FROM games_history WHERE id = :gameId")
    suspend fun getGameById(gameId: Int): GameRecord?
}