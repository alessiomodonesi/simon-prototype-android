package it.unipd.dei.esp2526.simon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "games_history")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // chiave primaria univoca per ogni partita

    @ColumnInfo(name = "sequence")
    val sequence: String // sequenza salvata come stringa separata da virgole (es. "R, G, B")
)