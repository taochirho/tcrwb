package com.taochirho.testvm


import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.taochirho.testvm.application.GAME_STATUS
import com.taochirho.testvm.application.TestVMApplication
import com.taochirho.testvm.database.Converters
import com.taochirho.testvm.database.CurrentGameEntity
import com.taochirho.testvm.database.Game
import com.taochirho.testvm.database.SentGame
import com.taochirho.testvm.model.GameModel
import com.taochirho.testvm.model.GameModelFactory
import java.util.*


class MainActivity : AppCompatActivity() {


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Class.forName("dalvik.system.CloseGuard")
                .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                .invoke(null, true)
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }

        if ((intent?.action == Intent.ACTION_SEND))  {
            handleSendJson(intent) // Handle text being sent
        }


/*

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
//                .detectDiskReads()
//                .detectDiskWrites()
                .detectAll() // or .detectNetwork for network problems
                .penaltyLog()
//                .penaltyDeath()
                .build()
        )


        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
 */

        setContentView(R.layout.main_activity)



    }

    private fun handleSendJson(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Log.w("handleSendJson", "it: $it")
            val application = requireNotNull(this).application as TestVMApplication
            val gameVM = this?.let { ViewModelProvider(it.viewModelStore, GameModelFactory(application)).get(
                GameModel::class.java) }!!

            val receivedGame = Gson().fromJson(it, SentGame::class.java)
            val gameTiles = Converters().fromStringToArrayTiles(receivedGame.gameString)

            val status = if (receivedGame.status == GAME_STATUS.SA) {
                GAME_STATUS.RA // i.e. received an answer
            } else {
                GAME_STATUS.C // i.e. received a challenge
            }

            val restoredGame = Game(
                0,
                gameTiles.size,
                receivedGame.timeLeft,
                receivedGame.dateSaved,
                receivedGame.gameFrom,
                receivedGame.gameTag,
                status,
                gameTiles
            )

            if (status == GAME_STATUS.RA) {
                gameVM.storeGame(restoredGame)
            } else {
                gameVM.storeCurrentGame(restoredGame)
            }


            // Update UI to reflect text being shared
        }
    }
}