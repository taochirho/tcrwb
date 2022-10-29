package com.taochirho.wordbox.application

import android.app.Application
import com.taochirho.wordbox.database.GameDatabase
import com.taochirho.wordbox.database.GameRepository

class Wordbox :Application() {

    val database by lazy { GameDatabase.getDatabase(this) }
    val repository by lazy { GameRepository(database.gameDao(), database.currentGameDao() ) }
}