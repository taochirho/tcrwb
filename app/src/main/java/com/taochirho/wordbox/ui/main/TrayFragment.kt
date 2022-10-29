package com.taochirho.wordbox.ui.main

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.database.TilePos
import com.taochirho.wordbox.database.TileState
import com.taochirho.wordbox.databinding.TrayFragmentBinding
import com.taochirho.wordbox.model.GameModel
import com.taochirho.wordbox.model.GameModelFactory

class TrayFragment : Fragment() {
//    private val TAG = "TrayFragment"
    private val swapID = 16474
    private val startID = 27585

    private var trayTileX = 60
    private var trayTileHeight = 56f
    private var textHeight = 14f
//TODO replace these magic numbers

    private lateinit var viewModel: GameModel
    private lateinit var binding: TrayFragmentBinding

    private val mOnDragScratchpadListener: View.OnDragListener
    private val mOnDragSwapListener: View.OnDragListener

    init {

        mOnDragScratchpadListener = View.OnDragListener { _ , dragEvent: DragEvent -> doScratchPadDrag(dragEvent) }
        mOnDragSwapListener = View.OnDragListener { view: View, dragEvent: DragEvent -> doSwapDrag(view, dragEvent) }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {

        val application = requireNotNull(this.activity).application as Wordbox
        viewModel = activity?.let { ViewModelProvider(it.viewModelStore, GameModelFactory(application))[GameModel::class.java] }!!

        binding = DataBindingUtil.inflate(inflater, R.layout.tray_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        trayTileHeight = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            resources.displayMetrics.widthPixels / 10.0f
        } else {
            resources.displayMetrics.heightPixels / 10.0f
        }

        trayTileX = (trayTileHeight * 1.10f).toInt()
        textHeight = trayTileHeight * 0.3f

        val theTrayObserver = Observer<GameModel.Tray> {
           setOutTray(it)
        }
        viewModel.theTray.observe(viewLifecycleOwner, theTrayObserver)
        binding.tray.setOnDragListener(mOnDragScratchpadListener)
    }

    private fun setOutTray(tray: GameModel.Tray) {

        var x: Int
        var y: Int

        binding.tray.removeAllViews()
        val cs = ConstraintSet()

        val swapTile = TCRSwapTile(context)
        swapTile.id = swapID
        swapTile.background = context?.let { ContextCompat.getDrawable(it, R.drawable.tile_swap) }
        swapTile.textSize = (resources.getDimension(R.dimen.tile_swap_height).toInt() / 5).toFloat()
        swapTile.gravity = Gravity.BOTTOM + Gravity.END
        swapTile.setPadding(0,0,16,4)
        swapTile.connectViewModel(viewModel, this)
        swapTile.setOnDragListener(mOnDragSwapListener)

        binding.tray.addView(swapTile)

        cs.clone(binding.tray)
        cs.constrainHeight(swapTile.id,  resources.getDimension(R.dimen.tile_swap_height).toInt())
        cs.constrainWidth(swapTile.id,  resources.getDimension(R.dimen.tile_swap_height).toInt())
        cs.connect(swapTile.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, resources.getDimension(R.dimen.tile_swap_margin).toInt())
        cs.connect(swapTile.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, resources.getDimension(R.dimen.tile_swap_margin).toInt())

        cs.applyTo(binding.tray)

        val startTile = TCRStartTile(context)
        startTile.id = startID
        startTile.background = context?.let { ContextCompat.getDrawable(it, R.drawable.tile_start_selector) }
        startTile.connectViewModel(viewModel, this)

        binding.tray.addView(startTile)

        cs.clone(binding.tray)
        cs.constrainHeight(startTile.id,  resources.getDimension(R.dimen.tile_swap_height).toInt())
        cs.constrainWidth(startTile.id,  resources.getDimension(R.dimen.tile_swap_height).toInt())
        cs.connect(startTile.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, resources.getDimension(R.dimen.tile_swap_margin).toInt())
        cs.connect(startTile.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, resources.getDimension(R.dimen.tile_swap_margin).toInt())

        cs.applyTo(binding.tray)

        val margin = resources.getDimension(R.dimen.scratchpad_padding).toInt()

        if (tray.positionsSet) {
            for (i in 0 until viewModel.tileCount.value!!) {

                if (tray[i].letter != null) {

                    x = tray[i].tilePos.row
                    y = tray[i].tilePos.col

                    if (x == -1) { // i.e. tile added by double tap in grid
                        val row = i / 6
                        val col = i % 6

                        x = (trayTileX * col) + margin
                        y = (trayTileX * row) + margin
                    }

                    val newTile = TCRTile(context)

                    newTile.id = viewModel.trayTileID + i
                    newTile.text = tray[i].letter
                    newTile.textSize = textHeight
                    newTile.gravity = Gravity.CENTER
                    newTile.includeFontPadding = false
                    newTile.tileState = TileState.IN_TRAY
                    newTile.background = context?.let { ContextCompat.getDrawable(it, R.drawable.tile_game_selector) }

                    newTile.tag = TilePos(y, x, i)
                    newTile.connectViewModel(viewModel, TilePos(y, x, i))
                    binding.tray.addView(newTile)

                    cs.clone(binding.tray)
                    cs.constrainHeight(newTile.id, trayTileHeight.toInt())
                    cs.constrainWidth(newTile.id, trayTileHeight.toInt())
                    cs.connect(newTile.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, y)
                    cs.connect(newTile.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, x)
                    cs.applyTo(binding.tray)
                }
            }
        } else {
            for (i in 0 until viewModel.tileCount.value!!) {

                if (tray[i].letter != null) {
                    val row = i / 6
                    val col = i % 6

                    x = (trayTileX * col) + margin
                    y = (trayTileX * row) + margin
                    viewModel.setTrayPosition(i, x, y)

                    val newTile = TCRTile(context)

                    newTile.id = viewModel.trayTileID + i
                    newTile.tileState = TileState.IN_TRAY
                    newTile.background = context?.let { ContextCompat.getDrawable(it, R.drawable.tile_game_selector) }
                    newTile.text = tray[i].letter
                    newTile.textSize = textHeight
                    newTile.gravity = Gravity.CENTER
                    newTile.includeFontPadding = false
                    newTile.tag = TilePos(y, x, i)
                    newTile.connectViewModel(viewModel, TilePos(y, x, i))

                    binding.tray.addView(newTile)

                    cs.clone(binding.tray)
                    cs.constrainHeight(newTile.id, trayTileHeight.toInt())
                    cs.constrainWidth(newTile.id, trayTileHeight.toInt())
                    cs.connect(newTile.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, y)
                    cs.connect(newTile.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, x)
                    cs.applyTo(binding.tray)
                }
            }
        }
        viewModel.positions(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun doScratchPadDrag(dragEvent: DragEvent): Boolean {

        if (dragEvent.action == DragEvent.ACTION_DROP) {
            val localState = dragEvent.localState as TileState
//            Log.w("TrayFragment", "doScratchPadDrag - ACTION_DROP $localState")
            val x = dragEvent.x.toInt()
            val y = dragEvent.y.toInt()

            if  (localState == TileState.IN_TRAY ) {
                viewModel.setTrayPositionOnMove(viewModel.tileInfoFromClipData(dragEvent.clipData).trayIndex, x, y)
            } else {
                viewModel.removeTileFromGrid(TilePos(viewModel.tileInfoFromClipData(dragEvent.clipData).row, viewModel.tileInfoFromClipData(dragEvent.clipData).col))
                viewModel.addTileToTray(viewModel.tileInfoFromClipData(dragEvent.clipData).letter ?: "", x, y )
            }
        }
        return true
    }


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
                view.background = context?.let { ContextCompat.getDrawable(it, R.drawable.tile_swap_entered) }
            }
            DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> {
                view.background = context?.let { ContextCompat.getDrawable(it, R.drawable.tile_swap) }
            }
            DragEvent.ACTION_DROP -> {
                viewModel.swapLetter(viewModel.tileInfoFromClipData(dragEvent.clipData).trayIndex )
            }
        }
        return true
    }
}


