package com.taochirho.testvm.application

import android.app.Application
import com.taochirho.testvm.database.GameDatabase
import com.taochirho.testvm.database.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class TestVMApplication :Application() {

    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { GameDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { GameRepository(database.gameDao(), database.currentGameDao() ) }
}