package com.taochirho.wordbox.database

import android.os.Parcelable
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.application.TAG_SORTED_COUNT
import com.taochirho.wordbox.application.TAG_SHUFFLED_COUNT
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.flow.Flow

import java.util.*

data class Game(
    val uid: Int,
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

        other as Game

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

data class Tile(
    var letter: String? = null,
    var state: TileState,
    var tilePos: TilePos = TilePos(0, 0)
) {

    override fun toString(): String  {
    return if (state == TileState.IN_TRAY) {
            this.letter + "99" + state.ordinal.toString()
        } else {
            this.letter + (this.hashCode() + 10).toString() + state.ordinal.toString() // add 10 so that all the numbers have 2 digits
        }

    }

    override fun hashCode(): Int {
        //this is essential for the comparison of tiles in the onGetSentenceSuggestions hash sets in score grid to work.  With the default intersection is always empty.
        return (this.tilePos.row * 10) + tilePos.col
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tile

        if (letter != other.letter) return false
        if (state != other.state) return false
        if (tilePos != other.tilePos) return false

        return true
    }
}

@Parcelize
data class TilePos(var row: Int, var col: Int, var trayIndex: Int = -1) : Parcelable

internal class Line(val word: String, val tiles: Set<Tile>) {

    override fun toString(): String {
        return "Line{" +
                word + '\'' +
                ", from " + tiles.toString() +
                '}'
    }
}


enum class TileState { EMPTY, RIGHT, NEARLY_RIGHT, WRONG, IN_TRAY, ENTERED, LETTER_ENTERED;}

/*enum class TileState (val ts: String) {
    EMPTY("0"), RIGHT("1") , NEARLY_RIGHT("2"), WRONG("3"), IN_TRAY("4"), ENTERED("5"), LETTER_ENTERED("6");

    fun fromTS(s: String): TileState {
        return when (s) {
            EMPTY.ts -> EMPTY
            RIGHT.ts -> RIGHT
            NEARLY_RIGHT.ts -> NEARLY_RIGHT
            WRONG.ts -> WRONG
            IN_TRAY.ts -> IN_TRAY
            ENTERED.ts -> ENTERED
            LETTER_ENTERED.ts -> LETTER_ENTERED
            else -> EMPTY
        }
    }
}*/

class GameRepository(private val gameDao: GameDao, private val currentGameDao: CurrentGameDao) {
    val allGames: Flow<List<Game>> = gameDao.getAllGames()

    suspend fun insert(game: Game) = gameDao.insert(game)

    suspend fun updateGameStatus(update: GameStatus) = gameDao.updateGameStatus(update)

    suspend fun getGameWithUid(key: Int) = gameDao.getGameWithUid(key)

    suspend fun delete(game: Game) = gameDao.delete(game)

    suspend fun clear() = gameDao.clear()

    suspend fun insertCurrent(game: Game) = currentGameDao.insertCurrent(game)

    suspend fun getCurrent() = currentGameDao.getCurrent()

    suspend fun updateCurrentGame(game: Game) = currentGameDao.updateCurrentGame(game)

    fun gameTagFromTiles(enteredTiles: Array<Tile>, shuffledTiles: Array<Tile>, gameTag: String): String{

        val sbEntered: StringBuilder = java.lang.StringBuilder(enteredTiles.count())

        for (tile in enteredTiles) {
            if (tile.letter != null) {
                sbEntered.append(tile.letter)
            }
        }

        val sbShuffled: StringBuilder = java.lang.StringBuilder(shuffledTiles.count())

        var count = 0

        for (tile in shuffledTiles) {
            if (tile.letter != null) {
                count++
                if (count > TAG_SORTED_COUNT) {
                    sbShuffled.append(tile.letter)
                }
            }
        }

        val sortedCount = if (sbEntered.count() <= TAG_SORTED_COUNT) sbEntered.count() else TAG_SORTED_COUNT
        val shuffledCount = if (sbShuffled.count() <= TAG_SHUFFLED_COUNT) sbShuffled.count() else TAG_SHUFFLED_COUNT

        return sbEntered.substring(0, sortedCount) + " " + sbShuffled.substring(0, shuffledCount) + " " + gameTag
    }
}
