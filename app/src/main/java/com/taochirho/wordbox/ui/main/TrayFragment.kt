package com.taochirho.wordbox.ui.main

import android.annotation.SuppressLint


import android.os.Bundle

import android.view.*
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.color.MaterialColors
import com.taochirho.wordbox.R

import com.taochirho.wordbox.database.TilePos
import com.taochirho.wordbox.database.TileState
import com.taochirho.wordbox.databinding.TrayFragmentBinding
import com.taochirho.wordbox.model.WordBoxViewModel
import kotlin.math.floor
import kotlin.math.sqrt

class TrayFragment : Fragment(), ViewTreeObserver.OnGlobalLayoutListener  {
    //    private val TAG = "TrayFragment"
    private val wordboxVM: WordBoxViewModel by activityViewModels()

//  private val swapID = 16474

    private var trayHeight = 400
    private var trayWidth = 200
    private var tileHeight = 64


    private lateinit var binding: TrayFragmentBinding

    private val mOnDragScratchpadListener: View.OnDragListener
//    private val mOnDragSwapListener: View.OnDragListener

    init {

        mOnDragScratchpadListener =
            View.OnDragListener { _, dragEvent: DragEvent -> doScratchPadDrag(dragEvent) }
      /*  mOnDragSwapListener =
            View.OnDragListener { view: View, dragEvent: DragEvent -> doSwapDrag(view, dragEvent) }*/
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = DataBindingUtil.inflate(inflater, R.layout.tray_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tray.setOnDragListener(mOnDragScratchpadListener)
        view.viewTreeObserver.addOnGlobalLayoutListener(this)
        val theTrayObserver = Observer<WordBoxViewModel.Tray> {
            setOutTray()
        }

        wordboxVM.theTray.observe(viewLifecycleOwner, theTrayObserver)
    }

    private fun setOutTray() {
        val tray = wordboxVM.theTray.value ?: return
        val margin = resources.getDimension(R.dimen.scratchpad_padding).toInt()
        val trayHeightAvailable = view?.height?.minus((2 * margin)) ?: return
        val trayWidthAvailable = view?.width?.minus(resources.getDimension(R.dimen.tile_swap_height).toInt() + (2 * margin)) ?: return

        if ((trayHeightAvailable <= 0) || (trayWidthAvailable <= 0)) {
            return
        }


        val tc = wordboxVM.tileCount.value ?: 30

        val trayAreaPerTile = (trayWidthAvailable * trayHeightAvailable) / tc
        val th = sqrt(trayAreaPerTile.toDouble()).toInt() - margin
        val cols = floor((trayWidthAvailable / th).toDouble()).toInt()


        var rows = tc / cols
        if ((tc % cols) > 0) {
            rows += 1
        }

        tileHeight = th.coerceAtMost((trayHeightAvailable / rows) - margin)

        var x: Int
        var y: Int

        val density = resources.displayMetrics.density * 1.2F // the 1.2 further reduces text size

        binding.tray.removeAllViews()
        val cs = ConstraintSet()

        /*val swapTile = TCRSwapTile(context)
        swapTile.id = swapID
        swapTile.background = context?.let { ContextCompat.getDrawable(it, R.drawable.tile_swap) }
        swapTile.textSize =
            (resources.getDimension(R.dimen.tile_swap_height).toInt() / (1.5f * density))
        swapTile.gravity = Gravity.BOTTOM + Gravity.END
        swapTile.setPadding(0, 0, 16, 4)
        swapTile.connectViewModel(wordboxVM, this)
        swapTile.setOnDragListener(mOnDragSwapListener)

        binding.tray.addView(swapTile)

        cs.clone(binding.tray)
        cs.constrainHeight(swapTile.id, resources.getDimension(R.dimen.tile_swap_height).toInt())
        cs.constrainWidth(swapTile.id, resources.getDimension(R.dimen.tile_swap_height).toInt())
        cs.connect(
            swapTile.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            resources.getDimension(R.dimen.tile_swap_margin).toInt()
        )
        cs.connect(
            swapTile.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            resources.getDimension(R.dimen.tile_swap_margin).toInt()
        )
        cs.applyTo(binding.tray)
*/
        if (tray.positionsSet) {
            for (i in 0 until wordboxVM.tileCount.value!!) {

                if (tray[i].letter != null) {

                    x = tray[i].tilePos.row
                    y = tray[i].tilePos.col

                    if (x == -1) { // i.e. tile added by double tap in grid
                        val row = i / 6
                        val col = i % 6

                        x = (tileHeight * col) + margin
                        y = (tileHeight * row) + margin
                    }

                    val newTile = TCRTile(context)

                    newTile.id = wordboxVM.trayTileID + i
                    newTile.text = tray[i].letter
                    newTile.textSize = tileHeight / density
                    newTile.setTextColor(
                        MaterialColors.getColor(
                            requireContext(),
                            R.attr.colorOnPrimary,
                            android.graphics.Color.DKGRAY
                        )
                    )

                    newTile.gravity = Gravity.CENTER
                    newTile.includeFontPadding = false
                    newTile.tileState = TileState.IN_TRAY
                    newTile.background = context?.let {
                        ContextCompat.getDrawable(
                            it,
                            R.drawable.tile_game_selector
                        )
                    }

                    newTile.tag = TilePos(y, x, i)
                    newTile.connectViewModel(wordboxVM, TilePos(y, x, i))
                    binding.tray.addView(newTile)

                    cs.clone(binding.tray)
                    cs.constrainHeight(newTile.id, tileHeight)
                    cs.constrainWidth(newTile.id, tileHeight)
                    cs.connect(
                        newTile.id,
                        ConstraintSet.TOP,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.TOP,
                        y
                    )
                    cs.connect(
                        newTile.id,
                        ConstraintSet.LEFT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.LEFT,
                        x
                    )
                    cs.applyTo(binding.tray)
                }
            }
        } else {
            for (i in 0 until wordboxVM.tileCount.value!!) {

                if (tray[i].letter != null) {
                    val row = i / cols
                    val col = i % cols

                    x =
                        ((tileHeight + margin) * col) + resources.getDimension(R.dimen.tile_swap_margin)
                            .toInt()
                    y =
                        ((tileHeight + margin) * row) + resources.getDimension(R.dimen.tile_swap_margin)
                            .toInt()

                    wordboxVM.setTrayPosition(i, x, y)

                    val newTile = TCRTile(context)

                    newTile.id = wordboxVM.trayTileID + i
                    newTile.tileState = TileState.IN_TRAY
                    newTile.background = context?.let {
                        ContextCompat.getDrawable(
                            it,
                            R.drawable.tile_game_selector
                        )
                    }
                    newTile.text = tray[i].letter
                    newTile.textSize = tileHeight / density
                    newTile.setTextColor(
                        MaterialColors.getColor(
                            requireContext(),
                            R.attr.colorOnPrimary,
                            android.graphics.Color.DKGRAY
                        )
                    )
                    newTile.gravity = Gravity.CENTER
                    newTile.includeFontPadding = false
                    newTile.tag = TilePos(y, x, i)
                    newTile.connectViewModel(wordboxVM, TilePos(y, x, i))

                    binding.tray.addView(newTile)

                    cs.clone(binding.tray)
                    cs.constrainHeight(newTile.id, tileHeight)
                    cs.constrainWidth(newTile.id, tileHeight)
                    cs.connect(
                        newTile.id,
                        ConstraintSet.TOP,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.TOP,
                        y
                    )
                    cs.connect(
                        newTile.id,
                        ConstraintSet.LEFT,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.LEFT,
                        x
                    )
                    cs.applyTo(binding.tray)
                }
            }
        }
        wordboxVM.positions(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doScratchPadDrag(dragEvent: DragEvent): Boolean {

        if (dragEvent.action == DragEvent.ACTION_DROP) {
            val localState = dragEvent.localState as TileState
// Log.w("doScratchPadDrag", "w $trayWidth h $trayHeight th $tileHeight AD x ${dragEvent.x}  y ${dragEvent.y} $localState")
            var x = dragEvent.x.toInt()
            var y = dragEvent.y.toInt()

            // ensure whole tile is visible in tray.  Note DragEvent is not triggered when drop point is outside tray.

            if (x > trayWidth - tileHeight ){
                x = trayWidth - tileHeight
            }

            if (y > trayHeight - tileHeight ){
                y = trayHeight - tileHeight
            }

        //    Log.w("doScratch adj", "x $x y $y")


            with(wordboxVM) {

                if (localState == TileState.IN_TRAY) {
                    setTrayPositionOnMove(tileInfoFromClipData(dragEvent.clipData).trayIndex, x, y)
                } else {
                    removeTileFromGrid(
                        TilePos(
                            tileInfoFromClipData(dragEvent.clipData).row,
                            tileInfoFromClipData(dragEvent.clipData).col
                        )
                    )
                    addTileToTray(tileInfoFromClipData(dragEvent.clipData).letter ?: "", x, y)
                }
            }
        }
        return true
    }
/*
    @SuppressLint("SetTextI18n")
    private fun doSwapDrag(view: View, dragEvent: DragEvent): Boolean {

        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                return if (dragEvent.localState != null) {
                    (dragEvent.localState as TileState) == TileState.IN_TRAY
                } else {
                    false
                }
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                view.background =
                    context?.let { ContextCompat.getDrawable(it, R.drawable.tile_swap_entered) }
            }
            DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                view.background =
                    context?.let { ContextCompat.getDrawable(it, R.drawable.tile_swap) }
            }
            DragEvent.ACTION_DROP -> {
                wordboxVM.swapLetter(wordboxVM.tileInfoFromClipData(dragEvent.clipData).trayIndex)
            }
        }
        return true
    }*/

    override fun onGlobalLayout() {

        trayHeight = view?.height!!
        trayWidth = view?.width!!
        view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        setOutTray()
    }
}


