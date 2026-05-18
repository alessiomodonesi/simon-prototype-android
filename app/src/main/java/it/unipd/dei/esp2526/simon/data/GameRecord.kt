package it.unipd.dei.esp2526.simon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * rappresenta un singolo record nella tabella "games_history" del database,
 * memorizzando i dati riassuntivi di una partita conclusa.
 * l'uso di una data class ottimizza il confronto (diffing) nelle liste di Compose.
 *
 * @property id chiave primaria univoca. il valore di default 0 viene ignorato in fase di INSERT per delegare l'autoincremento a SQLite.
 * @property maxLength la lunghezza massima della sequenza riprodotta correttamente dall'utente.
 * @property sequence la sequenza completa (inclusiva di errori) salvata come stringa separata da virgole (es. "R, G, B").
 */
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