package com.taochirho.wordbox.application

import android.app.Application
import com.taochirho.wordbox.database.GameDatabase
import com.taochirho.wordbox.database.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class Wordbox :Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { GameDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { GameRepository(database.gameDao(), database.currentGameDao() ) }
}