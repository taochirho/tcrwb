package com.taochirho.wordbox.ui.main



import android.content.res.Configuration

import android.graphics.Color

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController


import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.taochirho.wordbox.R
import com.taochirho.wordbox.database.*
import com.taochirho.wordbox.databinding.GamesListFragmentBinding
import com.taochirho.wordbox.model.WordBoxViewModel
import kotlinx.coroutines.*

class GamesListFragment : Fragment() {


    private val TAG = "GamesListFragment"
    lateinit var binding: GamesListFragmentBinding
    private val wordboxVM: WordBoxViewModel by activityViewModels()

    private val glfJob = Job()
    private val glfScope = CoroutineScope(Dispatchers.Main + glfJob)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        binding = DataBindingUtil.inflate(inflater, R.layout.games_list_fragment, container, false)
        binding.gamesListModel = wordboxVM
        binding.lifecycleOwner = viewLifecycleOwner


        val adapter = GamesListAdapter(
            GamesListAdapter.GameDeleteListener { game -> onGameDeleteClicked(game) },
            GamesListAdapter.GameSendListener { uid -> onGameSendClicked(uid) },
            GamesListAdapter.GameRestoreListener { uid -> onGameRestoreClicked(uid) },
            GamesListAdapter.GameStarToggleListener { game -> onGameToggleLetterClicked(game) })


        binding.gamesList.adapter = adapter

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            val verticalDivider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            context?.let { it ->
                ContextCompat.getDrawable(it, R.drawable.line_divider_portrait)
                    ?.let { verticalDivider.setDrawable(it) }
            }
            binding.gamesList.addItemDecoration(verticalDivider)
            binding.gamesList.layoutManager =
                GridLayoutManager(this.activity, 1, GridLayoutManager.VERTICAL, false)
        } else {
            val horizontalDivider = DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL)
            context?.let { adapterContext ->
                ContextCompat.getDrawable(adapterContext, R.drawable.line_divider_landscapet)
                    ?.let { horizontalDivider.setDrawable(it) }
            }
            binding.gamesList.addItemDecoration(horizontalDivider)
            binding.gamesList.layoutManager =
                GridLayoutManager(this.activity, 1, GridLayoutManager.HORIZONTAL, false)
        }

        wordboxVM.allGames.observe(viewLifecycleOwner) {
            it?.let { adapter.submitList(it) }
        }

        /*wordboxVM.navigateToGame.observe(viewLifecycleOwner) { uid ->
                  uid?.let {
                this.findNavController()
                    .navigate(GamesListFragmentDirections.actionGamesListToGameFragment())
                wordboxVM.onNavigatedToGame()
            }
        }*/
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.TCRListToolbar.inflateMenu(R.menu.boxset_games_list_menu)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.TCRListToolbar.title = resources.getString(R.string.app_name)
            binding.TCRListToolbar.setTitleTextColor(MaterialColors.getColor(
                requireContext(),
                R.attr.colorOnPrimary,
                Color.GREEN
            ))

            binding.TCRListToolbar.subtitle = resources.getString(R.string.app_subtitle_list)
            binding.TCRListToolbar.setSubtitleTextColor(MaterialColors.getColor(
                requireContext(),
                R.attr.colorOnSecondary,
                Color.GREEN
            ))



        } else {
            val title = resources.getString(R.string.app_name)
            val subTitle = resources.getString(R.string.app_subtitle_list)

            val newTitle = SpannableString("$title $subTitle")
            newTitle.setSpan(
                ForegroundColorSpan(
                    MaterialColors.getColor(
                        requireContext(),
                        R.attr.colorOnPrimary,
                        Color.MAGENTA
                    )
                ), 18, newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // newTitle.setSpan(RelativeSizeSpan(0.8f),title.length + 1 , newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.TCRListToolbar.title = newTitle
        }

        binding.TCRListToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.phone_clear -> {
                    wordboxVM.clearGames()
                    true
                }
                else -> {
                    true
                }
            }
        }
    }

    override fun onDestroy() {
        glfJob.cancel()
        super.onDestroy()
    }

    private fun onGameDeleteClicked(game: Game) {
        wordboxVM.gameDelete(game)
    }

    private fun onGameRestoreClicked(uid: Int) {
        glfScope.launch {
            wordboxVM.setCurrentGame(wordboxVM.getGame(uid))
        }
        findNavController().navigate(R.id.gameFragment)
    }

    private fun onGameToggleLetterClicked(game: Game) {
        wordboxVM.gameToggleLetter(game)
    }

    private fun onGameSendClicked(game: Game) {

        Log.w("GamesListFragment", "clicked")

        val firestoreDB = Firebase.firestore

        val fbGame = SentGame(
            game.timeSet,
            game.gameFrom,
            game.gameTag,
            game.dateSaved,
            game.status,
            stringFromTiles(game.gameTiles)
        )

       /* val fbGame = hashMapOf(

            "timeSet" to game.timeSet,
            "gameFrom" to game.gameFrom,
            "gameTag" to game.gameTag,
            "dateSaved" to game.dateSaved,
            "status" to game.status,
            "gameString" to stringFromTiles(game.gameTiles)

        )*/

        firestoreDB.collection("games").document(game.gameTag).set(fbGame)
           // .add(fbGame)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${game.gameTag}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }



        /*glfScope.launch {

            val game = wordboxVM.getGame(uid)

            val sentGame = SentGame(
                game.timeSet,
                game.gameFrom,
                game.gameTag,
                game.dateSaved,
                game.status,
                Converters().toStringFromArrayTiles(game.gameTiles)
            )

            val json = Gson().toJson(sentGame)

            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                //   component =  ComponentName(this@MainActivity.baseContext,  MainActivity::class.java)
                putExtra(Intent.EXTRA_TEXT, json)


                putExtra(Intent.EXTRA_TITLE, "Wordbox from Dave Rawcliffe")
                type = "text/json"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context?.let { ContextCompat.startActivity(it, shareIntent, null) }
        }*/
    }
}
