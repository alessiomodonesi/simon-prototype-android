package it.unipd.dei.esp2526.simon.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [GameRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    // espone il DAO per permettere l'accesso ai dati
    abstract fun gameDao(): GameDao
}