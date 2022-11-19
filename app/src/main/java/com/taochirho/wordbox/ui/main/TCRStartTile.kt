package com.taochirho.wordbox.ui.main


import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.taochirho.wordbox.R
import com.taochirho.wordbox.model.GameModel

class TCRStartTile : androidx.appcompat.widget.AppCompatTextView  {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)


    private var viewModel : GameModel? = null
    private lateinit var myParent : Fragment

    private var tileStartState = GameModel.TileStartState.TIMER
        set(value) {
            field = value
            refreshDrawableState()
            invalidate()
        }

    fun connectViewModel(viewModel: GameModel, parent: Fragment) {
        this.viewModel = viewModel
        myParent = parent
        viewModel.timerRunning.observe(myParent.viewLifecycleOwner, timerStateObserver)
        }

    val timerStateObserver = Observer<Boolean> {

        tileStartState = if (it) { //  it = timerRunning
            GameModel.TileStartState.PAUSE
        } else {
            if (viewModel?.millisecondsLeft?.value!! > 999) { // timer stops when ;ess than 1000 millisecs left
                GameModel.TileStartState.PLAY
            } else {
                GameModel.TileStartState.TIMER
            }
        }
    }


    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        if (tileStartState == null) {  //ignore warning that this is always false.  App crashes if this test is removed
            return super.onCreateDrawableState(extraSpace)
        }

        val state = super.onCreateDrawableState(extraSpace + 1)

        when (tileStartState) {
            GameModel.TileStartState.TIMER -> mergeDrawableStates(state, intArrayOf(R.attr.timer))
            GameModel.TileStartState.PAUSE -> mergeDrawableStates(state, intArrayOf(R.attr.pause))
            GameModel.TileStartState.PLAY -> mergeDrawableStates(state, intArrayOf(R.attr.play))

        }
        return state
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val gm = viewModel ?: return false

        if (event != null) {
            if (event.action == MotionEvent.ACTION_UP) {
                when (tileStartState){
                    GameModel.TileStartState.TIMER -> {
                        gm.startTimer()
                    }
                    GameModel.TileStartState.PAUSE -> {
                        gm.pauseTimer()
                    }
                    GameModel.TileStartState.PLAY -> {
                        gm.restartTimer()
                    }
                }
            }
        }
        return true
    }
}