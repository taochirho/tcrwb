package com.taochirho.testvm.database

//import kotlinx.coroutines.flow.Flow
import android.content.Context
import androidx.room.*
import com.taochirho.testvm.application.GAME_STATUS

import kotlinx.coroutines.CoroutineScope
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
        val hash = string.substring(1, 3).toInt() - 10  // take away the 10 added in toString
        Tile(
            string.substring(0, 1),
            TileState.values()[string.substring(3).toInt()],
            TilePos(hash / 10, hash % 10)
        )
    }
}

data class SentGame(
    val timeLeft: Long,
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
    val timeLeft: Long,
    val dateSaved: Date,
    val gameFrom: String,
    val gameTag: String,
    val status: GAME_STATUS,
    val gameTiles: Array<Tile>
)


@Entity(tableName = "saved_games")
internal data class GameEntity(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    val tileCount: Int,
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

        other as GameEntity

        if (uid != other.uid) return false
        if (tileCount != other.tileCount) return false
        if (timeLeft != other.timeLeft) return false
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
        result = 31 * result + timeLeft.hashCode()
        result = 31 * result + dateSaved.hashCode()
        result = 31 * result + gameFrom.hashCode()
        result = 31 * result + gameTag.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + gameTiles.contentHashCode()
        return result
    }
}

data class GameStatus (
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
        val sb = StringBuilder(gameTiles.size * 4)

        for (element in gameTiles) {
            sb.append(element.toString())
        }
        return sb.toString()
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
    }*/

    @Query("UPDATE `sqlite_sequence` SET `seq` = 1 WHERE `name` = 'saved_games'")
    fun resetAutoIncrement()

    @Query("DELETE FROM saved_games")
    suspend fun clear()

}

@Dao
interface CurrentGameDao {

    @Query("SELECT * from current_game WHERE uid = 0")
    suspend fun getCurrent(): Game

    @Insert(entity = CurrentGameEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrent(game: Game)

    @Update(entity = CurrentGameEntity::class)
    suspend fun updateCurrentGame(game: Game)

    @Query("DELETE FROM current_game")
    suspend fun deleteCurrent()
}


@Database(
    entities = [GameEntity::class, CurrentGameEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class GameDatabase : RoomDatabase() {

    abstract fun gameDao(): GameDao
    abstract fun currentGameDao(): CurrentGameDao
/*
    private class GameDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.currentGameDao())
                }
            }
        }

        suspend fun populateDatabase(currentGameDao: CurrentGameDao) {
            // Delete all content here.

            Log.w("GameDatabase","populateDatabase")
            currentGameDao.deleteCurrent()
            currentGameDao.insertCurrent(
                Game(
                    0,
                    15,
                    900000L, // default 15 minutes (and 1 sec)
                    Date(),
                    "Word Box",
                    GAME_STATUS.C,
                    arrayOf(
                        Tile("P", TileState.RIGHT, TilePos(0, 0)),
                        Tile("E", TileState.RIGHT, TilePos(0, 1)),
                        Tile("L", TileState.RIGHT, TilePos(0, 2)),
                        Tile("C", TileState.RIGHT, TilePos(0, 3)),
                        Tile("O", TileState.RIGHT, TilePos(0, 4)),
                        Tile("M", TileState.RIGHT, TilePos(0, 5)),
                        Tile("E", TileState.RIGHT, TilePos(0, 6)),
                        Tile("T", TileState.NEARLY_RIGHT, TilePos(2, 1)),
                        Tile("W", TileState.WRONG, TilePos(3, 0)),
                        Tile("O", TileState.WRONG, TilePos(3, 1)),
                        Tile("R", TileState.WRONG, TilePos(3, 2)),
                        Tile("D", TileState.WRONG, TilePos(3, 3)),
                        Tile("B", TileState.WRONG, TilePos(3, 4)),
                        Tile("O", TileState.WRONG, TilePos(4, 5)),
                        Tile("X", TileState.WRONG, TilePos(5, 6)),
                    )
                )
            )

        }
    }*/

    companion object {

        @Volatile
        private var INSTANCE: GameDatabase? = null

        @OptIn(InternalCoroutinesApi::class)
        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): GameDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "stored_games",
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // Migration is not part of this codelab.
                    .fallbackToDestructiveMigration()
                    //                   .addCallback(GameDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}


