package com.taochirho.wordbox.ui.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.color.MaterialColors
import com.google.gson.Gson
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.database.*
import com.taochirho.wordbox.databinding.GamesListFragmentBinding
import com.taochirho.wordbox.model.GamesListModel
import com.taochirho.wordbox.model.GamesListModelFactory
import kotlinx.coroutines.*
import java.util.*



class GamesListFragment : Fragment() {
    //    private val TAG = "GamesListFragment"
    lateinit var binding: GamesListFragmentBinding
    private lateinit var gamesListVM: GamesListModel
    private val glfJob = Job()
    private val glfScope = CoroutineScope(Dispatchers.Main + glfJob)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val application = requireNotNull(this.activity).application as Wordbox

        gamesListVM = ViewModelProvider(
            this,
            GamesListModelFactory(application)
        )[GamesListModel::class.java]

        binding = DataBindingUtil.inflate(inflater, R.layout.games_list_fragment, container, false)
        binding.gamesListModel = gamesListVM
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

        gamesListVM.allGames.observe(viewLifecycleOwner) {
            it?.let { adapter.submitList(it) }
        }

        gamesListVM.navigateToGame.observe(viewLifecycleOwner) { uid ->
            uid?.let {
                this.findNavController()
                    .navigate(GamesListFragmentDirections.actionGamesListToGameFragment(uid, false))
                gamesListVM.onNavigatedToGame()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.TCRListToolbar.inflateMenu(R.menu.boxset_games_list_menu)

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.TCRListToolbar.title = resources.getString(R.string.app_name)
            binding.TCRListToolbar.subtitle = resources.getString(R.string.app_subtitle_list)
        } else {
            val title = resources.getString(R.string.app_name)
            val subTitle = resources.getString(R.string.app_subtitle_list)

            val newTitle = SpannableString("$title $subTitle")
            newTitle.setSpan(
                ForegroundColorSpan(
                    MaterialColors.getColor(
                        requireContext(),
                        R.attr.subtitleTextColor,
                        Color.MAGENTA
                    )
                ), 18, newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            // newTitle.setSpan(RelativeSizeSpan(0.8f),title.length + 1 , newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            binding.TCRListToolbar.title = newTitle
        }

        binding.TCRListToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.clear_games -> {
                    gamesListVM.clearGames()
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
        gamesListVM.gameDelete(game)
    }

    private fun onGameRestoreClicked(uid: Int) {
        gamesListVM.gameRestore(uid)
    }

    private fun onGameToggleLetterClicked(game: Game) {
        gamesListVM.gameToggleLetter(game)
    }

    private fun onGameSendClicked(uid: Int) {
        glfScope.launch {

            val game = gamesListVM.getGame(uid)

            val sentGame = SentGame(
                game.timeLeft,
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
        }
    }
}
