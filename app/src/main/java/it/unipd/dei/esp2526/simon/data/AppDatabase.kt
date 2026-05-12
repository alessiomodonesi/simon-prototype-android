package it.unipd.dei.esp2526.simon.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao  // espone il DAO per permettere l'accesso ai dati

    companion object {
        @Volatile // garantisce che il valore di questa variabile sia sempre aggiornato per tutti i thread
        private var INSTANCE: AppDatabase? =
            null // singleton per evitare di creare più istanze del database

        fun getDatabase(context: Context): AppDatabase {
            // se INSTANCE non è null, la ritorna, altrimenti crea il database
            if (INSTANCE == null) {
                // il blocco synchronized previene race condition,
                // assicurando che un solo thread alla volta possa eseguire l'inizializzazione
                synchronized(this) {
                    val instance = Room.databaseBuilder(
                        // l'uso del context globale dell'applicazione previene i memory leak
                        // che si verificherebbero passando il context di un'Activity
                        context.applicationContext,
                        AppDatabase::class.java,
                        "simon_db"
                    ).build()
                    INSTANCE = instance
                }
            }
            return INSTANCE!! // double-bang operator: lancia l’eccezione if INSTANCE = null
        }
    }
}