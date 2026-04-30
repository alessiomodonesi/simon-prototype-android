package it.unipd.dei.esp2526.simon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao  // espone il DAO per permettere l'accesso ai dati

    companion object {
        // volatile garantisce che il valore di questa variabile sia sempre aggiornato per tutti i thread
        @Volatile
        private var INSTANCE: AppDatabase? =
            null // singleton per evitare di creare più istanze del database

        fun getDatabase(context: Context): AppDatabase {
            // se INSTANCE non è null, la ritorna, altrimenti crea il database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "simon_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}