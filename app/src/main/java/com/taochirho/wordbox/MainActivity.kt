package com.taochirho.wordbox


import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.database.Converters
import com.taochirho.wordbox.database.Game
import com.taochirho.wordbox.database.SentGame
import com.taochirho.wordbox.model.GameModel
import com.taochirho.wordbox.model.GameModelFactory


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Class.forName("dalvik.system.CloseGuard")
                .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                .invoke(null, true)
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }

        if ((intent?.action == Intent.ACTION_SEND)) {
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

            val application = requireNotNull(this).application as Wordbox
            val gameVM = this.let {
                ViewModelProvider(it.viewModelStore, GameModelFactory(application)).get(
                    GameModel::class.java
                )
            }

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