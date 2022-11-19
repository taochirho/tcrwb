package com.taochirho.wordbox.model


import androidx.lifecycle.*
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.database.Game
import com.taochirho.wordbox.database.GameStatus
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException

class GamesListModel(
    application: Wordbox
) : AndroidViewModel(application) {

    private val dataRepository = application.repository
    val allGames: LiveData<List<Game>> = dataRepository.allGames.asLiveData()

    fun clearGames() {
        viewModelScope.launch {
            dataRepository.clear()
        }
    }

    private val _navigateToGame = MutableLiveData<Int?>()
    val navigateToGame: LiveData<Int?>
        get() = _navigateToGame

    fun gameRestore(uid: Int) {
        _navigateToGame.value = uid
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

    fun onNavigatedToGame() {
        _navigateToGame.value = null
    }

    fun gameDelete(game: Game) {
        viewModelScope.launch {
            dataRepository.delete(game)
        }
    }

    suspend fun getGame(uid: Int): Game {
        return dataRepository.getGameWithUid(uid)
    }
}

class GamesListModelFactory(
    private val application: Wordbox
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(GamesListModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GamesListModel(application) as T
        }
        throw IllegalArgumentException("Unknown GamesListViewModel class")
    }
}





