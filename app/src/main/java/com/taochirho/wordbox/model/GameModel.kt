@file:Suppress("ArrayInDataClass")

package com.taochirho.wordbox.model

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.util.Log
import android.view.textservice.*
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.database.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashSet

class GameModel(application: Wordbox) : AndroidViewModel(application),
    SpellCheckerSession.SpellCheckerSessionListener,
    SharedPreferences.OnSharedPreferenceChangeListener {


    private val TAG = "GameModel"

    val trayTileID = 427  // if tile index 0 the swap tile does not appear

    private val tcrTwoLetterWords = setOf(
        "AH",
        "AM",
        "AN",
        "AS",
        "AT",
        "AY",
        "BE",
        "BI",
        "BY",
        "DO",
        "EH",
        "EM",
        "GO",
        "HA",
        "HE",
        "HI",
        "ID",
        "IF",
        "IN",
        "IS",
        "IT",
        "KO",
        "LA",
        "LO",
        "MA",
        "ME",
        "MU",
        "MY",
        "NO",
        "OF",
        "OH",
        "OI",
        "ON",
        "OR",
        "OX",
        "PA",
        "PI",
        "QI",
        "RE",
        "SO",
        "TA",
        "TO",
        "UH",
        "UP",
        "US",
        "WE",
        "YE",
        "YO"
    )
    private val tcrThreeLetterWords = setOf(
        "ABS",
        "ACE",
        "ACT",
        "ADD",
        "ADO",
        "ADS",
        "AFT",
        "AGE",
        "AGO",
        "AID",
        "AIM",
        "AIR",
        "ALE",
        "ALL",
        "AMP",
        "AND",
        "ANT",
        "ANY",
        "APE",
        "APP",
        "APT",
        "ARC",
        "ARE",
        "ARK",
        "ARM",
        "ART",
        "ASH",
        "ASK",
        "ASP",
        "ASS",
        "ATE",
        "AWE",
        "AWK",
        "AXE",
        "AYE",
        "AYU",
        "BAD",
        "BAG",
        "BAN",
        "BAR",
        "BAT",
        "BAY",
        "BED",
        "BEE",
        "BEG",
        "BET",
        "BIB",
        "BID",
        "BIG",
        "BIN",
        "BIT",
        "BOG",
        "BOW",
        "BOX",
        "BOY",
        "BUD",
        "BUG",
        "BUM",
        "BUN",
        "BUS",
        "BUT",
        "BUY",
        "BYE",
        "CAB",
        "CAD",
        "CAM",
        "CAN",
        "CAP",
        "CAR",
        "CAT",
        "CAW",
        "CEL",
        "CHI",
        "COB",
        "COD",
        "COG",
        "COL",
        "CON",
        "COO",
        "COP",
        "COT",
        "COW",
        "COX",
        "COY",
        "CRY",
        "CUB",
        "CUE",
        "CUE",
        "CUM",
        "CUP",
        "CUT",
        "DAB",
        "DAM",
        "DAN",
        "DAY",
        "DEN",
        "DEW",
        "DID",
        "DIE",
        "DIG",
        "DIM",
        "DIN",
        "DIP",
        "DOE",
        "DOG",
        "DON",
        "DOT",
        "DRY",
        "DUB",
        "DUE",
        "DUG",
        "DUN",
        "DUO",
        "DYE",
        "EAR",
        "EAT",
        "EBB",
        "EGG",
        "EGO",
        "ELF",
        "ELM",
        "EMU",
        "END",
        "ERA",
        "ERG",
        "EVE",
        "EYE",
        "EXE",
        "FAB",
        "FAD",
        "FAG",
        "FAN",
        "FAR",
        "FAT",
        "FAX",
        "FAY",
        "FED",
        "FEE",
        "FEN",
        "FEW",
        "FIG",
        "FIN",
        "FIR",
        "FIT",
        "FIX",
        "FLU",
        "FLY",
        "FOE",
        "FOG",
        "FOP",
        "FOR",
        "FOX",
        "FRY",
        "FUN",
        "FUR",
        "GAG",
        "GAL",
        "GAP",
        "GAS",
        "GAY",
        "GEL",
        "GEM",
        "GET",
        "GIG",
        "GIN",
        "GOT",
        "GOY",
        "GUM",
        "GUN",
        "GUT",
        "GUY",
        "GYM",
        "HAD",
        "HAG",
        "HAM",
        "HAS",
        "HAT",
        "HAY",
        "HEM",
        "HEN",
        "HER",
        "HID",
        "HIM",
        "HIP",
        "HIS",
        "HIT",
        "HOD",
        "HOP",
        "HOG",
        "HOT",
        "HOW",
        "HUB",
        "HUE",
        "HUG",
        "HUM",
        "HUT",
        "ICE",
        "ICY",
        "ILL",
        "IMP",
        "INK",
        "INN",
        "ION",
        "IRE",
        "ITS",
        "IVY",
        "JAM",
        "JAR",
        "JAW",
        "JAY",
        "JET",
        "JEW",
        "JOB",
        "JOE",
        "JOG",
        "JOY",
        "JUG",
        "KEN",
        "KEY",
        "KID",
        "KIN",
        "KIT",
        "LAD",
        "LAG",
        "LAP",
        "LAW",
        "LAX",
        "LAY",
        "LEA",
        "LED",
        "LEG",
        "LET",
        "LID",
        "LIE",
        "LIN",
        "LIP",
        "LIT",
        "LOG",
        "LOT",
        "LOW",
        "LOX",
        "MAD",
        "MAN",
        "MAP",
        "MAR",
        "MAS",
        "MAT",
        "MAX",
        "MAY",
        "MEN",
        "MET",
        "MID",
        "MIX",
        "MOM",
        "MOP",
        "MUD",
        "MUG",
        "MUM",
        "NAN",
        "NAG",
        "NAP",
        "NAY",
        "NET",
        "NEW",
        "NIL",
        "NIM",
        "NIP",
        "NIT",
        "NOD",
        "NOR",
        "NOT",
        "NOW",
        "NUN",
        "NUT",
        "OAK",
        "ODD",
        "OFF",
        "OFT",
        "OIL",
        "OIL",
        "OLD",
        "ONE",
        "OPT",
        "ORB",
        "ORE",
        "OUR",
        "OUT",
        "OWE",
        "OWL",
        "OWN",
        "PAD",
        "PAN",
        "PAP",
        "PAR",
        "PAW",
        "PAY",
        "PEA",
        "PEG",
        "PEL",
        "PEN",
        "PEP",
        "PET",
        "PEW",
        "PIE",
        "PIG",
        "PIN",
        "PIP",
        "PIT",
        "PIX",
        "POD",
        "POP",
        "POT",
        "POX",
        "PRO",
        "PUB",
        "PUG",
        "PUP",
        "PUT",
        "PYX",
        "QIN",
        "RAD",
        "RAG",
        "RAJ",
        "RAM",
        "RAN",
        "RAT",
        "RAW",
        "RAY",
        "RED",
        "RIB",
        "RID",
        "RIG",
        "RIM",
        "RIP",
        "ROB",
        "ROD",
        "ROE",
        "ROT",
        "ROW",
        "RUB",
        "RUE",
        "RUG",
        "RUM",
        "RUN",
        "RUT",
        "RYE",
        "SAC",
        "SAD",
        "SAG",
        "SAL",
        "SAP",
        "SAT",
        "SAW",
        "SAY",
        "SEA",
        "SEE",
        "SEN",
        "SET",
        "SEW",
        "SEX",
        "SHE",
        "SHY",
        "SIN",
        "SIP",
        "SIR",
        "SIT",
        "SIX",
        "SKI",
        "SKY",
        "SLY",
        "SOD",
        "SOL",
        "SON",
        "SOW",
        "SOY",
        "SPA",
        "SPY",
        "SUB",
        "SUE",
        "SUM",
        "SUN",
        "SUP",
        "TAB",
        "TAD",
        "TAG",
        "TAN",
        "TAN",
        "TAP",
        "TAR",
        "TAT",
        "TAX",
        "TEA",
        "TED",
        "TEE",
        "TEN",
        "THE",
        "THY",
        "TIC",
        "TIE",
        "TIL",
        "TIN",
        "TIP",
        "TOD",
        "TOE",
        "TOM",
        "TON",
        "TOR",
        "TOT",
        "TOW",
        "TOY",
        "TRY",
        "TUB",
        "TUG",
        "TWO",
        "USE",
        "VAN",
        "VAT",
        "VET",
        "VIA",
        "VIE",
        "VOW",
        "VUG",
        "WAN",
        "WAR",
        "WAS",
        "WAX",
        "WAY",
        "WEB",
        "WED",
        "WEE",
        "WET",
        "WHO",
        "WHY",
        "WIG",
        "WIN",
        "WIT",
        "WOK",
        "WON",
        "WOO",
        "WOW",
        "WRY",
        "YAK",
        "YEN",
        "YES",
        "YET",
        "YOU",
        "ZIP",
        "ZOO"

    )

    private val dataRepository = application.repository

    private val sb =
        java.lang.StringBuilder(84)// based on observation (log of length in checkSpelling())

    private var cdt: CountDownTimer

    data class Grid(var grid: Array<Array<Tile>>) {

        operator fun get(tilePos: TilePos): Tile {
            return grid[tilePos.row][tilePos.col]
        }

        operator fun set(tilePos: TilePos, tile: Tile) {
            grid[tilePos.row][tilePos.col] = tile
        }
    }

    data class Tray(var tray: Array<Tile>, var positionsSet: Boolean) {
        operator fun get(index: Int): Tile {
            return tray[index]
        }

        operator fun set(index: Int, tile: Tile) {
            tray[index] = tile
        }
    }

    /*@Parcelize
    data class TilePos(var row: Int, var col: Int, var trayIndex: Int = -1) :Parcelable

    internal class Line(val word: String, val tiles: Set<Tile>) {

        override fun toString(): String {
            return "Line{" +
                    word + '\'' +
                    ", from " + tiles.toString() +
                    '}'
        }
    }
*/
    private var grid: Grid
    private var tray: Tray
    private var prefDuration =
        PreferenceManager.getDefaultSharedPreferences(getApplication()).getInt(
            getApplication<Application>().resources.getString(
                R.string.keyTimer
            ), getApplication<Application>().resources.getInteger(R.integer.default_timer)
        )
    private var prefTiles = PreferenceManager.getDefaultSharedPreferences(getApplication()).getInt(
        getApplication<Application>().resources.getString(
            R.string.keyTiles
        ), getApplication<Application>().resources.getInteger(R.integer.default_tile_count)
    )

    lateinit var startDragPos: TilePos
    private var swapped = 0
    private var laid = 0
    private var filled = 0
    private val allLines = ArrayList<Line>()
    private val twoAndThree = ArrayList<Line>()
    private val errorTiles: MutableSet<Tile> = HashSet()

    private lateinit var session: SpellCheckerSession

    private val _gameTag: MutableLiveData<String> = MutableLiveData("Default")
    var gameTag: LiveData<String> = _gameTag

    private val _gameCreator: MutableLiveData<String> = MutableLiveData("Piltdown Man")
    var gameCreator: LiveData<String> = _gameCreator

    private val _theGrid: MutableLiveData<Grid> by lazy {
        MutableLiveData<Grid>()
    }
    var theGrid: LiveData<Grid> = _theGrid

    private val _theTray: MutableLiveData<Tray> by lazy {
        MutableLiveData<Tray>()
    }
    val theTray: LiveData<Tray> = _theTray

    private var _tileCount: MutableLiveData<Int> = MutableLiveData(30)
    var tileCount: LiveData<Int> = _tileCount

    private var _timerRunning: MutableLiveData<Boolean> = MutableLiveData(false)
    var timerRunning: LiveData<Boolean> = _timerRunning

//    private var _startTileState: MutableLiveData<TileStartState> = MutableLiveData(TileStartState.TIMER)
//    var startTileState: LiveData<TileStartState> = _startTileState

    private var _millisecondsLeft: MutableLiveData<Long> = MutableLiveData(0)
    var millisecondsLeft: LiveData<Long> = _millisecondsLeft

    private var _score = MutableLiveData(0)
    var score: LiveData<Int> = _score

    private var _tilesSwapped: MutableLiveData<Int> = MutableLiveData(0)
    var tilesSwapped: LiveData<Int> = _tilesSwapped

    private val letterSet = java.lang.StringBuilder(33)
    private var shuffleLetterSet = java.lang.StringBuilder(33)

    init {
        try {
            val tsm = application.getSystemService(TextServicesManager::class.java)
            session = tsm.newSpellCheckerSession(
                null,
                application.resources.configuration.locales[0],
                this,
                true
            )!!
        } catch (e: Exception) {
            Log.w(TAG, "Spell check error $e")
        }

        cdt = object : CountDownTimer(_millisecondsLeft.value!!, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _millisecondsLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _timerRunning.value = false
            }
        }

        laid = 0
        filled = 0

        prefTiles = PreferenceManager.getDefaultSharedPreferences(
            getApplication()
        ).getInt(
            getApplication<Application>().resources.getString(R.string.keyTiles),
            getApplication<Application>().resources.getInteger(R.integer.default_tile_count)
        )

        prefDuration = PreferenceManager.getDefaultSharedPreferences(
            getApplication()
        ).getInt(
            getApplication<Application>().resources.getString(R.string.keyTimer),
            getApplication<Application>().resources.getInteger(R.integer.default_timer)
        )

        PreferenceManager.getDefaultSharedPreferences(getApplication())
            .registerOnSharedPreferenceChangeListener(this)

        grid = Grid(Array(7) { Array(7) { Tile(state = TileState.EMPTY) } })
        tray = Tray(Array(getApplication<Application>().resources.getInteger(R.integer.max_tiles)) {
            Tile(state = TileState.IN_TRAY)
        }, false)
    }

    enum class TileStartState {
        TIMER, PAUSE, PLAY
    }

    data class TileInfo(
        // used in drag and drop
        var letter: String? = null,
        var row: Int = 0,
        var col: Int = 0,
        var trayIndex: Int = 0,
    )

    fun clipDataFromTileView(text: String, row: Int, col: Int, trayIndex: Int): ClipData {

        val dragData = ClipData(
            text, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
            ClipData.Item(text)
        )

        dragData.addItem(ClipData.Item(row.toString()))
        dragData.addItem(ClipData.Item(col.toString()))
        dragData.addItem(ClipData.Item((trayIndex).toString()))

        return dragData
    }

    fun tileInfoFromClipData(cd: ClipData): TileInfo {

        return TileInfo(
            cd.getItemAt(0).text.toString(),
            cd.getItemAt(1).text.toString().toInt(),
            cd.getItemAt(2).text.toString().toInt(),
            cd.getItemAt(3).text.toString().toInt(),
        )
    }

    fun updateTile(letter: String?, tilePos: TilePos) {
        if (letter != null) {
            grid[tilePos] = Tile(letter, TileState.WRONG, tilePos)
        } else {
            grid[tilePos] = Tile(null, TileState.EMPTY, tilePos)
        }
        _theGrid.value = grid
    }

    fun addTileToGrid(letter: String?, tilePos: TilePos) {

        if (letter != null) {
            grid[tilePos] = Tile(letter, TileState.WRONG, tilePos)
        } else {
            grid[tilePos] = Tile(null, TileState.EMPTY, tilePos)
        }
        scoreGrid()
        _theGrid.value = grid
    }

    fun removeTileFromGrid(tilePos: TilePos) {
        grid[tilePos] = Tile(null, TileState.EMPTY, tilePos)
        scoreGrid()
        _theGrid.value = grid
    }

    fun getLetterFromGrid(tilePos: TilePos): String? {
        return grid[tilePos].letter
    }

    private fun scoreGrid() {

        var tile: Tile
        var nextLetter: String?

        allLines.clear()
        errorTiles.clear()
        twoAndThree.clear()

        // words and sets across
        for (row in 0..6) {
            var word = StringBuilder()
            var setAcross: MutableSet<Tile> = HashSet()
            //  Set<Integer> setAll = new HashSet<>();
            for (col in 0..6) {
                tile = grid[TilePos(row, col)]
                nextLetter = if (col < 6) {
                    grid[TilePos(row, col + 1)].letter
                } else {
                    null
                }
                if (tile.letter != null) { // i.e. is a letter
                    grid[TilePos(row, col)].state = TileState.WRONG
                    word.append(tile.letter)
                    setAcross.add(tile)

                    if (nextLetter == null) { //i.e. followed by empty tile or end of line
                        when (word.length) {
                            1 -> {}
                            2 -> {
                                val intersection = mutableSetOf(word.toString())
                                intersection.retainAll(tcrTwoLetterWords)
                                if (intersection.isEmpty()) {
                                    errorTiles.addAll(setAcross)
                                } else {
                                    twoAndThree.add(
                                        Line(
                                            word.toString(),
                                            setAcross
                                        )
                                    ) // valid 2 & 3 letter words added to allLines  after the spellcheck
                                }
                            }
                            3 -> {
                                val intersection = mutableSetOf(word.toString())
                                intersection.retainAll(tcrThreeLetterWords)
                                if (intersection.isEmpty()) {
                                    errorTiles.addAll(setAcross)
                                } else {
                                    twoAndThree.add(Line(word.toString(), setAcross))
                                }
                            }
                            else -> {
                                allLines.add(Line(word.toString(), setAcross))
                            }
                        }
                        word = StringBuilder() // reset tile letter
                        setAcross = HashSet()
                    }
                }
            }
        }

        // words and sets down
        for (col in 0..6) {
            var word = StringBuilder()
            var setDown: MutableSet<Tile> = HashSet()
            // Set<Integer> setAll = new HashSet<>();
            for (row in 0..6) {
                tile = grid[TilePos(row, col)]
                nextLetter = if (row < 6) {
                    grid[TilePos(row + 1, col)].letter
                } else {
                    null // no letter past end of line!
                }
                if (tile.letter != null) { // i.e. is a letter
                    word.append(tile.letter)
                    setDown.add(tile)

                    if (nextLetter == null) { //i.e. followed by empty tile or end of line
                        when (word.length) {
                            1 -> {}
                            2 -> {//the android dictionary has many 2 & 3 letter "words" which aren't words
                                val intersection = mutableSetOf(word.toString())
                                intersection.retainAll(tcrTwoLetterWords)
                                if (intersection.isEmpty()) {
                                    errorTiles.addAll(setDown)
                                } else {
                                    twoAndThree.add(Line(word.toString(), setDown))
                                }
                            }
                            3 -> {
                                val intersection = mutableSetOf(word.toString())
                                intersection.retainAll(tcrThreeLetterWords)
                                if (intersection.isEmpty()) {
                                    errorTiles.addAll(setDown)
                                } else {
                                    twoAndThree.add(Line(word.toString(), setDown))
                                }
                            }
                            else -> {
                                allLines.add(Line(word.toString(), setDown))
                            }
                        }
                        word = StringBuilder()
                        setDown = HashSet()
                    }
                }
            }
        }
        checkSpelling()
    }

    private fun checkSpelling() {

        if (allLines.isEmpty()) {  // e.g if tiles have been loaded to scratchpad
            return
        }
        sb.clear()

        for (allLine in allLines) {
            sb.append(allLine.word)
            sb.append(" ")
        }

        session.getSentenceSuggestions(arrayOf(TextInfo(sb.toString())), 1)
    }

    override fun onGetSuggestions(p0: Array<out SuggestionsInfo>?) {
        TODO("Not yet implemented - but no need")
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) {

        for (ssInfo in results) {
            for (i in ssInfo.suggestionsCount - 1 downTo 0) {  // need to count down so that removeAt(i) removes at end otherwise index out of bounds error

                if (ssInfo.getSuggestionsInfoAt(i).suggestionsAttributes != SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) {
                    errorTiles.addAll(allLines[i].tiles)
                    allLines.removeAt(i)
                }
            }
            /*
            Iterate through allLine until it is empty.
            For each iteration the first item becomes the working set and is removed.
            If subsequent sets intersect with the working set they are combined with it and then removed.  When no more sets can be combined with the working set it is added to finalSets.
             */

        //    Log.w("onGetSentenceSuggestions", twoAndThree.toString())

            allLines.addAll(twoAndThree)

            val finalSets = HashSet<Set<Tile>>(allLines.size)

            while (allLines.isNotEmpty()) {
                var workingSet = allLines[allLines.size - 1].tiles
                allLines.removeAt(allLines.size - 1)
                var addingToSet = true

                while (addingToSet) {
                    addingToSet = false

                    if (allLines.isNotEmpty()) {

                        for (i in allLines.size - 1 downTo 0) {
                            if ((workingSet intersect allLines[i].tiles).isNotEmpty()) {
                                workingSet = workingSet union allLines[i].tiles
                                allLines.removeAt(i)
                                addingToSet = true
                                //                      Log.w("new working set", "index $i $workingSet")
                            }
                        }
                    }
                }
                finalSets.add(workingSet)
            }

            var score = 0

            for (nextSet in finalSets) {
                val intersection: MutableSet<Tile> = HashSet(errorTiles)
                intersection.retainAll(nextSet)
                if (intersection.isEmpty()) { // i.e. there are no tiles in error in the set
                    score = score.coerceAtLeast(nextSet.size)
                    for (tile in nextSet) {
                        grid[tile.tilePos].state = TileState.RIGHT
                    }
                } else {
                    for (tile in nextSet) {
                        grid[tile.tilePos].state = TileState.NEARLY_RIGHT
                    }
                }
            }

            for (tile in errorTiles) {
                grid[tile.tilePos].state = TileState.WRONG
            }

            _theGrid.value = grid
            _score.value = score
        }
    }

    fun loadTray(newGame: Boolean, withLetter: () -> String) {

        shuffleLetterSet.clear()
//        logLetterSet("loadTray start", letterSet)
        shuffleLetterSet.append(letterSet)
        letterSet.clear()

        // clear Grid

        for (row in 0..6) {
            for (col in 0..6) {
                grid[TilePos(row, col)] = Tile(null, TileState.EMPTY, TilePos(row, col))
            }
        }
        _theGrid.value = grid

        tray.positionsSet = false

        if (newGame) {
            _tileCount.value = prefTiles
         }

        for (i in 0 until _tileCount.value!!) {
            tray[i] = Tile(withLetter(), TileState.IN_TRAY, TilePos(-1, -1))
            letterSet.append(tray[i].letter)
        }

        _theTray.value = tray

        if (newGame) {
            startTimer()
            _tilesSwapped.value = 0
            _score.value = 0
        }
        _gameTag.value = getApplication<Application>().resources.getString(R.string.start)
        _gameCreator.value = ""
    }

    fun addTileToTray(letter: String, x: Int, y: Int): Int {
        var index = 0
        for (i in 0.._tileCount.value!!) {
            if (tray[i].letter == null) {
                index = i
                tray[i].letter = letter
                tray[i].state = TileState.IN_TRAY
                tray[i].tilePos = TilePos(x, y)
                break
            }
        }
        tray.positionsSet = true
        _theTray.value = tray
        return index
    }

    fun removeTileFromTray(index: Int) {
        tray[index] = Tile(null, TileState.IN_TRAY, TilePos(-1, -1))
        _theTray.value = tray
    }

    fun setTrayPosition(index: Int, x: Int, y: Int) {
        tray[index].tilePos = TilePos(x, y)
    }

    fun setTrayPositionOnMove(index: Int, x: Int, y: Int) {
        tray[index].tilePos = TilePos(x, y)
        _theTray.value = tray
    }

    fun swapLetter(index: Int) {
        swapped++
        _tilesSwapped.value = swapped

        var newLetter = randomLetter()

        while (newLetter == tray[index].letter) {
            newLetter = randomLetter()
        }

        tray[index].letter = newLetter

        _theTray.value = tray

    }

    fun positions(areSet: Boolean) {
        tray.positionsSet = areSet
    }

    fun loadGrid(newGame: Boolean, withLetter: () -> String) {

        shuffleLetterSet.clear()
        shuffleLetterSet.append(letterSet)
//        logLetterSet("loadGrid start", letterSet)

        letterSet.clear()

        // clear Tray
        for (i in tray.tray.indices) {
            tray[i] = Tile(null, TileState.IN_TRAY, TilePos(-1, -1))
        }
        tray.positionsSet = false
        _theTray.value = tray

        laid = 0
        filled = 0

        if (newGame) {
            _tileCount.value = prefTiles
        }

        for (row in 0..6) {
            for (col in 0..6) {
                grid[TilePos(row, col)] = getGridTile(TilePos(row, col), withLetter)
            }
        }

        _theGrid.value = grid

        if (newGame) {
            startTimer()
            _tilesSwapped.value = 0
            _score.value = 0
        }
        _gameTag.value = getApplication<Application>().resources.getString(R.string.start)
        _gameCreator.value = ""

    }

    private fun getGridTile(tilePos: TilePos, withLetter: () -> String): Tile {
        laid += 1
        return if ((filled > (_tileCount.value!! - 1)) || ((49 - laid > _tileCount.value!! - filled) && ((Math.random() * 10000) > 6122))) {
            Tile(null, TileState.EMPTY, tilePos)
        } else {
            filled += 1
            val tile = Tile(withLetter(), TileState.WRONG, tilePos)
            letterSet.append(tile.letter)
            tile
        }
    }

    fun randomLetter(): String {
        val r = Math.random() * 10000
        return when {
            r < 780 -> "A"
            r < 980 -> "B"
            r < 1380 -> "C"
            r < 1760 -> "D"
            r < 2860 -> "E"
            r < 3000 -> "F"
            r < 3300 -> "G"
            r < 3530 -> "H"
            r < 4390 -> "I"
            r < 4411 -> "J"
            r < 4516 -> "K"
            r < 5046 -> "L"
            r < 5316 -> "M"
            r < 6036 -> "N"
            r < 6646 -> "O"
            r < 6926 -> "P"
            r < 6952 -> "Q"
            r < 7682 -> "R"
            r < 8552 -> "S"
            r < 9222 -> "T"
            r < 9558 -> "U"
            r < 9658 -> "V"
            r < 9749 -> "W"
            r < 9789 -> "X"
            r < 9949 -> "Y"

            else -> "Z"
        }
    }

    fun shuffledLetter(): String {

        val i = (Math.random() * shuffleLetterSet.length).toInt()
        val s = shuffleLetterSet[i].toString()
        shuffleLetterSet.delete(i, i + 1)

        return s
    }

    fun pauseTimer() {
//        Log.w(TAG, "pauseTimer()")
        _timerRunning.value = false
        cdt.cancel()
    }

    fun restartTimer() {

        cdt = object : CountDownTimer(_millisecondsLeft.value!!, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _millisecondsLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _timerRunning.value = false
            }
        }.start()
        _timerRunning.value = true
    }

    fun startTimer() {
        cdt.cancel()
        val mills: Long = ((1000L * 60L * prefDuration) + 1000)
        cdt = object : CountDownTimer(mills, 1000) {

            override fun onTick(millisUntilFinished: Long) {
                _millisecondsLeft.value = millisUntilFinished
            }

            override fun onFinish() {

                _timerRunning.value = false
            }
        }.start()
        _timerRunning.value = true
    }

    private operator fun <T> MutableLiveData<T>.set(row: Int, col: Int, tile: Tile) {
        this[row, col] = tile
    }

    private operator fun <T> MutableLiveData<T>.get(row: Int, col: Int): Tile {
        return this[row, col]
    }

    operator fun <T> LiveData<T>.get(row: Int, col: Int): Tile {
        return this[row, col]
    }

    /*  fun clear() {
          viewModelScope.launch {
              dataRepository.clear()
          }
      }
  */
    fun saveGame() {

        val count = _tileCount.value ?: return
        viewModelScope.launch {
            val gameTiles = Array(count) { Tile(null, TileState.EMPTY, TilePos(0, 0)) }
            var i = 0

            for (row in 0..6) {
                for (col in 0..6) {

                    if (grid[TilePos(row, col)].letter != null) {
                        gameTiles[i] = grid[TilePos(row, col)]
                        i += 1
                    }
                }
            }

            for (index in 0 until count) {
                if (tray[index].letter != null) {
                    gameTiles[i] = tray[index]
                    i += 1
                }
            }

            dataRepository.insert(
                Game(
                    0,
                    _tileCount.value!!,
                    _millisecondsLeft.value!!,
                    Date(),
                    PreferenceManager.getDefaultSharedPreferences(getApplication()).getString(
                        getApplication<Application>().resources.getString(R.string.keyGameCreator),
                        getApplication<Application>().resources.getString(R.string.gameCreatorDefault)
                    )
                        ?: getApplication<Application>().resources.getString(R.string.gameCreatorDefault),
                    dataRepository.gameTagFromTiles(gameTiles, gameTiles, getApplication<Application>().resources.getString(R.string.saved)),
                    GAME_STATUS.C,
                    gameTiles
                )
            )
        }
    }

    fun updateCurrentGame() {

        val count = _tileCount.value ?: return

        val gameTiles = Array(count) { Tile(null, TileState.EMPTY, TilePos(0, 0)) }
        var i = 0

        for (row in 0..6) {
            for (col in 0..6) {

                if (_theGrid.value?.get(TilePos(row, col))?.letter != null) {
                    gameTiles[i] = _theGrid.value!![TilePos(row, col)]
                    i += 1
                }
            }
        }

        for (index in 0 until count) {
            if (_theTray.value?.get(index)?.letter != null) {
                gameTiles[i] = _theTray.value!![index]
                i += 1
            }
        }

        viewModelScope.launch {
            dataRepository.updateCurrentGame(
                Game(
                    0,
                    _tileCount.value!!,
                    _millisecondsLeft.value!!,
                    Date(),
                    PreferenceManager.getDefaultSharedPreferences(getApplication()).getString(
                        getApplication<Application>().resources.getString(R.string.keyGameCreator),
                        getApplication<Application>().resources.getString(R.string.gameCreatorDefault)
                    )
                        ?: getApplication<Application>().resources.getString(R.string.gameCreatorDefault),
                    _gameTag.value!!,
                    GAME_STATUS.C,
                    gameTiles
                )
            )
        }
    }

    override fun onCleared() {
        saveGame()
        super.onCleared()
    }

    fun storeGame(game: Game) {
        viewModelScope.launch {
            dataRepository.insert(game)
        }
    }

    fun retrieveGame(uid: Int) {
        Log.w("GameModel retrieveGame", "uid $uid")
        viewModelScope.launch {
            loadGame(dataRepository.getGameWithUid(uid))
        }
    }

    fun storeCurrentGame(game: Game) {
        viewModelScope.launch {
            dataRepository.insertCurrent(game)
        }
    }

    fun restoreCurrentGame() {
        viewModelScope.launch {
             loadGame(dataRepository.getCurrent())
        }
    }

    private fun loadGame(rg: Game) {

        _gameTag.value = rg.gameTag
        _gameCreator.value = rg.gameFrom
        _tileCount.value = rg.tileCount

        _score.value = 0
        _millisecondsLeft.value = rg.timeLeft

        // clear Grid
        for (row in 0..6) {
            for (col in 0..6) {
                grid[TilePos(row, col)] = Tile(null, TileState.EMPTY, TilePos(row, col))
            }
        }

        // clear Tray

        for (i in tray.tray.indices) {
            tray[i] = Tile(null, TileState.IN_TRAY, TilePos(-1, -1))
        }
        tray.positionsSet = false

        var trayIndex = 0

        letterSet.clear()

        for (i in 0 until rg.tileCount) {
            val tile = rg.gameTiles[i]

            letterSet.append(tile.letter)

            if (tile.state == TileState.IN_TRAY) {
                tray[trayIndex] = tile
                trayIndex++

            } else {
                grid[tile.tilePos] = tile
            }
        }

        scoreGrid()

        _theGrid.value = grid
        _theTray.value = tray


    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "kt" -> {
                if (sharedPreferences != null) {
                    prefTiles = sharedPreferences.getInt(
                        key,
                        getApplication<Application>().resources.getInteger(R.integer.default_tile_count)
                    )
                }
            }
            "kd" -> {
                if (sharedPreferences != null) {
                    prefDuration = sharedPreferences.getInt(
                        key,
                        getApplication<Application>().resources.getInteger(R.integer.default_timer)
                    )
                }
            }
        }
    }
}

class GameModelFactory(
    private val application: Wordbox
) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameModel::class.java)) {
//             Log.w("in GameModelFactory", "no args")
            return GameModel(application) as T
        }
        throw IllegalArgumentException("Unknown GameViewModel class")
    }
}




