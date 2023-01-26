package com.taochirho.wordbox.ui.main


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.application.STARTER_ACTION
import com.taochirho.wordbox.database.CurrentGame

import com.taochirho.wordbox.databinding.GameFragmentBinding

import com.taochirho.wordbox.model.WordBoxViewModel

class GameFragment : Fragment() {
    //    private val TAG = "GameFragment"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) { // test here so multiple fragments aren't created
            childFragmentManager.beginTransaction()
                .add(R.id.grid_container, GridFragment())
                .add(R.id.tray_container, TrayFragment())
                .commit()
        }
    }

    private var _bindingGame: GameFragmentBinding? = null

    // This property is only valid between onCreateView and
// onDestroyView.
    private val bindingGame get() = _bindingGame!!
    private val wordboxVM: WordBoxViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _bindingGame = GameFragmentBinding.inflate(inflater, container, false)
        return bindingGame.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindingGame.starter.setOnClickListener {
            wordboxVM.gameStart()
        }

        bindingGame.TCRToolbar.inflateMenu(R.menu.boxset_menu)
        bindingGame.TCRToolbar.setOnMenuItemClickListener {

            when (it.itemId) {

                R.id.new_game_grid -> {
                    wordboxVM.loadGrid(true) { ::wordboxVM.get().randomLetter() }
                    true
                }
                R.id.new_game_tray -> {
                   
                    wordboxVM.loadTray(true) { ::wordboxVM.get().randomLetter() }
                    true
                }
                R.id.save_local -> {
                    wordboxVM.saveGame(GAME_STATUS.C)
                    true
                }
                R.id.save_game_challenge -> {
                    wordboxVM.saveGame(GAME_STATUS.C)
                    true
                }
                R.id.save_game_solution -> {
                    wordboxVM.saveGame(GAME_STATUS.RA)
                    true
                }
                R.id.create_game -> {
                    findNavController().navigate(R.id.gameCreate)
                    true
                }
                R.id.gamesList -> {
                    findNavController().navigate(R.id.gamesList)
                    true
                }
                R.id.reset -> {
                    wordboxVM.currentGame.value?.let { it1 -> wordboxVM.setTimer(it1.timeSet) }
                    true
                }
                R.id.shuffle_game_grid -> {
                    wordboxVM.loadGrid(false) { ::wordboxVM.get().shuffledLetter() }
                    true
                }
                R.id.shuffle_game_tray -> {
                    wordboxVM.loadTray(false) { ::wordboxVM.get().shuffledLetter() }
                    true
                }
                R.id.prefs -> {
                    findNavController().navigate(R.id.TCRPrefsFragment)
                    true
                }
                else -> {
                    true
                }
            }
        }
/*   how to find and decorate a menu item
       val mi = bindingGame.TCRToolbar.menu.findItem(com.taochirho.wordbox.R.id.current)

        val title = mi.title.toString()
        val newTitle = SpannableString(title)
        newTitle.setSpan(ForegroundColorSpan(Color.rgb(116,230,0)), 0, newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)       // newTitle.setSpan(UnderlineSpan(),0 , newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        newTitle.setSpan(RelativeSizeSpan(1.2f),0 , newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        newTitle.setSpan(UnderlineSpan(),0 , newTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        mi.title = newTitle

*/
/*
        I can't work out how to get color from theme :-(


        val color = MaterialColors.getColor(requireContext(), R.attr.textColorPrimary, Color.YELLOW)
        val typedValue = TypedValue()
        val theme = requireContext().theme
        theme.resolveAttribute(R.attr.textColorPrimary, typedValue, true)
        @ColorInt val color = typedValue.data
*/

        wordboxVM.currentGame.observe(viewLifecycleOwner, currentGameObserver)
        wordboxVM.millisecondsLeft.observe(viewLifecycleOwner, timeLeftObserver)
        wordboxVM.score.observe(viewLifecycleOwner, scoreObserver)
        wordboxVM.starterAction.observe(viewLifecycleOwner, starterActionObserver)

    }

    private val timeLeftObserver = Observer<Long> {

        if (it > 999) { // timer does not go down to 0!
            var seconds = (it / 1000).toInt()
            val minutes = seconds / 60
            seconds %= 60
            bindingGame.timer.text =
                String.format(resources.configuration.locales[0], "%d:%02d", minutes, seconds)
        } else {
            bindingGame.timer.text =
                String.format(resources.configuration.locales[0], "%d:%02d", 0, 0)
        }
    }

    private val scoreObserver = Observer<Int> {
        bindingGame.score.text = it.toString()
    }

    private val currentGameObserver = Observer<CurrentGame> {
        bindingGame.TCRToolbar.findViewById<TextView>(R.id.wb_subtitle).text = it.gameTag
        bindingGame.TCRToolbar.findViewById<TextView>(R.id.wb_gameCreator).text = it.gameFrom
        wordboxVM.loadGame(it)
    }

    private val starterActionObserver = Observer<STARTER_ACTION> {

        with(bindingGame.starter) {
            when (it) {
                STARTER_ACTION.S -> {
                    setImageDrawable(context?.let {
                        ContextCompat.getDrawable(it, R.drawable.ic_baseline_play_circle_24)
                    })
                    contentDescription = resources.getString(R.string.game_start)//"Play"//
                }
                STARTER_ACTION.P -> {
                    setImageDrawable(context?.let {
                        ContextCompat.getDrawable(it, R.drawable.ic_baseline_pause_circle_24)
                    })
                    contentDescription = resources.getString(R.string.game_pause)
                }

                STARTER_ACTION.F -> {
                    setImageDrawable(context?.let {
                        ContextCompat.getDrawable(it, R.drawable.ic_baseline_stop_circle_24)
                    })
                    contentDescription = resources.getString(R.string.restart_timer)
                }

                else -> {
                    setImageDrawable(context?.let { //observer may be null in Java
                        ContextCompat.getDrawable(it, R.drawable.ic_baseline_play_circle_24)
                    })
                    contentDescription = resources.getString(R.string.game_start)//"Play"
                }
            }
        }
    }

    override fun onDestroyView() {
        wordboxVM.updateCurrentGame()
        _bindingGame = null
        super.onDestroyView()
    }
}
