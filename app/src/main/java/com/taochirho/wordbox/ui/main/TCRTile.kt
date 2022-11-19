package com.taochirho.wordbox.ui.main


import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import com.taochirho.wordbox.R
import com.taochirho.wordbox.database.TilePos
import com.taochirho.wordbox.database.TileState
import com.taochirho.wordbox.model.GameModel


class TCRTile : androidx.appcompat.widget.AppCompatTextView  {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)

    private lateinit var mGestureDetector: GestureDetectorCompat
    private var viewModel : GameModel? = null
    private lateinit var tilePos: TilePos


    private var startX = 0F
    private var startY = 0F
    private val delta = 100F

    var tileState = TileState.EMPTY
        set(value) {
            field = value
            refreshDrawableState()
            invalidate()
        }

    fun connectViewModel(viewModel: GameModel, tilePos: TilePos) {
        this.viewModel = viewModel
        this.tilePos = tilePos

        mGestureDetector = GestureDetectorCompat(context, TCRTileGestureListener(viewModel, tilePos, tileState ))
        mGestureDetector.setIsLongpressEnabled(false)
        }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        if (tileState == null) {  //ignore warning that this is always false.  App crashes if this test is removed
            return super.onCreateDrawableState(extraSpace)
        }

        val state = super.onCreateDrawableState(extraSpace + 1)

        when (tileState) {
            TileState.IN_TRAY -> mergeDrawableStates(state, intArrayOf(R.attr.in_tray))
            TileState.EMPTY -> mergeDrawableStates(state, intArrayOf(R.attr.empty))
            TileState.RIGHT -> mergeDrawableStates(state, intArrayOf(R.attr.right))
            TileState.NEARLY_RIGHT -> mergeDrawableStates(state, intArrayOf(R.attr.nearly_right))
            TileState.WRONG -> mergeDrawableStates(state, intArrayOf(R.attr.wrong))
            TileState.ENTERED -> mergeDrawableStates(state, intArrayOf(R.attr.entered))
            TileState.LETTER_ENTERED  -> mergeDrawableStates(state, intArrayOf(R.attr.letter_entered))
        }
        return state
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val gm = viewModel ?: return false

        if (gm.timerRunning.value != true) {
            return false
        }

        if (tileState == TileState.EMPTY) {
            return false
        }

        if (event != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    mGestureDetector.onTouchEvent(event)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dX = startX - event.rawX
                    val dY = startY - event.rawY

                    if ( ((dX * dX) + (dY * dY)) > delta ) {
                        gm.startDragPos = this.tag as TilePos
                        if (tileState == TileState.IN_TRAY) {
                            startDragAndDrop(gm.clipDataFromTileView(this.text.toString(), (this.tag as TilePos).row, (this.tag as TilePos).col, (this.tag as TilePos).trayIndex), DragShadowBuilder(this ), tileState, 0)
                        } else {
                            startDragAndDrop(gm.clipDataFromTileView(this.text.toString(), (this.tag as TilePos).row, (this.tag as TilePos).col, -1), DragShadowBuilder(this ), tileState, 0)
                        }

                    } else {
                        mGestureDetector.onTouchEvent(event)
                    }
                 }

                else -> mGestureDetector.onTouchEvent(event)
            }
        }
        if (event != null) {
            mGestureDetector.onTouchEvent(event)
        }
        return true
    }

    private class TCRTileGestureListener(private val gm: GameModel, private val tilePos: TilePos, private val tileState:TileState): GestureDetector.SimpleOnGestureListener() {

        override fun onDoubleTap(e: MotionEvent): Boolean {
            if ((tileState == TileState.IN_TRAY)){  // ignore double taps in tray
                return true
            }
            gm.addTileToTray(gm.getLetterFromGrid(tilePos) ?: "" , -1, -1)
            gm.removeTileFromGrid(tilePos)
            return true
        }
  }
}