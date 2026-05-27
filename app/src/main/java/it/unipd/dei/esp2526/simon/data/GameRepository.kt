package it.unipd.dei.esp2526.simon.data

import kotlinx.coroutines.flow.Flow

/**
 * repository che astrae l'accesso ai dati dal DAO.
 * gestisce le operazioni sui dati fornendo un'API pulita al resto dell'app,
 * separando la sorgente dei dati (Room, Rete, ecc.) dal ViewModel.
 */
class GameRepository(private val gameDao: GameDao) {

    // ottiene il Flow contenente tutte le partite direttamente dal DAO
    val allGames: Flow<List<GameRecord>> = gameDao.getAllGames()

    /** metodo suspend per inserire una partita nel database */
    suspend fun insertGame(game: GameRecord) {
        gameDao.insertGame(game)
    }

    /** metodo suspend per recuperare i dettagli di una singola partita */
    suspend fun getGameById(id: Int): GameRecord? {
        return gameDao.getGameById(id)
    }
}