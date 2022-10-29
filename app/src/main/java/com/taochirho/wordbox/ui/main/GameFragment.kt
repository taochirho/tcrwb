package com.taochirho.wordbox.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.taochirho.wordbox.CreatePuzzle
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.databinding.GameFragmentBinding
import com.taochirho.wordbox.model.GameModel
import com.taochirho.wordbox.model.GameModelFactory

class GameFragment : Fragment() {
//    private val TAG = "GameFragment"
    private val args: GameFragmentArgs by navArgs()

    /*// Receiver
    private val getNewGame =
        registerForActivityResult(GetNewGame())
        {

            if (it == null) {
                Toast.makeText(
                    this.context,
                    "Failed new game null",
                    Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    this.context,
                    "Game created by: " + it.gameFrom,
                    Toast.LENGTH_LONG).show()

         //   gameVM.restoreGame(it)
            }

        }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        childFragmentManager.beginTransaction()
            .add(R.id.grid_container, GridFragment())
            .add(R.id.tray_container, TrayFragment())
            .commit()
    }

    private var _bindingGame: GameFragmentBinding? = null
    // This property is only valid between onCreateView and
// onDestroyView.
    private val bindingGame get() = _bindingGame!!
    private lateinit var gameVM: GameModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val application = requireNotNull(this.activity).application as Wordbox
        gameVM = activity?.let { ViewModelProvider(it.viewModelStore, GameModelFactory(application)).get(GameModel::class.java) }!!

        if (args.restoreCurrent) {
            gameVM.restoreCurrentGame()
        } else {
            gameVM.retrieveGame(args.gameID)
        }

        _bindingGame = GameFragmentBinding.inflate(inflater, container, false)

        return bindingGame.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindingGame.TCRToolbar.inflateMenu(R.menu.boxset_menu)


        bindingGame.TCRToolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.new_game_grid -> {
                    gameVM.loadGrid(true) { ::gameVM.get().randomLetter() }
                    true
                }
                R.id.new_game_tray -> {
                    gameVM.loadTray(true) { ::gameVM.get().randomLetter() }
                    true
                }
                R.id.save_game -> {
                    gameVM.saveGame()
                    true
                }
                R.id.create_game -> {
                    val intent = Intent(this.context, CreatePuzzle::class.java)
                    startActivity(intent)
                    true
                }
                R.id.gamesList -> {
                    findNavController().navigate(R.id.gamesList)
                    true
                }
                R.id.restart -> {
                    gameVM.startTimer()
                    true
                }
                R.id.shuffle_game_grid -> {
                    gameVM.loadGrid(false) { ::gameVM.get().shuffledLetter() }
                    true
                }
                R.id.shuffle_game_tray -> {
                    gameVM.loadTray(false) { ::gameVM.get().shuffledLetter() }
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
/*   how to find and decorate a menuitem
       val mi = bindingGame.TCRToolbar.menu.findItem(com.taochirho.testvm.R.id.current)

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



        gameVM.gameTag.observe(viewLifecycleOwner, gameTagObserver)
        gameVM.gameCreator.observe(viewLifecycleOwner, gameCreatorObserver)
        gameVM.millisecondsLeft.observe(viewLifecycleOwner, timeLeftObserver)
        gameVM.score.observe(viewLifecycleOwner, scoreObserver)
      /*  gameVM.duration.observe(viewLifecycleOwner, durationObserver)
        gameVM.tileCount.observe(viewLifecycleOwner, tilesObserver)*/

    }

    val timeLeftObserver = Observer<Long> {

        if (it > 999) { // timer does not go down to 0!
            var seconds = (it / 1000).toInt()
            val minutes = seconds / 60
            seconds %= 60
            bindingGame.timer.text =
                String.format(resources.configuration.locales[0], "%d:%02d", minutes, seconds)
        } else {
            bindingGame.timer.text = resources.getText(R.string.timer_ended)
        }
    }

    private val scoreObserver = Observer<Int> {
        bindingGame.score.text = it.toString()
    }

    private val gameTagObserver = Observer<String> {
       //bindingGame.TCRToolbar.subtitle =  String.format(resources.getString(R.string.app_subtitle), it)
        bindingGame.TCRToolbar.findViewById<TextView>(R.id.wb_subtitle).text = it
     }

    private val gameCreatorObserver = Observer<String> {
        //bindingGame.TCRToolbar.subtitle =  String.format(resources.getString(R.string.app_subtitle), it)
        bindingGame.TCRToolbar.findViewById<TextView>(R.id.wb_gameCreator).text = it
    }


    override fun onDestroyView() {

        gameVM.updateCurrentGame()
        super.onDestroyView()
        _bindingGame = null
    }
}
/*

class GetNewGame : ActivityResultContract<Intent, Game?>() {
    override fun createIntent(context: Context, input: Intent): Intent {
        var intent = Intent(context, CreatePuzzle::class.java)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Game? = when
    {
        resultCode != Activity.RESULT_OK -> null      // Return null, if action is cancelled
        else -> intent?.getParcelableExtra("newGame")
    }

}

*/

