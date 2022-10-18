package com.taochirho.testvm.model

import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.*
import com.google.gson.Gson
import com.taochirho.testvm.application.GAME_STATUS

import com.taochirho.testvm.application.TestVMApplication
import com.taochirho.testvm.database.Converters
import com.taochirho.testvm.database.Game
import com.taochirho.testvm.database.GameStatus
import com.taochirho.testvm.database.SentGame
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import java.util.*







class GamesListModel(
    application: TestVMApplication
) : AndroidViewModel(application) {

    private val dataRepository = application.repository
    private val context = application.applicationContext


//    private val _currentGame = MutableLiveData<Game>()
//    val currentGame: LiveData<Game>
//        get() = _currentGame

  /*  val allGames : LiveData<List<Game>> =
        dataRepository.getAllGames().asLiveData()
*/

    val allGames: LiveData<List<Game>> = dataRepository.allGames.asLiveData()

    fun clearGames() {
        viewModelScope.launch {
            dataRepository.clear()
        }
    }


    private val _viewLetters: MutableLiveData<Boolean> = MutableLiveData(true)
    val viewLetters : LiveData<Boolean> = _viewLetters

    private val _navigateToGame = MutableLiveData<Int?>()
    val navigateToGame :LiveData<Int?>
        get() = _navigateToGame

    fun onGameRestoreClicked(uid: Int) {
        _navigateToGame.value = uid
    }

    fun onGameToggleLetterClicked(game: Game) {

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

    fun onNavigatedToGame() {
        _navigateToGame.value = null
    }

    fun onGameSendClicked(uid: Int) {

        viewModelScope.launch {
            val game = dataRepository.getGameWithUid(uid)

            val sentGame = SentGame(
                game.timeLeft,
                game.gameFrom,
                game.gameTag,
                game.dateSaved,
                game.status,
                Converters().toStringFromArrayTiles(game.gameTiles)
            )

            val json =  Gson().toJson(sentGame)

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                //   component =  ComponentName(this@MainActivity.baseContext,  MainActivity::class.java)
                putExtra(Intent.EXTRA_TEXT, json)
                putExtra(Intent.EXTRA_TITLE, "Wordbox from Dave Rawcliffe")
                type = "text/json"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(context, shareIntent, null)

        }
    }

    fun onGameDeleteClicked(game: Game) {
        viewModelScope.launch {
            dataRepository.delete(game)
        }
    }
}

class GamesListModelFactory(
    private val application: TestVMApplication
) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(GamesListModel::class.java)) {
            return GamesListModel(application) as T
        }
        throw IllegalArgumentException("Unknown GamesListViewModel class")
    }
}





