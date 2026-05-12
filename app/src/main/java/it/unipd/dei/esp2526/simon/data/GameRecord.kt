package it.unipd.dei.esp2526.simon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// l'uso di una data class genera in automatico metodi come equals() e hashCode(),
// ottimizzando il "diffing" (confronto) nelle liste di Compose
@Entity(tableName = "games_history")
data class GameRecord(
    // Room mappa questo campo sul ROWID di SQLite:
    // il valore 0 di default viene ignorato durante l'INSERT per delegare l'autoincremento al DB
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // chiave primaria univoca per ogni partita

    @ColumnInfo(name = "max_length")
    val maxLength: Int, // lunghezza massima riprodotta correttamente

    @ColumnInfo(name = "sequence")
    val sequence: String // sequenza salvata come stringa separata da virgole (es. "R, G, B")
)