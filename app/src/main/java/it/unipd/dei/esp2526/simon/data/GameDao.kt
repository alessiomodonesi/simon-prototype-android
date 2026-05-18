package it.unipd.dei.esp2526.simon.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) per l'entità GameRecord.
 * fornisce i metodi per interagire con la tabella "games_history" nel database SQLite.
 * le operazioni di singola scrittura/lettura sono esposte come funzioni "suspend" per
 * un'esecuzione asincrona sicura (Main-safe), mentre la lettura globale restituisce
 * un "Flow" per l'osservazione reattiva dei dati.
 */
@Dao
interface GameDao {
    /**
     * inserisce una nuova partita.
     * utilizzo "suspend" per indicare che l'operazione è asincrona e può essere sospesa.
     * Room implementa queste funzioni in modo "Main-safe", delegando autonomamente
     * il lavoro di I/O a un thread in background per non bloccare la UI.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE) // se il record esiste già, annulla l'inserimento per evitare crash e proteggere i vecchi dati
    suspend fun insertGame(game: GameRecord)

    /**
     * recupera tutte le partite, ordinate dall'ultima alla prima.
     * il Flow è di per sé un "tubo" reattivo e asincrono.
     * creare il tubo è un'operazione istantanea (quindi non serve suspend);
     * è l'ascolto dei dati all'interno del tubo che avverrà in modo asincrono nel tempo.
     * inoltre, Room verifica la sintassi SQL e il tipo di ritorno a compile-time;
     * i due punti ':' mappano in modo sicuro il parametro della funzione
     * @see "https://developer.android.com/kotlin/flows"
     */
    @Query("SELECT * FROM games_history ORDER BY id DESC")
    fun getAllGames(): Flow<List<GameRecord>>

    /**
     * recupera una singola partita, utile per recupere i dettagli nella DetailActivity.
     * utilizzo "suspend" per indicare che l'operazione è asincrona e può essere sospesa.
     * Room implementa queste funzioni in modo "Main-safe", delegando autonomamente
     * il lavoro di I/O a un thread in background per non bloccare la UI.
     */
    @Query("SELECT * FROM games_history WHERE id = :gameId")
    suspend fun getGameById(gameId: Int): GameRecord?
}