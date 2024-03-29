package com.taochirho.wordbox.model

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.SharedPreferences
import android.os.CountDownTimer
import android.util.Log

import android.view.textservice.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.*
import com.taochirho.wordbox.database.*
import com.taochirho.wordbox.database.Line
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashSet

class WordBoxViewModel(application: Wordbox) : AndroidViewModel(application),
    SpellCheckerSession.SpellCheckerSessionListener,
    SharedPreferences.OnSharedPreferenceChangeListener  {

    private val tag = "WordBoxViewModel"
    val trayTileID = 427  // if tile index 0 the swap tile does not appear

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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Grid

            if (!grid.contentDeepEquals(other.grid)) return false

            return true
        }

        override fun hashCode(): Int {
            return grid.contentDeepHashCode()
        }
    }

    data class Tray(var tray: Array<Tile>,
                    var positionsSet: Boolean,
                  ) {
        operator fun get(index: Int): Tile {
            return tray[index]
        }

        operator fun set(index: Int, tile: Tile) {
            tray[index] = tile
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Tray

            if (!tray.contentEquals(other.tray)) return false
            if (positionsSet != other.positionsSet) return false
          /*  if (width != other.width) return false
            if (height != other.height) return false*/

            return true
        }

        override fun hashCode(): Int {
            var result = tray.contentHashCode()
            result = 31 * result + positionsSet.hashCode()
            /*result = 31 * result + width
            result = 31 * result + height*/
            return result
        }
    }

    private var grid: Grid
    private var tray: Tray
    private var prefDuration = 60000L *
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

    private val _currentGame: MutableLiveData<CurrentGame> = MutableLiveData(
        CurrentGame(
            0,
            15,
            0,
            900000L,
            900000L,// default 15 minutes (and 1 sec)
            Date(),
            "Word Box",
            "Welcome",
            GAME_STATUS.C,
            arrayOf(
                Tile("W", TileState.RIGHT, TilePos(0, 0)),
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
                Tile("A", TileState.WRONG, TilePos(4, 5)),
                Tile("X", TileState.WRONG, TilePos(5, 6)),
            )
        )
    )
    var currentGame: LiveData<CurrentGame> = _currentGame

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

    private var _starterAction: MutableLiveData<STARTER_ACTION> = MutableLiveData(STARTER_ACTION.S)
    var starterAction: LiveData<STARTER_ACTION> = _starterAction

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
            Log.w(tag, "Spell check error $e")
        }

        cdt = object : CountDownTimer(prefDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _millisecondsLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _timerRunning.value = false
            }
        }

        viewModelScope.launch {

            with (dataRepository.getCurrent()) {
                setCurrentGame(Game(
                    uid, tileCount, swappedTiles, timeSet, dateSaved, gameFrom, gameTag, status, gameTiles
                ))
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

        PreferenceManager.getDefaultSharedPreferences(getApplication())
            .registerOnSharedPreferenceChangeListener(this)

        grid = Grid(Array(7) { Array(7) { Tile(state = TileState.EMPTY) } })
        tray = Tray(Array(getApplication<Application>().resources.getInteger(R.integer.max_tiles)) {
            Tile(state = TileState.IN_TRAY)
        }, false)
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
        //("Not yet implemented - but no need")
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) {

        for (ssInfo in results) {
            for (i in ssInfo.suggestionsCount - 1 downTo 0) {
                if (ssInfo.getSuggestionsInfoAt(i).suggestionsAttributes != SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) {
               /*     Log.w("onGSS", "$i OK")
                    Log.w("onGSS", "allLines[i].tiles ${allLines[i].tiles.toString()}")
                } else {
                    Log.w("onGSS", "$i Not in dic")
                    Log.w("onGSS", "allLines[i].tiles ${allLines[i].tiles.toString()}")*/
                    errorTiles.addAll(allLines[i].tiles)
                    allLines.removeAt(i)
                }
            }
            /*
            Iterate through allLine until it is empty.
            For each iteration the first item becomes the working set and is removed.
            If subsequent sets intersect with the working set they are combined with it and then removed.  When no more sets can be combined with the working set it is added to finalSets.
             */

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
        _score.value = 0

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
            setTimer(prefDuration)
            _tilesSwapped.value = 0
            _score.value = 0
        }
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
/*

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
*/

    fun positions(areSet: Boolean) {
        tray.positionsSet = areSet
    }

    fun loadGrid(newGame: Boolean, withLetter: () -> String) {

        shuffleLetterSet.clear()
        shuffleLetterSet.append(letterSet)
        letterSet.clear()

        // clear Tray
        for (i in tray.tray.indices) {
            tray[i] = Tile(null, TileState.IN_TRAY, TilePos(-1, -1))
        }
        tray.positionsSet = false
        _theTray.value = tray

        laid = 0
        filled = 0
        _score.value = 0

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
            setTimer(prefDuration)
            _tilesSwapped.value = 0
            _score.value = 0
        }
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

    fun gameStart() {

        when (_starterAction.value){
            STARTER_ACTION.S -> startTimer()
            STARTER_ACTION.P -> pauseTimer()
            STARTER_ACTION.F -> {
                _currentGame.value?.let { setTimer(it.timeSet) }
            }
            else -> {}
        }
    }

    private fun pauseTimer() {
        _timerRunning.value = false
        _starterAction.value = STARTER_ACTION.S
        cdt.cancel()
    }

    fun setTimer(time: Long) {
        cdt.cancel()
        _millisecondsLeft.value = time
        _timerRunning.value = false
        _starterAction.value = STARTER_ACTION.S
    }

    private fun startTimer() {
        cdt.cancel()
        cdt = object : CountDownTimer(_millisecondsLeft.value!!, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _millisecondsLeft.value = millisUntilFinished
            }

            override fun onFinish() {
                _timerRunning.value = false
                _starterAction.value = STARTER_ACTION.F
            }
        }
        _timerRunning.value = true
        _starterAction.value = STARTER_ACTION.P
        cdt.start()
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

    private fun getTilesFromGridAndTray() : Array<Tile> {
        val count = _tileCount.value ?: 0

        val gameTiles = Array(count){ Tile(null, TileState.EMPTY, TilePos(0, 0)) }
        var i = 0

        for (row in 0..6) {
            for (col in 0..6) {

                if (grid[TilePos(row, col)].letter != null) {
                    gameTiles[i] = grid[TilePos(row, col)]
                    i += 1
                }
            }
        }
        for (index in 0 until tray.tray.size) {
            if (tray[index].letter != null) {
                gameTiles[i] = tray[index]
                i += 1
            }
        }
        return gameTiles
    }

    fun saveGame(status: GAME_STATUS) {

        viewModelScope.launch {
            val gameTiles = getTilesFromGridAndTray()
            val desc = if (status == GAME_STATUS.RA) {
                _tilesSwapped.value = 0
                getApplication<Application>().resources.getString(R.string.answer)
            } else {
                if (_tilesSwapped.value == 0) {
                    getApplication<Application>().resources.getString(R.string.saved)
                } else {
                    getApplication<Application>().resources.getString(
                        R.string.saved_with_swaps,
                        _tilesSwapped.value
                    )
                }
            }

            dataRepository.insert(
                Game(
                    0,
                    _tileCount.value!!,
                    _tilesSwapped.value!!,
                    _millisecondsLeft.value!!,
                    Date(),
                    PreferenceManager.getDefaultSharedPreferences(getApplication()).getString(
                        getApplication<Application>().resources.getString(R.string.keyGameCreator),
                        getApplication<Application>().resources.getString(R.string.gameCreatorDefault)
                    )
                        ?: getApplication<Application>().resources.getString(R.string.gameCreatorDefault),
                    dataRepository.gameTagFromTiles(gameTiles, gameTiles, desc),
                    status,
                    gameTiles
                )
            )

            val firestoreDB = Firebase.firestore

            val user = hashMapOf(
                "first" to dataRepository.gameTagFromTiles(gameTiles, gameTiles, desc),
                "last" to  "nallhblah",
                "born" to 1815

            )


            firestoreDB.collection("Games")
                .add(user)
                .addOnSuccessListener { documentReference ->
                    Log.d(tag, "DocumentSnapshot added with ID: ${documentReference.id}")
                }
                .addOnFailureListener { e ->
                    Log.w(tag, "Error adding document", e)
                }

        }
    }

    fun setCurrentGame(game: Game) {

        _currentGame.value = CurrentGame(
                0,
                game.tileCount,
                game.swappedTiles,
                game.timeSet,
                game.timeSet,
                game.dateSaved,
                game.gameFrom,
                game.gameTag,
                game.status,
                game.gameTiles
            )
     }

    fun updateCurrentGame() {
        _currentGame.value = CurrentGame(
            0,
            _tileCount.value!!,
            _tilesSwapped.value!!,
            _currentGame.value?.timeSet!!,
            _millisecondsLeft.value!!,
            _currentGame.value?.dateSaved!!,
            _currentGame.value?.gameFrom!!,
            _currentGame.value?.gameTag!!,
            _currentGame.value?.status!!,
            getTilesFromGridAndTray()
        )
    }

    override fun onCleared() {
        viewModelScope.launch {
            _currentGame.value?.let { dataRepository.updateCurrentGame(it) }
        }
        super.onCleared()
    }

    fun storeGame(game: Game) {
        viewModelScope.launch {
            dataRepository.insert(game)
        }
    }

    fun loadGame(currentGame: CurrentGame) {
        with (currentGame) {
            _tileCount.value = tileCount
            _tilesSwapped.value = swappedTiles
            _score.value = 0
            _millisecondsLeft.value = timeLeft
        }
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

        for (i in 0 until currentGame.tileCount) {
            val tile = currentGame.gameTiles[i]

            letterSet.append(tile.letter)

            if (tile.state == TileState.IN_TRAY) {
                tray[trayIndex] = tile
                trayIndex++

            } else {
                grid[tile.tilePos] = tile
            }
        }

        //scoreGrid()

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
                    prefDuration = 60000L * sharedPreferences.getInt(
                        key,
                        getApplication<Application>().resources.getInteger(R.integer.default_timer)
                    )
                }
            }
        }
    }

    // from GamesListModel

    val allGames: LiveData<List<Game>> = dataRepository.allGames.asLiveData()

    fun clearGames() {
        viewModelScope.launch {
            dataRepository.clear()
        }
    }

    fun gameToggleLetter(game: Game) {

        if (game.status == GAME_STATUS.C) {
            return
        }

        val gs = if (game.status == GAME_STATUS.SA) {
            GAME_STATUS.RA
        } else {
            GAME_STATUS.SA
        }
        viewModelScope.launch {
            dataRepository.updateGameStatus(GameStatus(game.uid, gs))
        }
    }

    fun gameDelete(game: Game) {
        viewModelScope.launch {
            dataRepository.delete(game)
        }
    }

    suspend fun getGame(uid: Int): Game {
        return dataRepository.getGameWithUid(uid)
    }

    // from GameCreateModel

    private val gameStr = application.applicationContext.getString(R.string.game)
    private val answerStr = application.applicationContext.getString(R.string.answer)

    val uiState = UIState(7, 7)
    private val pattern = Regex("[a-zA-Z]")
    private var resetTray = false

    private val shuffledLetters = CharArray(49) { '\u0020' } // u0020 is unicode space

    private val _gameDuration: MutableLiveData<Int> = MutableLiveData(0)
    val gameDuration: LiveData<Int> = _gameDuration

    private val _toastMsg = MutableLiveData<Event<TOAST_MSGS>>()
    val toastMsg: LiveData<Event<TOAST_MSGS>> = _toastMsg

    fun setToastMsg(msg: TOAST_MSGS) {
        _toastMsg.value = Event(msg)
    }

    private val _letter0: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter0: LiveData<Char> = _letter0

    private val _letter1: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter1: LiveData<Char> = _letter1

    private val _letter2: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter2: LiveData<Char> = _letter2

    private val _letter3: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter3: LiveData<Char> = _letter3

    private val _letter4: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter4: LiveData<Char> = _letter4

    private val _letter5: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter5: LiveData<Char> = _letter5

    private val _letter6: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter6: LiveData<Char> = _letter6

    private val _letter7: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter7: LiveData<Char> = _letter7

    private val _letter8: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter8: LiveData<Char> = _letter8

    private val _letter9: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter9: LiveData<Char> = _letter9

    private val _letter10: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter10: LiveData<Char> = _letter10

    private val _letter11: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter11: LiveData<Char> = _letter11

    private val _letter12: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter12: LiveData<Char> = _letter12

    private val _letter13: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter13: LiveData<Char> = _letter13

    private val _letter14: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter14: LiveData<Char> = _letter14

    private val _letter15: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter15: LiveData<Char> = _letter15

    private val _letter16: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter16: LiveData<Char> = _letter16

    private val _letter17: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter17: LiveData<Char> = _letter17

    private val _letter18: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter18: LiveData<Char> = _letter18

    private val _letter19: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter19: LiveData<Char> = _letter19

    private val _letter20: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter20: LiveData<Char> = _letter20

    private val _letter21: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter21: LiveData<Char> = _letter21

    private val _letter22: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter22: LiveData<Char> = _letter22

    private val _letter23: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter23: LiveData<Char> = _letter23

    private val _letter24: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter24: LiveData<Char> = _letter24

    private val _letter25: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter25: LiveData<Char> = _letter25

    private val _letter26: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter26: LiveData<Char> = _letter26

    private val _letter27: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter27: LiveData<Char> = _letter27

    private val _letter28: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter28: LiveData<Char> = _letter28

    private val _letter29: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter29: LiveData<Char> = _letter29

    private val _letter30: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter30: LiveData<Char> = _letter30

    private val _letter31: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter31: LiveData<Char> = _letter31

    private val _letter32: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter32: LiveData<Char> = _letter32

    private val _letter33: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter33: LiveData<Char> = _letter33

    private val _letter34: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter34: LiveData<Char> = _letter34

    private val _letter35: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter35: LiveData<Char> = _letter35

    private val _letter36: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter36: LiveData<Char> = _letter36

    private val _letter37: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter37: LiveData<Char> = _letter37

    private val _letter38: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter38: LiveData<Char> = _letter38

    private val _letter39: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter39: LiveData<Char> = _letter39

    private val _letter40: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter40: LiveData<Char> = _letter40

    private val _letter41: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter41: LiveData<Char> = _letter41

    private val _letter42: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter42: LiveData<Char> = _letter42

    private val _letter43: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter43: LiveData<Char> = _letter43

    private val _letter44: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter44: LiveData<Char> = _letter44

    private val _letter45: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter45: LiveData<Char> = _letter45

    private val _letter46: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter46: LiveData<Char> = _letter46

    private val _letter47: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter47: LiveData<Char> = _letter47

    private val _letter48: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val letter48: LiveData<Char> = _letter48

    private val _letterElse: MutableLiveData<Char> = MutableLiveData('\u0020') // Z
    private val letterElse: LiveData<Char> = _letterElse

    private val _enteredLetter0: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter0: LiveData<Char> = _enteredLetter0

    private val _enteredLetter1: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter1: LiveData<Char> = _enteredLetter1

    private val _enteredLetter2: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter2: LiveData<Char> = _enteredLetter2

    private val _enteredLetter3: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter3: LiveData<Char> = _enteredLetter3

    private val _enteredLetter4: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter4: LiveData<Char> = _enteredLetter4

    private val _enteredLetter5: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter5: LiveData<Char> = _enteredLetter5

    private val _enteredLetter6: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter6: LiveData<Char> = _enteredLetter6

    private val _enteredLetter7: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter7: LiveData<Char> = _enteredLetter7

    private val _enteredLetter8: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter8: LiveData<Char> = _enteredLetter8

    private val _enteredLetter9: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter9: LiveData<Char> = _enteredLetter9

    private val _enteredLetter10: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter10: LiveData<Char> = _enteredLetter10

    private val _enteredLetter11: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter11: LiveData<Char> = _enteredLetter11

    private val _enteredLetter12: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter12: LiveData<Char> = _enteredLetter12

    private val _enteredLetter13: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter13: LiveData<Char> = _enteredLetter13

    private val _enteredLetter14: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter14: LiveData<Char> = _enteredLetter14

    private val _enteredLetter15: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter15: LiveData<Char> = _enteredLetter15

    private val _enteredLetter16: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter16: LiveData<Char> = _enteredLetter16

    private val _enteredLetter17: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter17: LiveData<Char> = _enteredLetter17

    private val _enteredLetter18: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter18: LiveData<Char> = _enteredLetter18

    private val _enteredLetter19: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter19: LiveData<Char> = _enteredLetter19

    private val _enteredLetter20: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter20: LiveData<Char> = _enteredLetter20

    private val _enteredLetter21: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter21: LiveData<Char> = _enteredLetter21

    private val _enteredLetter22: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter22: LiveData<Char> = _enteredLetter22

    private val _enteredLetter23: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter23: LiveData<Char> = _enteredLetter23

    private val _enteredLetter24: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter24: LiveData<Char> = _enteredLetter24

    private val _enteredLetter25: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter25: LiveData<Char> = _enteredLetter25

    private val _enteredLetter26: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter26: LiveData<Char> = _enteredLetter26

    private val _enteredLetter27: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter27: LiveData<Char> = _enteredLetter27

    private val _enteredLetter28: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter28: LiveData<Char> = _enteredLetter28

    private val _enteredLetter29: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter29: LiveData<Char> = _enteredLetter29

    private val _enteredLetter30: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter30: LiveData<Char> = _enteredLetter30

    private val _enteredLetter31: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter31: LiveData<Char> = _enteredLetter31

    private val _enteredLetter32: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter32: LiveData<Char> = _enteredLetter32

    private val _enteredLetter33: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter33: LiveData<Char> = _enteredLetter33

    private val _enteredLetter34: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter34: LiveData<Char> = _enteredLetter34

    private val _enteredLetter35: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter35: LiveData<Char> = _enteredLetter35

    private val _enteredLetter36: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter36: LiveData<Char> = _enteredLetter36

    private val _enteredLetter37: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter37: LiveData<Char> = _enteredLetter37

    private val _enteredLetter38: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter38: LiveData<Char> = _enteredLetter38

    private val _enteredLetter39: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter39: LiveData<Char> = _enteredLetter39

    private val _enteredLetter40: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter40: LiveData<Char> = _enteredLetter40

    private val _enteredLetter41: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter41: LiveData<Char> = _enteredLetter41

    private val _enteredLetter42: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter42: LiveData<Char> = _enteredLetter42

    private val _enteredLetter43: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter43: LiveData<Char> = _enteredLetter43

    private val _enteredLetter44: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter44: LiveData<Char> = _enteredLetter44

    private val _enteredLetter45: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter45: LiveData<Char> = _enteredLetter45

    private val _enteredLetter46: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter46: LiveData<Char> = _enteredLetter46

    private val _enteredLetter47: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter47: LiveData<Char> = _enteredLetter47

    private val _enteredLetter48: MutableLiveData<Char> = MutableLiveData('\u0020')
    private val enteredLetter48: LiveData<Char> = _enteredLetter48

    private val _letterCount: MutableLiveData<Int> = MutableLiveData(0)
    val letterCount: LiveData<Int> = _letterCount

    fun getLetter(index: Int): LiveData<Char> {
        return when (index) {
            0 -> letter0
            1 -> letter1
            2 -> letter2
            3 -> letter3
            4 -> letter4
            5 -> letter5
            6 -> letter6
            7 -> letter7
            8 -> letter8
            9 -> letter9
            10 -> letter10
            11 -> letter11
            12 -> letter12
            13 -> letter13
            14 -> letter14
            15 -> letter15
            16 -> letter16
            17 -> letter17
            18 -> letter18
            19 -> letter19
            20 -> letter20
            21 -> letter21
            22 -> letter22
            23 -> letter23
            24 -> letter24
            25 -> letter25
            26 -> letter26
            27 -> letter27
            28 -> letter28
            29 -> letter29
            30 -> letter30
            31 -> letter31
            32 -> letter32
            33 -> letter33
            34 -> letter34
            35 -> letter35
            36 -> letter36
            37 -> letter37
            38 -> letter38
            39 -> letter39
            40 -> letter40
            41 -> letter41
            42 -> letter42
            43 -> letter43
            44 -> letter44
            45 -> letter45
            46 -> letter46
            47 -> letter47
            48 -> letter48
            else -> {
                letterElse
            }
        }
    }

    fun getEnteredLetter(index: Int): LiveData<Char> {
        return when (index) {
            0 -> enteredLetter0
            1 -> enteredLetter1
            2 -> enteredLetter2
            3 -> enteredLetter3
            4 -> enteredLetter4
            5 -> enteredLetter5
            6 -> enteredLetter6
            7 -> enteredLetter7
            8 -> enteredLetter8
            9 -> enteredLetter9
            10 -> enteredLetter10
            11 -> enteredLetter11
            12 -> enteredLetter12
            13 -> enteredLetter13
            14 -> enteredLetter14
            15 -> enteredLetter15
            16 -> enteredLetter16
            17 -> enteredLetter17
            18 -> enteredLetter18
            19 -> enteredLetter19
            20 -> enteredLetter20
            21 -> enteredLetter21
            22 -> enteredLetter22
            23 -> enteredLetter23
            24 -> enteredLetter24
            25 -> enteredLetter25
            26 -> enteredLetter26
            27 -> enteredLetter27
            28 -> enteredLetter28
            29 -> enteredLetter29
            30 -> enteredLetter30
            31 -> enteredLetter31
            32 -> enteredLetter32
            33 -> enteredLetter33
            34 -> enteredLetter34
            35 -> enteredLetter35
            36 -> enteredLetter36
            37 -> enteredLetter37
            38 -> enteredLetter38
            39 -> enteredLetter39
            40 -> enteredLetter40
            41 -> enteredLetter41
            42 -> enteredLetter42
            43 -> enteredLetter43
            44 -> enteredLetter44
            45 -> enteredLetter45
            46 -> enteredLetter46
            47 -> enteredLetter47
            48 -> enteredLetter48

            else -> {
                letterElse
            }
        }
    }

    fun processInput(newTextValue: TextFieldValue, index: Int) {

        with(newTextValue) {
            if (text.isEmpty()) {
                updateLetters(index, '\u0020')
                return
            }

            if (text[selection.start - 1].toString().matches(pattern)) {
                updateLetters(index, text[selection.start - 1].uppercaseChar())
            }
        }
    }

    fun onDurationChange(newDuration: String) {
        try {
            val x = newDuration.toInt()
            _gameDuration.value = x
        } catch (e: NumberFormatException) {
            _toastMsg.value = Event(TOAST_MSGS.NOT_A_NUMBER)
        }
    }

    private fun updateLetters(index: Int, letter: Char) {
        when (index) {
            0 -> {
                _letter0.value = letter
                _enteredLetter0.value = letter
            }
            1 -> {
                _letter1.value = letter
                _enteredLetter1.value = letter
            }
            2 -> {
                _letter2.value = letter
                _enteredLetter2.value = letter
            }
            3 -> {
                _letter3.value = letter
                _enteredLetter3.value = letter
            }
            4 -> {
                _letter4.value = letter
                _enteredLetter4.value = letter
            }
            5 -> {
                _letter5.value = letter
                _enteredLetter5.value = letter
            }
            6 -> {
                _letter6.value = letter
                _enteredLetter6.value = letter
            }
            7 -> {
                _letter7.value = letter
                _enteredLetter7.value = letter
            }
            8 -> {
                _letter8.value = letter
                _enteredLetter8.value = letter
            }
            9 -> {
                _letter9.value = letter
                _enteredLetter9.value = letter
            }
            10 -> {
                _letter10.value = letter
                _enteredLetter10.value = letter
            }
            11 -> {
                _letter11.value = letter
                _enteredLetter11.value = letter
            }
            12 -> {
                _letter12.value = letter
                _enteredLetter12.value = letter
            }
            13 -> {
                _letter13.value = letter
                _enteredLetter13.value = letter
            }
            14 -> {
                _letter14.value = letter
                _enteredLetter14.value = letter
            }
            15 -> {
                _letter15.value = letter
                _enteredLetter15.value = letter
            }
            16 -> {
                _letter16.value = letter
                _enteredLetter16.value = letter
            }
            17 -> {
                _letter17.value = letter
                _enteredLetter17.value = letter
            }
            18 -> {
                _letter18.value = letter
                _enteredLetter18.value = letter
            }
            19 -> {
                _letter19.value = letter
                _enteredLetter19.value = letter
            }
            20 -> {
                _letter20.value = letter
                _enteredLetter20.value = letter
            }
            21 -> {
                _letter21.value = letter
                _enteredLetter21.value = letter
            }
            22 -> {
                _letter22.value = letter
                _enteredLetter22.value = letter
            }
            23 -> {
                _letter23.value = letter
                _enteredLetter23.value = letter
            }
            24 -> {
                _letter24.value = letter
                _enteredLetter24.value = letter
            }
            25 -> {
                _letter25.value = letter
                _enteredLetter25.value = letter
            }
            26 -> {
                _letter26.value = letter
                _enteredLetter26.value = letter
            }
            27 -> {
                _letter27.value = letter
                _enteredLetter27.value = letter
            }
            28 -> {
                _letter28.value = letter
                _enteredLetter28.value = letter
            }
            29 -> {
                _letter29.value = letter
                _enteredLetter29.value = letter
            }
            30 -> {
                _letter30.value = letter
                _enteredLetter30.value = letter
            }
            31 -> {
                _letter31.value = letter
                _enteredLetter31.value = letter
            }
            32 -> {
                _letter32.value = letter
                _enteredLetter32.value = letter
            }
            33 -> {
                _letter33.value = letter
                _enteredLetter33.value = letter
            }
            34 -> {
                _letter34.value = letter
                _enteredLetter34.value = letter
            }
            35 -> {
                _letter35.value = letter
                _enteredLetter35.value = letter
            }
            36 -> {
                _letter36.value = letter
                _enteredLetter36.value = letter
            }
            37 -> {
                _letter37.value = letter
                _enteredLetter37.value = letter
            }
            38 -> {
                _letter38.value = letter
                _enteredLetter38.value = letter
            }
            39 -> {
                _letter39.value = letter
                _enteredLetter39.value = letter
            }
            40 -> {
                _letter40.value = letter
                _enteredLetter40.value = letter
            }
            41 -> {
                _letter41.value = letter
                _enteredLetter41.value = letter
            }
            42 -> {
                _letter42.value = letter
                _enteredLetter42.value = letter
            }
            43 -> {
                _letter43.value = letter
                _enteredLetter43.value = letter
            }
            44 -> {
                _letter44.value = letter
                _enteredLetter44.value = letter
            }
            45 -> {
                _letter45.value = letter
                _enteredLetter45.value = letter
            }
            46 -> {
                _letter46.value = letter
                _enteredLetter46.value = letter
            }
            47 -> {
                _letter47.value = letter
                _enteredLetter47.value = letter
            }
            48 -> {
                _letter48.value = letter
                _enteredLetter48.value = letter
            }
        }

        if (resetTray) {
            resetTray()
            resetTray = false
        }
        _letterCount.value = countLetters()
    }

    fun clearLetters() {

        val chSpace = '\u0020'

        for (i in 0..48)
            when (i) {
                0 -> {
                    _letter0.value = chSpace
                    _enteredLetter0.value = chSpace
                }
                1 -> {
                    _letter1.value = chSpace
                    _enteredLetter1.value = chSpace
                }
                2 -> {
                    _letter2.value = chSpace
                    _enteredLetter2.value = chSpace
                }
                3 -> {
                    _letter3.value = chSpace
                    _enteredLetter3.value = chSpace
                }
                4 -> {
                    _letter4.value = chSpace
                    _enteredLetter4.value = chSpace
                }
                5 -> {
                    _letter5.value = chSpace
                    _enteredLetter5.value = chSpace
                }
                6 -> {
                    _letter6.value = chSpace
                    _enteredLetter6.value = chSpace
                }
                7 -> {
                    _letter7.value = chSpace
                    _enteredLetter7.value = chSpace
                }
                8 -> {
                    _letter8.value = chSpace
                    _enteredLetter8.value = chSpace
                }
                9 -> {
                    _letter9.value = chSpace
                    _enteredLetter9.value = chSpace
                }
                10 -> {
                    _letter10.value = chSpace
                    _enteredLetter10.value = chSpace
                }
                11 -> {
                    _letter11.value = chSpace
                    _enteredLetter11.value = chSpace
                }
                12 -> {
                    _letter12.value = chSpace
                    _enteredLetter12.value = chSpace
                }
                13 -> {
                    _letter13.value = chSpace
                    _enteredLetter13.value = chSpace
                }
                14 -> {
                    _letter14.value = chSpace
                    _enteredLetter14.value = chSpace
                }
                15 -> {
                    _letter15.value = chSpace
                    _enteredLetter15.value = chSpace
                }
                16 -> {
                    _letter16.value = chSpace
                    _enteredLetter16.value = chSpace
                }
                17 -> {
                    _letter17.value = chSpace
                    _enteredLetter17.value = chSpace
                }
                18 -> {
                    _letter18.value = chSpace
                    _enteredLetter18.value = chSpace
                }
                19 -> {
                    _letter19.value = chSpace
                    _enteredLetter19.value = chSpace
                }
                20 -> {
                    _letter20.value = chSpace
                    _enteredLetter20.value = chSpace
                }
                21 -> {
                    _letter21.value = chSpace
                    _enteredLetter21.value = chSpace
                }
                22 -> {
                    _letter22.value = chSpace
                    _enteredLetter22.value = chSpace
                }
                23 -> {
                    _letter23.value = chSpace
                    _enteredLetter23.value = chSpace
                }
                24 -> {
                    _letter24.value = chSpace
                    _enteredLetter24.value = chSpace
                }
                25 -> {
                    _letter25.value = chSpace
                    _enteredLetter25.value = chSpace
                }
                26 -> {
                    _letter26.value = chSpace
                    _enteredLetter26.value = chSpace
                }
                27 -> {
                    _letter27.value = chSpace
                    _enteredLetter27.value = chSpace
                }
                28 -> {
                    _letter28.value = chSpace
                    _enteredLetter28.value = chSpace
                }
                29 -> {
                    _letter29.value = chSpace
                    _enteredLetter29.value = chSpace
                }
                30 -> {
                    _letter30.value = chSpace
                    _enteredLetter30.value = chSpace
                }
                31 -> {
                    _letter31.value = chSpace
                    _enteredLetter31.value = chSpace
                }
                32 -> {
                    _letter32.value = chSpace
                    _enteredLetter32.value = chSpace
                }
                33 -> {
                    _letter33.value = chSpace
                    _enteredLetter33.value = chSpace
                }
                34 -> {
                    _letter34.value = chSpace
                    _enteredLetter34.value = chSpace
                }
                35 -> {
                    _letter35.value = chSpace
                    _enteredLetter35.value = chSpace
                }
                36 -> {
                    _letter36.value = chSpace
                    _enteredLetter36.value = chSpace
                }
                37 -> {
                    _letter37.value = chSpace
                    _enteredLetter37.value = chSpace
                }
                38 -> {
                    _letter38.value = chSpace
                    _enteredLetter38.value = chSpace
                }
                39 -> {
                    _letter39.value = chSpace
                    _enteredLetter39.value = chSpace
                }
                40 -> {
                    _letter40.value = chSpace
                    _enteredLetter40.value = chSpace
                }
                41 -> {
                    _letter41.value = chSpace
                    _enteredLetter41.value = chSpace
                }
                42 -> {
                    _letter42.value = chSpace
                    _enteredLetter42.value = chSpace
                }
                43 -> {
                    _letter43.value = chSpace
                    _enteredLetter43.value = chSpace
                }
                44 -> {
                    _letter44.value = chSpace
                    _enteredLetter44.value = chSpace
                }
                45 -> {
                    _letter45.value = chSpace
                    _enteredLetter45.value = chSpace
                }
                46 -> {
                    _letter46.value = chSpace
                    _enteredLetter46.value = chSpace
                }
                47 -> {
                    _letter47.value = chSpace
                    _enteredLetter47.value = chSpace
                }
                48 -> {
                    _letter48.value = chSpace
                    _enteredLetter48.value = chSpace
                }
            }
        _letterCount.value = 0
    }

    fun shuffleLetters() {

        var enteredLetterSB = StringBuilder(49)

        for (i in 0..48)
            when (i) {
                0 -> enteredLetterSB = enteredLetterSB.append(_letter0.value!!)
                1 -> enteredLetterSB = enteredLetterSB.append(_letter1.value!!)
                2 -> enteredLetterSB = enteredLetterSB.append(_letter2.value!!)
                3 -> enteredLetterSB = enteredLetterSB.append(_letter3.value!!)
                4 -> enteredLetterSB = enteredLetterSB.append(_letter4.value!!)
                5 -> enteredLetterSB = enteredLetterSB.append(_letter5.value!!)
                6 -> enteredLetterSB = enteredLetterSB.append(_letter6.value!!)
                7 -> enteredLetterSB = enteredLetterSB.append(_letter7.value!!)
                8 -> enteredLetterSB = enteredLetterSB.append(_letter8.value!!)
                9 -> enteredLetterSB = enteredLetterSB.append(_letter9.value!!)
                10 -> enteredLetterSB = enteredLetterSB.append(_letter10.value!!)
                11 -> enteredLetterSB = enteredLetterSB.append(_letter11.value!!)
                12 -> enteredLetterSB = enteredLetterSB.append(_letter12.value!!)
                13 -> enteredLetterSB = enteredLetterSB.append(_letter13.value!!)
                14 -> enteredLetterSB = enteredLetterSB.append(_letter14.value!!)
                15 -> enteredLetterSB = enteredLetterSB.append(_letter15.value!!)
                16 -> enteredLetterSB = enteredLetterSB.append(_letter16.value!!)
                17 -> enteredLetterSB = enteredLetterSB.append(_letter17.value!!)
                18 -> enteredLetterSB = enteredLetterSB.append(_letter18.value!!)
                19 -> enteredLetterSB = enteredLetterSB.append(_letter19.value!!)
                20 -> enteredLetterSB = enteredLetterSB.append(_letter20.value!!)
                21 -> enteredLetterSB = enteredLetterSB.append(_letter21.value!!)
                22 -> enteredLetterSB = enteredLetterSB.append(_letter22.value!!)
                23 -> enteredLetterSB = enteredLetterSB.append(_letter23.value!!)
                24 -> enteredLetterSB = enteredLetterSB.append(_letter24.value!!)
                25 -> enteredLetterSB = enteredLetterSB.append(_letter25.value!!)
                26 -> enteredLetterSB = enteredLetterSB.append(_letter26.value!!)
                27 -> enteredLetterSB = enteredLetterSB.append(_letter27.value!!)
                28 -> enteredLetterSB = enteredLetterSB.append(_letter28.value!!)
                29 -> enteredLetterSB = enteredLetterSB.append(_letter29.value!!)
                30 -> enteredLetterSB = enteredLetterSB.append(_letter30.value!!)
                31 -> enteredLetterSB = enteredLetterSB.append(_letter31.value!!)
                32 -> enteredLetterSB = enteredLetterSB.append(_letter32.value!!)
                33 -> enteredLetterSB = enteredLetterSB.append(_letter33.value!!)
                34 -> enteredLetterSB = enteredLetterSB.append(_letter34.value!!)
                35 -> enteredLetterSB = enteredLetterSB.append(_letter35.value!!)
                36 -> enteredLetterSB = enteredLetterSB.append(_letter36.value!!)
                37 -> enteredLetterSB = enteredLetterSB.append(_letter37.value!!)
                38 -> enteredLetterSB = enteredLetterSB.append(_letter38.value!!)
                39 -> enteredLetterSB = enteredLetterSB.append(_letter39.value!!)
                40 -> enteredLetterSB = enteredLetterSB.append(_letter40.value!!)
                41 -> enteredLetterSB = enteredLetterSB.append(_letter41.value!!)
                42 -> enteredLetterSB = enteredLetterSB.append(_letter42.value!!)
                43 -> enteredLetterSB = enteredLetterSB.append(_letter43.value!!)
                44 -> enteredLetterSB = enteredLetterSB.append(_letter44.value!!)
                45 -> enteredLetterSB = enteredLetterSB.append(_letter45.value!!)
                46 -> enteredLetterSB = enteredLetterSB.append(_letter46.value!!)
                47 -> enteredLetterSB = enteredLetterSB.append(_letter47.value!!)
                48 -> enteredLetterSB = enteredLetterSB.append(_letter48.value!!)
            }

        var j = 0

        while (enteredLetterSB.isNotEmpty()) {
            val i = (Math.random() * enteredLetterSB.length).toInt()

            shuffledLetters[j] = enteredLetterSB[i]
            enteredLetterSB = enteredLetterSB.removeRange(i, i + 1) as StringBuilder
            j++
        }

        for (i in 0..49)
            when (i) {
                0 -> _letter0.value = shuffledLetters[0]
                1 -> _letter1.value = shuffledLetters[1]
                2 -> _letter2.value = shuffledLetters[2]
                3 -> _letter3.value = shuffledLetters[3]
                4 -> _letter4.value = shuffledLetters[4]
                5 -> _letter5.value = shuffledLetters[5]
                6 -> _letter6.value = shuffledLetters[6]
                7 -> _letter7.value = shuffledLetters[7]
                8 -> _letter8.value = shuffledLetters[8]
                9 -> _letter9.value = shuffledLetters[9]
                10 -> _letter10.value = shuffledLetters[10]
                11 -> _letter11.value = shuffledLetters[11]
                12 -> _letter12.value = shuffledLetters[12]
                13 -> _letter13.value = shuffledLetters[13]
                14 -> _letter14.value = shuffledLetters[14]
                15 -> _letter15.value = shuffledLetters[15]
                16 -> _letter16.value = shuffledLetters[16]
                17 -> _letter17.value = shuffledLetters[17]
                18 -> _letter18.value = shuffledLetters[18]
                19 -> _letter19.value = shuffledLetters[19]
                20 -> _letter20.value = shuffledLetters[20]
                21 -> _letter21.value = shuffledLetters[21]
                22 -> _letter22.value = shuffledLetters[22]
                23 -> _letter23.value = shuffledLetters[23]
                24 -> _letter24.value = shuffledLetters[24]
                25 -> _letter25.value = shuffledLetters[25]
                26 -> _letter26.value = shuffledLetters[26]
                27 -> _letter27.value = shuffledLetters[27]
                28 -> _letter28.value = shuffledLetters[28]
                29 -> _letter29.value = shuffledLetters[29]
                30 -> _letter30.value = shuffledLetters[30]
                31 -> _letter31.value = shuffledLetters[31]
                32 -> _letter32.value = shuffledLetters[32]
                33 -> _letter33.value = shuffledLetters[33]
                34 -> _letter34.value = shuffledLetters[34]
                35 -> _letter35.value = shuffledLetters[35]
                36 -> _letter36.value = shuffledLetters[36]
                37 -> _letter37.value = shuffledLetters[37]
                38 -> _letter38.value = shuffledLetters[38]
                39 -> _letter39.value = shuffledLetters[39]
                40 -> _letter40.value = shuffledLetters[40]
                41 -> _letter41.value = shuffledLetters[41]
                42 -> _letter42.value = shuffledLetters[42]
                43 -> _letter43.value = shuffledLetters[43]
                44 -> _letter44.value = shuffledLetters[44]
                45 -> _letter45.value = shuffledLetters[45]
                46 -> _letter46.value = shuffledLetters[46]
                47 -> _letter47.value = shuffledLetters[47]
                48 -> _letter48.value = shuffledLetters[48]
            }

        resetTray = true
    }

    fun unShuffleLetters() {

        for (i in 0..48)
            when (i) {
                0 -> _letter0.value = _enteredLetter0.value
                1 -> _letter1.value = _enteredLetter1.value
                2 -> _letter2.value = _enteredLetter2.value
                3 -> _letter3.value = _enteredLetter3.value
                4 -> _letter4.value = _enteredLetter4.value
                5 -> _letter5.value = _enteredLetter5.value
                6 -> _letter6.value = _enteredLetter6.value
                7 -> _letter7.value = _enteredLetter7.value
                8 -> _letter8.value = _enteredLetter8.value
                9 -> _letter9.value = _enteredLetter9.value
                10 -> _letter10.value = _enteredLetter10.value
                11 -> _letter11.value = _enteredLetter11.value
                12 -> _letter12.value = _enteredLetter12.value
                13 -> _letter13.value = _enteredLetter13.value
                14 -> _letter14.value = _enteredLetter14.value
                15 -> _letter15.value = _enteredLetter15.value
                16 -> _letter16.value = _enteredLetter16.value
                17 -> _letter17.value = _enteredLetter17.value
                18 -> _letter18.value = _enteredLetter18.value
                19 -> _letter19.value = _enteredLetter19.value
                20 -> _letter20.value = _enteredLetter20.value
                21 -> _letter21.value = _enteredLetter21.value
                22 -> _letter22.value = _enteredLetter22.value
                23 -> _letter23.value = _enteredLetter23.value
                24 -> _letter24.value = _enteredLetter24.value
                25 -> _letter25.value = _enteredLetter25.value
                26 -> _letter26.value = _enteredLetter26.value
                27 -> _letter27.value = _enteredLetter27.value
                28 -> _letter28.value = _enteredLetter28.value
                29 -> _letter29.value = _enteredLetter29.value
                30 -> _letter30.value = _enteredLetter30.value
                31 -> _letter31.value = _enteredLetter31.value
                32 -> _letter32.value = _enteredLetter32.value
                33 -> _letter33.value = _enteredLetter33.value
                34 -> _letter34.value = _enteredLetter34.value
                35 -> _letter35.value = _enteredLetter35.value
                36 -> _letter36.value = _enteredLetter36.value
                37 -> _letter37.value = _enteredLetter37.value
                38 -> _letter38.value = _enteredLetter38.value
                39 -> _letter39.value = _enteredLetter39.value
                40 -> _letter40.value = _enteredLetter40.value
                41 -> _letter41.value = _enteredLetter41.value
                42 -> _letter42.value = _enteredLetter42.value
                43 -> _letter43.value = _enteredLetter43.value
                44 -> _letter44.value = _enteredLetter44.value
                45 -> _letter45.value = _enteredLetter45.value
                46 -> _letter46.value = _enteredLetter46.value
                47 -> _letter47.value = _enteredLetter47.value
                48 -> _letter48.value = _enteredLetter48.value
            }
    }

    private fun resetTray() {

        for (i in 0..48)
            when (i) {
                0 -> _enteredLetter0.value = _letter0.value
                1 -> _enteredLetter1.value = _letter1.value
                2 -> _enteredLetter2.value = _letter2.value
                3 -> _enteredLetter3.value = _letter3.value
                4 -> _enteredLetter4.value = _letter4.value
                5 -> _enteredLetter5.value = _letter5.value
                6 -> _enteredLetter6.value = _letter6.value
                7 -> _enteredLetter7.value = _letter7.value
                8 -> _enteredLetter8.value = _letter8.value
                9 -> _enteredLetter9.value = _letter9.value
                10 -> _enteredLetter10.value = _letter10.value
                11 -> _enteredLetter11.value = _letter11.value
                12 -> _enteredLetter12.value = _letter12.value
                13 -> _enteredLetter13.value = _letter13.value
                14 -> _enteredLetter14.value = _letter14.value
                15 -> _enteredLetter15.value = _letter15.value
                16 -> _enteredLetter16.value = _letter16.value
                17 -> _enteredLetter17.value = _letter17.value
                18 -> _enteredLetter18.value = _letter18.value
                19 -> _enteredLetter19.value = _letter19.value
                20 -> _enteredLetter20.value = _letter20.value
                21 -> _enteredLetter21.value = _letter21.value
                22 -> _enteredLetter22.value = _letter22.value
                23 -> _enteredLetter23.value = _letter23.value
                24 -> _enteredLetter24.value = _letter24.value
                25 -> _enteredLetter25.value = _letter25.value
                26 -> _enteredLetter26.value = _letter26.value
                27 -> _enteredLetter27.value = _letter27.value
                28 -> _enteredLetter28.value = _letter28.value
                29 -> _enteredLetter29.value = _letter29.value
                30 -> _enteredLetter30.value = _letter30.value
                31 -> _enteredLetter31.value = _letter31.value
                32 -> _enteredLetter32.value = _letter32.value
                33 -> _enteredLetter33.value = _letter33.value
                34 -> _enteredLetter34.value = _letter34.value
                35 -> _enteredLetter35.value = _letter35.value
                36 -> _enteredLetter36.value = _letter36.value
                37 -> _enteredLetter37.value = _letter37.value
                38 -> _enteredLetter38.value = _letter38.value
                39 -> _enteredLetter39.value = _letter39.value
                40 -> _enteredLetter40.value = _letter40.value
                41 -> _enteredLetter41.value = _letter41.value
                42 -> _enteredLetter42.value = _letter42.value
                43 -> _enteredLetter43.value = _letter43.value
                44 -> _enteredLetter44.value = _letter44.value
                45 -> _enteredLetter45.value = _letter45.value
                46 -> _enteredLetter46.value = _letter46.value
                47 -> _enteredLetter47.value = _letter47.value
                48 -> _enteredLetter48.value = _letter48.value
            }
    }

    private fun countLetters(): Int {

        var count = 0

        for (i in 0..48)
            when (i) {
                0 -> if (_letter0.value?.isLetter() == true) count++
                1 -> if (_letter1.value?.isLetter() == true) count++
                2 -> if (_letter2.value?.isLetter() == true) count++
                3 -> if (_letter3.value?.isLetter() == true) count++
                4 -> if (_letter4.value?.isLetter() == true) count++
                5 -> if (_letter5.value?.isLetter() == true) count++
                6 -> if (_letter6.value?.isLetter() == true) count++
                7 -> if (_letter7.value?.isLetter() == true) count++
                8 -> if (_letter8.value?.isLetter() == true) count++
                9 -> if (_letter9.value?.isLetter() == true) count++
                10 -> if (_letter10.value?.isLetter() == true) count++
                11 -> if (_letter11.value?.isLetter() == true) count++
                12 -> if (_letter12.value?.isLetter() == true) count++
                13 -> if (_letter13.value?.isLetter() == true) count++
                14 -> if (_letter14.value?.isLetter() == true) count++
                15 -> if (_letter15.value?.isLetter() == true) count++
                16 -> if (_letter16.value?.isLetter() == true) count++
                17 -> if (_letter17.value?.isLetter() == true) count++
                18 -> if (_letter18.value?.isLetter() == true) count++
                19 -> if (_letter19.value?.isLetter() == true) count++
                20 -> if (_letter20.value?.isLetter() == true) count++
                21 -> if (_letter21.value?.isLetter() == true) count++
                22 -> if (_letter22.value?.isLetter() == true) count++
                23 -> if (_letter23.value?.isLetter() == true) count++
                24 -> if (_letter24.value?.isLetter() == true) count++
                25 -> if (_letter25.value?.isLetter() == true) count++
                26 -> if (_letter26.value?.isLetter() == true) count++
                27 -> if (_letter27.value?.isLetter() == true) count++
                28 -> if (_letter28.value?.isLetter() == true) count++
                29 -> if (_letter29.value?.isLetter() == true) count++
                30 -> if (_letter30.value?.isLetter() == true) count++
                31 -> if (_letter31.value?.isLetter() == true) count++
                32 -> if (_letter32.value?.isLetter() == true) count++
                33 -> if (_letter33.value?.isLetter() == true) count++
                34 -> if (_letter34.value?.isLetter() == true) count++
                35 -> if (_letter35.value?.isLetter() == true) count++
                36 -> if (_letter36.value?.isLetter() == true) count++
                37 -> if (_letter37.value?.isLetter() == true) count++
                38 -> if (_letter38.value?.isLetter() == true) count++
                39 -> if (_letter39.value?.isLetter() == true) count++
                40 -> if (_letter40.value?.isLetter() == true) count++
                41 -> if (_letter41.value?.isLetter() == true) count++
                42 -> if (_letter42.value?.isLetter() == true) count++
                43 -> if (_letter43.value?.isLetter() == true) count++
                44 -> if (_letter44.value?.isLetter() == true) count++
                45 -> if (_letter45.value?.isLetter() == true) count++
                46 -> if (_letter46.value?.isLetter() == true) count++
                47 -> if (_letter47.value?.isLetter() == true) count++
                48 -> if (_letter48.value?.isLetter() == true) count++
            }
        return count
    }

    private fun shuffledLettersToTiles(count: Int): Array<Tile> {

        val gameTiles = Array(count) { Tile(null, TileState.EMPTY, TilePos(0, 0)) }

        var j = 0

        for (i in 0..48)
            when (i) {
                0 -> if (_letter0.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter0.value.toString(), TileState.WRONG, TilePos(0, 0))
                    j++
                }
                1 -> if (_letter1.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter1.value.toString(), TileState.WRONG, TilePos(0, 1))
                    j++
                }
                2 -> if (_letter2.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter2.value.toString(), TileState.WRONG, TilePos(0, 2))
                    j++
                }

                3 -> if (_letter3.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter3.value.toString(), TileState.WRONG, TilePos(0, 3))
                    j++
                }

                4 -> if (_letter4.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter4.value.toString(), TileState.WRONG, TilePos(0, 4))
                    j++

                }
                5 -> if (_letter5.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter5.value.toString(), TileState.WRONG, TilePos(0, 5))
                    j++

                }
                6 -> if (_letter6.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter6.value.toString(), TileState.WRONG, TilePos(0, 6))
                    j++

                }
                7 -> if (_letter7.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter7.value.toString(), TileState.WRONG, TilePos(1, 0))
                    j++

                }
                8 -> if (_letter8.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter8.value.toString(), TileState.WRONG, TilePos(1, 1))
                    j++

                }
                9 -> if (_letter9.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter9.value.toString(), TileState.WRONG, TilePos(1, 2))
                    j++

                }
                10 -> if (_letter10.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter10.value.toString(), TileState.WRONG, TilePos(1, 3))
                    j++

                }
                11 -> if (_letter11.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter11.value.toString(), TileState.WRONG, TilePos(1, 4))
                    j++

                }
                12 -> if (_letter12.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter12.value.toString(), TileState.WRONG, TilePos(1, 5))
                    j++

                }
                13 -> if (_letter13.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter13.value.toString(), TileState.WRONG, TilePos(1, 6))
                    j++

                }
                14 -> if (_letter14.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter14.value.toString(), TileState.WRONG, TilePos(2, 0))
                    j++
                    Tile(null, TileState.EMPTY, TilePos(2, 0))
                }
                15 -> if (_letter15.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter15.value.toString(), TileState.WRONG, TilePos(2, 1))
                    j++
                }
                16 -> if (_letter16.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter16.value.toString(), TileState.WRONG, TilePos(2, 2))
                    j++
                }
                17 -> if (_letter17.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter17.value.toString(), TileState.WRONG, TilePos(2, 3))
                    j++
                }
                18 -> if (_letter18.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter18.value.toString(), TileState.WRONG, TilePos(2, 4))
                    j++
                }
                19 -> if (_letter19.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter19.value.toString(), TileState.WRONG, TilePos(2, 5))
                    j++
                }
                20 -> if (_letter20.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter20.value.toString(), TileState.WRONG, TilePos(2, 6))
                    j++
                }
                21 -> if (_letter21.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter21.value.toString(), TileState.WRONG, TilePos(3, 0))
                    j++
                }
                22 -> if (_letter22.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter22.value.toString(), TileState.WRONG, TilePos(3, 1))
                    j++
                }
                23 -> if (_letter23.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter23.value.toString(), TileState.WRONG, TilePos(3, 2))
                    j++
                }
                24 -> if (_letter24.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter24.value.toString(), TileState.WRONG, TilePos(3, 3))
                    j++
                }
                25 -> if (_letter25.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter25.value.toString(), TileState.WRONG, TilePos(3, 4))
                    j++
                }
                26 -> if (_letter26.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter26.value.toString(), TileState.WRONG, TilePos(3, 5))
                    j++
                }
                27 -> if (_letter27.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter27.value.toString(), TileState.WRONG, TilePos(3, 6))
                    j++
                }
                28 -> if (_letter28.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter28.value.toString(), TileState.WRONG, TilePos(4, 0))
                    j++
                }
                29 -> if (_letter29.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter29.value.toString(), TileState.WRONG, TilePos(4, 1))
                    j++
                }
                30 -> if (_letter30.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter30.value.toString(), TileState.WRONG, TilePos(4, 2))
                    j++
                }
                31 -> if (_letter31.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter31.value.toString(), TileState.WRONG, TilePos(4, 3))
                    j++
                }
                32 -> if (_letter32.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter32.value.toString(), TileState.WRONG, TilePos(4, 4))
                    j++
                }
                33 -> if (_letter33.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter33.value.toString(), TileState.WRONG, TilePos(4, 5))
                    j++
                }
                34 -> if (_letter34.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter34.value.toString(), TileState.WRONG, TilePos(4, 6))
                    j++
                }
                35 -> if (_letter35.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter35.value.toString(), TileState.WRONG, TilePos(5, 0))
                    j++
                }
                36 -> if (_letter36.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter36.value.toString(), TileState.WRONG, TilePos(5, 1))
                    j++
                }
                37 -> if (_letter37.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter37.value.toString(), TileState.WRONG, TilePos(5, 2))
                    j++
                }
                38 -> if (_letter38.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter38.value.toString(), TileState.WRONG, TilePos(5, 3))
                    j++
                }
                39 -> if (_letter39.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter39.value.toString(), TileState.WRONG, TilePos(5, 4))
                    j++
                    Tile(null, TileState.EMPTY, TilePos(5, 4))
                }
                40 -> if (_letter40.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter40.value.toString(), TileState.WRONG, TilePos(5, 5))
                    j++
                }
                41 -> if (_letter41.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter41.value.toString(), TileState.WRONG, TilePos(5, 6))
                    j++
                }
                42 -> if (_letter42.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter42.value.toString(), TileState.WRONG, TilePos(6, 0))
                    j++
                }
                43 -> if (_letter43.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter43.value.toString(), TileState.WRONG, TilePos(6, 1))
                    j++
                }
                44 -> if (_letter44.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter44.value.toString(), TileState.WRONG, TilePos(6, 2))
                    j++
                }
                45 -> if (_letter45.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter45.value.toString(), TileState.WRONG, TilePos(6, 3))
                    j++
                    Tile(null, TileState.EMPTY, TilePos(6, 3))
                }
                46 -> if (_letter46.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter46.value.toString(), TileState.WRONG, TilePos(6, 4))
                    j++
                }
                47 -> if (_letter47.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter47.value.toString(), TileState.WRONG, TilePos(6, 5))
                    j++
                }
                48 -> if (_letter48.value?.isLetter() == true) {
                    gameTiles[j] = Tile(_letter48.value.toString(), TileState.WRONG, TilePos(6, 6))
                    j++
                }

            }

        return gameTiles
    }

    private fun enteredLettersToTiles(count: Int): Array<Tile> {

        val gameTiles = Array(count) { Tile(null, TileState.EMPTY, TilePos(0, 0)) }

        var j = 0

        for (i in 0..48)
            when (i) {
                0 -> if (_enteredLetter0.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter0.value.toString(), TileState.WRONG, TilePos(0, 0))
                    j++
                }
                1 -> if (_enteredLetter1.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter1.value.toString(), TileState.WRONG, TilePos(0, 1))
                    j++
                }
                2 -> if (_enteredLetter2.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter2.value.toString(), TileState.WRONG, TilePos(0, 2))
                    j++
                }

                3 -> if (_enteredLetter3.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter3.value.toString(), TileState.WRONG, TilePos(0, 3))
                    j++
                }

                4 -> if (_enteredLetter4.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter4.value.toString(), TileState.WRONG, TilePos(0, 4))
                    j++

                }
                5 -> if (_enteredLetter5.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter5.value.toString(), TileState.WRONG, TilePos(0, 5))
                    j++

                }
                6 -> if (_enteredLetter6.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter6.value.toString(), TileState.WRONG, TilePos(0, 6))
                    j++

                }
                7 -> if (_enteredLetter7.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter7.value.toString(), TileState.WRONG, TilePos(1, 0))
                    j++

                }
                8 -> if (_enteredLetter8.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter8.value.toString(), TileState.WRONG, TilePos(1, 1))
                    j++

                }
                9 -> if (_enteredLetter9.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter9.value.toString(), TileState.WRONG, TilePos(1, 2))
                    j++

                }
                10 -> if (_enteredLetter10.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter10.value.toString(), TileState.WRONG, TilePos(1, 3))
                    j++

                }
                11 -> if (_enteredLetter11.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter11.value.toString(), TileState.WRONG, TilePos(1, 4))
                    j++

                }
                12 -> if (_enteredLetter12.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter12.value.toString(), TileState.WRONG, TilePos(1, 5))
                    j++

                }
                13 -> if (_enteredLetter13.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter13.value.toString(), TileState.WRONG, TilePos(1, 6))
                    j++

                }
                14 -> if (_enteredLetter14.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter14.value.toString(), TileState.WRONG, TilePos(2, 0))
                    j++
                    Tile(null, TileState.EMPTY, TilePos(2, 0))
                }
                15 -> if (_enteredLetter15.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter15.value.toString(), TileState.WRONG, TilePos(2, 1))
                    j++
                }
                16 -> if (_enteredLetter16.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter16.value.toString(), TileState.WRONG, TilePos(2, 2))
                    j++
                }
                17 -> if (_enteredLetter17.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter17.value.toString(), TileState.WRONG, TilePos(2, 3))
                    j++
                }
                18 -> if (_enteredLetter18.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter18.value.toString(), TileState.WRONG, TilePos(2, 4))
                    j++
                }
                19 -> if (_enteredLetter19.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter19.value.toString(), TileState.WRONG, TilePos(2, 5))
                    j++
                }
                20 -> if (_enteredLetter20.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter20.value.toString(), TileState.WRONG, TilePos(2, 6))
                    j++
                }
                21 -> if (_enteredLetter21.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter21.value.toString(), TileState.WRONG, TilePos(3, 0))
                    j++
                }
                22 -> if (_enteredLetter22.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter22.value.toString(), TileState.WRONG, TilePos(3, 1))
                    j++
                }
                23 -> if (_enteredLetter23.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter23.value.toString(), TileState.WRONG, TilePos(3, 2))
                    j++
                }
                24 -> if (_enteredLetter24.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter24.value.toString(), TileState.WRONG, TilePos(3, 3))
                    j++
                }
                25 -> if (_enteredLetter25.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter25.value.toString(), TileState.WRONG, TilePos(3, 4))
                    j++
                }
                26 -> if (_enteredLetter26.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter26.value.toString(), TileState.WRONG, TilePos(3, 5))
                    j++
                }
                27 -> if (_enteredLetter27.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter27.value.toString(), TileState.WRONG, TilePos(3, 6))
                    j++
                }
                28 -> if (_enteredLetter28.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter28.value.toString(), TileState.WRONG, TilePos(4, 0))
                    j++
                }
                29 -> if (_enteredLetter29.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter29.value.toString(), TileState.WRONG, TilePos(4, 1))
                    j++
                }
                30 -> if (_enteredLetter30.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter30.value.toString(), TileState.WRONG, TilePos(4, 2))
                    j++
                }
                31 -> if (_enteredLetter31.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter31.value.toString(), TileState.WRONG, TilePos(4, 3))
                    j++
                }
                32 -> if (_enteredLetter32.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter32.value.toString(), TileState.WRONG, TilePos(4, 4))
                    j++
                }
                33 -> if (_enteredLetter33.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter33.value.toString(), TileState.WRONG, TilePos(4, 5))
                    j++
                }
                34 -> if (_enteredLetter34.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter34.value.toString(), TileState.WRONG, TilePos(4, 6))
                    j++
                }
                35 -> if (_enteredLetter35.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter35.value.toString(), TileState.WRONG, TilePos(5, 0))
                    j++
                }
                36 -> if (_enteredLetter36.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter36.value.toString(), TileState.WRONG, TilePos(5, 1))
                    j++
                }
                37 -> if (_enteredLetter37.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter37.value.toString(), TileState.WRONG, TilePos(5, 2))
                    j++
                }
                38 -> if (_enteredLetter38.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter38.value.toString(), TileState.WRONG, TilePos(5, 3))
                    j++
                }
                39 -> if (_enteredLetter39.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter39.value.toString(), TileState.WRONG, TilePos(5, 4))
                    j++
                    Tile(null, TileState.EMPTY, TilePos(5, 4))
                }
                40 -> if (_enteredLetter40.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter40.value.toString(), TileState.WRONG, TilePos(5, 5))
                    j++
                }
                41 -> if (_enteredLetter41.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter41.value.toString(), TileState.WRONG, TilePos(5, 6))
                    j++
                }
                42 -> if (_enteredLetter42.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter42.value.toString(), TileState.WRONG, TilePos(6, 0))
                    j++
                }
                43 -> if (_enteredLetter43.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter43.value.toString(), TileState.WRONG, TilePos(6, 1))
                    j++
                }
                44 -> if (_enteredLetter44.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter44.value.toString(), TileState.WRONG, TilePos(6, 2))
                    j++
                }
                45 -> if (_enteredLetter45.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter45.value.toString(), TileState.WRONG, TilePos(6, 3))
                    j++
                    Tile(null, TileState.EMPTY, TilePos(6, 3))
                }
                46 -> if (_enteredLetter46.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter46.value.toString(), TileState.WRONG, TilePos(6, 4))
                    j++
                }
                47 -> if (_enteredLetter47.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter47.value.toString(), TileState.WRONG, TilePos(6, 5))
                    j++
                }
                48 -> if (_enteredLetter48.value?.isLetter() == true) {
                    gameTiles[j] =
                        Tile(_enteredLetter48.value.toString(), TileState.WRONG, TilePos(6, 6))
                    j++
                }
            }
        return gameTiles
    }

    fun saveGame() {
        if ((_gameDuration.value!! < 5) || (_gameDuration.value!! > 20)) {
            _toastMsg.value = Event(TOAST_MSGS.OUT_OF_RANGE)
            return
        }

        val count = countLetters()

 //     Log.w("GameCreateModel saveGame", "letters $count")

        if (count > 0) {
            val mills: Long = (1000L * 60L * _gameDuration.value!!)
            val date = Date()

            val gameCreator =
                PreferenceManager.getDefaultSharedPreferences(getApplication()).getString(
                    getApplication<Application>().resources.getString(R.string.keyGameCreator),
                    getApplication<Application>().resources.getString(R.string.gameCreatorDefault)
                ) ?: getApplication<Application>().resources.getString(R.string.gameCreatorDefault)

            viewModelScope.launch {

                val sortedTiles = enteredLettersToTiles(count)
                val shuffledTiles = shuffledLettersToTiles(count)

                dataRepository.insert(
                    Game(
                        0,
                        count,
                        0,
                        mills,
                        date,
                        gameCreator,
                        dataRepository.gameTagFromTiles(sortedTiles, shuffledTiles, gameStr),
                        GAME_STATUS.C,
                        shuffledTiles
                    )
                )

                dataRepository.insert(
                    Game(
                        0,
                        count,
                        0,
                        mills,
                        date,
                        gameCreator,
                        dataRepository.gameTagFromTiles(sortedTiles, shuffledTiles, answerStr),
                        GAME_STATUS.SA,
                        sortedTiles // an answer
                    )
                )
                _toastMsg.value = Event(TOAST_MSGS.GAME_SAVED)
            }
        }
    }
  }

data class UIState(
    val rows: Int,
    val cols: Int,
)

open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /*
      /**
      * Returns the content, even if it's already been handled.
      **/
     fun peekContent(): T = content
     */
}

class WordBoxViewModelFactory(
    private val application: Wordbox
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(WordBoxViewModel::class.java)) {
          return WordBoxViewModel(application) as T
      }
      throw IllegalAccessException("Unknown WordboxViewModel class")
    }

}


