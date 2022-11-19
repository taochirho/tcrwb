@file:Suppress("PrivatePropertyName")

package com.taochirho.wordbox.ui.main

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.database.TilePos
import com.taochirho.wordbox.database.TileState
import com.taochirho.wordbox.databinding.GridFragmentBinding
import com.taochirho.wordbox.model.GameModel
import com.taochirho.wordbox.model.GameModelFactory

@SuppressLint("ClickableViewAccessibility")
class GridFragment : Fragment() {

 /*   companion object {
        fun newInstance() = GridFragment()
    }*/

//  private val TAG = "GridFragment"

    private lateinit var viewModel: GameModel


    private lateinit var gridArray: Array<Array<TCRTile>>
    private lateinit var binding: GridFragmentBinding

    private var mOnDragGridListener: View.OnDragListener
    private var mTileState = TileState.EMPTY

   init {

        mOnDragGridListener = View.OnDragListener { view: View, dragEvent: DragEvent ->
            doDragGridMove(
                    view as TCRTile,
                    dragEvent
            )
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val application = requireNotNull(this.activity).application as Wordbox
        viewModel = activity?.let { ViewModelProvider(it.viewModelStore, GameModelFactory(application))[GameModel::class.java] }!!

        binding = DataBindingUtil.inflate(inflater, R.layout.grid_fragment, container, false)
        gridArray = arrayOf(
                arrayOf(
                        binding.R0C0,
                        binding.R0C1,
                        binding.R0C2,
                        binding.R0C3,
                        binding.R0C4,
                        binding.R0C5,
                        binding.R0C6
                ),
                arrayOf(
                        binding.R1C0,
                        binding.R1C1,
                        binding.R1C2,
                        binding.R1C3,
                        binding.R1C4,
                        binding.R1C5,
                        binding.R1C6
                ),
                arrayOf(
                        binding.R2C0,
                        binding.R2C1,
                        binding.R2C2,
                        binding.R2C3,
                        binding.R2C4,
                        binding.R2C5,
                        binding.R2C6
                ),
                arrayOf(
                        binding.R3C0,
                        binding.R3C1,
                        binding.R3C2,
                        binding.R3C3,
                        binding.R3C4,
                        binding.R3C5,
                        binding.R3C6
                ),
                arrayOf(
                        binding.R4C0,
                        binding.R4C1,
                        binding.R4C2,
                        binding.R4C3,
                        binding.R4C4,
                        binding.R4C5,
                        binding.R4C6
                ),
                arrayOf(
                        binding.R5C0,
                        binding.R5C1,
                        binding.R5C2,
                        binding.R5C3,
                        binding.R5C4,
                        binding.R5C5,
                        binding.R5C6
                ),
                arrayOf(
                        binding.R6C0,
                        binding.R6C1,
                        binding.R6C2,
                        binding.R6C3,
                        binding.R6C4,
                        binding.R6C5,
                        binding.R6C6
                )
        )
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val theGridObserver = Observer<GameModel.Grid> {

            for (row in 0..6) {
                for (col in 0..6) {
                    gridArray[row][col].text = it[TilePos(row, col)].letter
                    gridArray[row][col].includeFontPadding = false
                    gridArray[row][col].tileState = it[TilePos(row, col)].state
                }
            }
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            binding.textSize = resources.displayMetrics.widthPixels / 8.0f
        } else {
            binding.textSize = resources.displayMetrics.heightPixels / 12.0f
        }

        viewModel.theGrid.observe(viewLifecycleOwner, theGridObserver)
        setListeners()
    }
    private fun doDragGridMove(tile: TCRTile, dragEvent: DragEvent): Boolean {

        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED -> return if (dragEvent.localState != null) {
                !((dragEvent.localState as TileState) == TileState.IN_TRAY && !tile.text.equals(""))
            } else {
                true
            }

            DragEvent.ACTION_DRAG_ENTERED -> if (tile.tag != viewModel.startDragPos) {
                mTileState = tile.tileState
                tile.tileState = TileState.ENTERED
            }

            DragEvent.ACTION_DRAG_EXITED -> if (tile.tag != viewModel.startDragPos) {
                tile.tileState = mTileState
            }

            DragEvent.ACTION_DROP -> {
                if (dragEvent.localState != null) {

                    if ((dragEvent.localState as TileState) == TileState.IN_TRAY) {
                        viewModel.removeTileFromTray(viewModel.tileInfoFromClipData(dragEvent.clipData).trayIndex)
                    } else {
                        viewModel.updateTile(viewModel.getLetterFromGrid(tile.tag as TilePos),  viewModel.startDragPos)
                    }
                }
                viewModel.addTileToGrid(viewModel.tileInfoFromClipData(dragEvent.clipData).letter, (tile.tag as TilePos))


            }
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners() {

        for (row in 0..6) {
            for (col in 0..6) {
                gridArray[row][col].connectViewModel(viewModel, TilePos(row, col))
                gridArray[row][col].setOnDragListener(mOnDragGridListener)
                gridArray[row][col].tag = TilePos(row, col)
            }
        }
    }
}



