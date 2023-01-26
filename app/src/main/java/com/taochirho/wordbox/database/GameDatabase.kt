package com.taochirho.wordbox.database


import android.content.Context
import androidx.room.*

import com.taochirho.wordbox.application.GAME_STATUS
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.internal.synchronized

import java.util.*

/**
 * A database that stores Games and the current game.
 * And a global method to get access to the database.
 *
 */

fun tileFromString(string: String): Tile {

    return if (string.substring(3).toInt() == TileState.IN_TRAY.ordinal) {
        Tile(string.substring(0, 1), TileState.IN_TRAY, TilePos(9, 9))
    } else {
        //  val s = string.substring(1, 3)
        //  Log.w("string.substring", s)
        val hash = string.substring(1, 3).toInt() - 10  // take away the 10 added in toString
        Tile(
            string.substring(0, 1),
            TileState.values()[string.substring(3).toInt()],
            TilePos(hash / 10, hash % 10)
        )
    }
}

fun stringFromTiles(tiles : Array<Tile>) : String {
    val sb = StringBuilder(tiles.size * 4)

    for (element in tiles) {
        sb.append(element.toString())
    }
    return sb.toString()
}

data class SentGame(
    val timeSet: Long,
    val gameFrom: String,
    val gameTag: String,
    val dateSaved: Date,
    val status: GAME_STATUS,
    val gameString: String
)

@Entity(tableName = "current_game")
internal data class CurrentGameEntity(
    @PrimaryKey val uid: Int,
    val tileCount: Int,
    val swappedTiles: Int,
    val timeSet: Long,
    val timeLeft: Long,
    val dateSaved: Date,
    val gameFrom: String,
    val gameTag: String,
    val status: GAME_STATUS,
    val gameTiles: Array<Tile>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CurrentGameEntity

        if (uid != other.uid) return false
        if (tileCount != other.tileCount) return false
        if (swappedTiles != other.swappedTiles) return false
        if (timeSet != other.timeSet) return false
        if (dateSaved != other.dateSaved) return false
        if (gameFrom != other.gameFrom) return false
        if (gameTag != other.gameTag) return false
        if (status != other.status) return false
        if (!gameTiles.contentEquals(other.gameTiles)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid
        result = 31 * result + tileCount
        result = 31 * result + swappedTiles
        result = 31 * result + timeSet.hashCode()
        result = 31 * result + dateSaved.hashCode()
        result = 31 * result + gameFrom.hashCode()
        result = 31 * result + gameTag.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + gameTiles.contentHashCode()
        return result
    }
}

@Entity(tableName = "saved_games")
internal data class GameEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    val tileCount: Int,
    val swappedTiles: Int,
    val timeSet: Long,
    val dateSaved: Date,
    val gameFrom: String,
    val gameTag: String,
    val status: GAME_STATUS,
    val gameTiles: Array<Tile>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameEntity

        if (uid != other.uid) return false
        if (tileCount != other.tileCount) return false
        if (swappedTiles != other.swappedTiles) return false
        if (timeSet != other.timeSet) return false
        if (dateSaved != other.dateSaved) return false
        if (gameFrom != other.gameFrom) return false
        if (gameTag != other.gameTag) return false
        if (status != other.status) return false
        if (!gameTiles.contentEquals(other.gameTiles)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid
        result = 31 * result + tileCount
        result = 31 * result + swappedTiles
        result = 31 * result + timeSet.hashCode()
        result = 31 * result + dateSaved.hashCode()
        result = 31 * result + gameFrom.hashCode()
        result = 31 * result + gameTag.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + gameTiles.contentHashCode()
        return result
    }
}

data class GameStatus(
    val uid: Int,
    val status: GAME_STATUS
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long): Date {
        return Date(value)
    }
    @TypeConverter
    fun dateToTimestamp(date: Date): Long {
        return date.time
    }
    @TypeConverter
    fun fromStringToArrayTiles(gameTiles: String): Array<Tile> {
        return Array(gameTiles.length / 4) { i ->
            tileFromString(gameTiles.substring(i * 4, (i * 4) + 4))
        }
    }
    @TypeConverter
    fun toStringFromArrayTiles(gameTiles: Array<Tile>): String {
        return stringFromTiles(gameTiles)
    }
}

@Dao
interface GameDao {
    @Insert(entity = GameEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: Game)

    @Update(entity = GameEntity::class)
    suspend fun updateGameStatus(gameStatus: GameStatus)

    @Query("SELECT * from saved_games WHERE uid = :uid")
    suspend fun getGameWithUid(uid: Int): Game

//    @Query("SELECT gameTiles from saved_games WHERE uid = :uid")
//    suspend fun getGameTilesWithUid(uid: Int): String?

    @Query("SELECT * FROM saved_games ORDER BY uid ASC")
    fun getAllGames(): Flow<List<Game>>

    @Delete(entity = GameEntity::class)
    suspend fun delete(game: Game)

    /*
        @Transaction
        suspend fun clearAndResetCount() {

            val currentGame = getGameWithUid(GameDatabase.CURRENTGAME)

            clear()
            resetAutoIncrement()
            insert(currentGame)
        }

        @Query("UPDATE `sqlite_sequence` SET `seq` = 1 WHERE `name` = 'saved_games'")
        fun resetAutoIncrement()
    */
    @Query("DELETE FROM saved_games")
    suspend fun clear()
}

@Dao
interface CurrentGameDao {

    @Query("SELECT * from current_game WHERE uid = 0")
    suspend fun getCurrent(): CurrentGame

    @Update(entity = CurrentGameEntity::class)
    suspend fun updateCurrentGame(game: CurrentGame)

}

@Database(
    entities = [GameEntity::class, CurrentGameEntity::class],
    version = 1,
    exportSchema = true
)

@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao
    abstract fun currentGameDao(): CurrentGameDao

    companion object {

        @Volatile
        private var INSTANCE: GameDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        fun getDatabase(
            context: Context
        ): GameDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "stored_games",
                )
                    .fallbackToDestructiveMigration()
                    //                 .addCallback(GameDatabaseCallback(scope))
                    .createFromAsset("wordboxinit.db")
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
