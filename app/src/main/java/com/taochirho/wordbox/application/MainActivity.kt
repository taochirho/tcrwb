package com.taochirho.wordbox.application


import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth


import com.google.gson.Gson
import com.taochirho.wordbox.R
import com.taochirho.wordbox.database.Converters
import com.taochirho.wordbox.database.Game
import com.taochirho.wordbox.database.SentGame
import com.taochirho.wordbox.model.WordBoxViewModel
import com.taochirho.wordbox.model.WordBoxViewModelFactory


class MainActivity : AppCompatActivity() {
    private val TAG = "Wordbox MainActivity"

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { result ->
        this.onSignInResult(result)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult?) {
       if (result == null) {
           Log.w(TAG, "User cancelled log in ")
       } else {
           if (result.resultCode == RESULT_OK) {
               // Successfully signed in
               val user = FirebaseAuth.getInstance().currentUser
           } else {
               val response = result.idpResponse
               Log.w(TAG, response.toString())
           }
       }

    }

    val actionCodeSettings = ActionCodeSettings.newBuilder()
        .setAndroidPackageName(
            "com.taochirho.wordbox",
            true,
            null)   //minimumVersion=
        .setHandleCodeInApp(true) // This must be set to true
        .setUrl("https://wordbox-b0d3c.firebaseapp.com") // This URL needs to be whitelisted
        .build()

    // Choose authentication providers
    val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build())

    // Create and launch sign-in intent
    val signInIntent = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers)
        .setLogo(R.mipmap.wb_launcher)
        .setTheme(R.style.Theme_TCRWordbox)
        .build()




    private lateinit var wordboxVM: WordBoxViewModel

   lateinit var analytics: FirebaseAnalytics



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Class.forName("dalvik.system.CloseGuard")
                .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                .invoke(null, true)
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }

        analytics = FirebaseAnalytics.getInstance(this)

        signInLauncher.launch(signInIntent)


        wordboxVM = this.let {
            ViewModelProvider(
                it.viewModelStore,
                WordBoxViewModelFactory(application as Wordbox)
            )[WordBoxViewModel::class.java]
        }



        if ((intent?.action == Intent.ACTION_SEND)) {
            handleSendJson(intent) // Handle text being sent
        }


/*
        val user = hashMapOf(
            "first" to "Ada",
            "last" to "Lovelace",
            "born" to 1815

        )

        firestoreDB.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

        val user2 = hashMapOf(
            "first" to "Alan",
            "middle" to "Mathison",
            "last" to "Turing",
            "born" to 1912
        )

        firestoreDB.collection("users")
            .add(user2)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }*/


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


            val receivedGame = Gson().fromJson(it, SentGame::class.java)

            Log.w("Activity","gametiles ${receivedGame.gameString}")
            analytics.logEvent("game_received", null)

            val gameTiles = Converters().fromStringToArrayTiles(receivedGame.gameString.substring(0,8))

            val status = if (receivedGame.status == GAME_STATUS.SA) {
                GAME_STATUS.RA // i.e. received an answer
            } else {
                GAME_STATUS.C // i.e. received a challenge
            }


            if (status == GAME_STATUS.RA) {
                wordboxVM.storeGame(
                    Game(
                        0,
                        gameTiles.size,
                        0,
                        receivedGame.timeSet,
                        receivedGame.dateSaved,
                        receivedGame.gameFrom,
                        receivedGame.gameTag,
                        status,
                        gameTiles
                    )
                )
            } else {
                wordboxVM.setCurrentGame(
                    Game(
                        0,
                        gameTiles.size,
                        0,
                        receivedGame.timeSet,
                        receivedGame.dateSaved,
                        receivedGame.gameFrom,
                        receivedGame.gameTag,
                        status,
                        gameTiles
                    )
                )
            }

            // Update UI to reflect text being shared
        }
    }
}