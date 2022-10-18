package com.taochirho.testvm.ui.main

import android.content.res.Configuration
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taochirho.testvm.application.GAME_STATUS
import com.taochirho.testvm.database.Game
import com.taochirho.testvm.database.GameStatus
import com.taochirho.testvm.database.Tile
import com.taochirho.testvm.database.TileState
import com.taochirho.testvm.databinding.GamesListRowBinding
import java.text.SimpleDateFormat
import java.util.*


class GamesListAdapter(
    private val deleteListener: GameDeleteListener,
    private val sendListener: GameSendListener,
    private val restoreListener: GameRestoreListener,
    private val toggleListener: GameAsterixToggleListener
) : ListAdapter<Game, GamesListAdapter.ViewHolder>(GamesListDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item,toggleListener, deleteListener, sendListener, restoreListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: GamesListRowBinding) : RecyclerView.ViewHolder(binding.root){

        private var gridCount = 0
        private var trayCount = 0
        private var showLetter = true

        private val grid = MutableList(49) { Tile("", TileState.EMPTY) }
        private val tray = MutableList(35) { Tile("?", TileState.EMPTY) }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = GamesListRowBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

        fun bind(item: Game,
                 toggleListener: GameAsterixToggleListener,
                 deleteListener: GameDeleteListener,
                 sendListener: GameSendListener,
                 restoreListener: GameRestoreListener) {
            binding.game = item

            //binding.letterVisible = item.status != GAME_STATUS.RA
            val hideLetter = item.status == GAME_STATUS.RA

            binding.theGrid.visibility = View.GONE
            binding.theTray.visibility = View.GONE

            binding.divider1?.visibility = View.GONE

            for (i in 0 until item.tileCount) {
                countTiles(item.gameTiles[i])
            }

            if (gridCount > 0 && trayCount > 0 ) { // i.e tiles in both grid and tray
                binding.theGrid.visibility = View.VISIBLE
                binding.theTray.visibility = View.VISIBLE
                binding.divider1?.visibility = View.VISIBLE

                trayCount = 0
                for (i in 0 until item.tileCount) {
                    setTile(item.gameTiles[i], hideLetter)
                }
            } else {
                if (gridCount > 0) { // i.e. all tiles in tray
                    binding.theGrid.visibility = View.VISIBLE
                    for (i in 0 until item.tileCount) {
                        addToGrid(item.gameTiles[i], hideLetter)
                        binding.divider1?.visibility = View.VISIBLE
                    }
                } else {
                    binding.theTray.visibility = View.VISIBLE

                    trayCount = 0
                    for (i in 0 until item.tileCount) {
                        addToTray(item.gameTiles[i])
                    }
                }
            }

            binding.theTray.visibility = View.VISIBLE

            trayCount = 0
//            for (i in 0 until item.tileCount) {
//                addToTray(Tile("A", TileState.EMPTY))
//            }

 /*           binding.textSize = 2f *
                if (itemView.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    itemView.resources.configuration.screenWidthDp / 7
                } else {
                    itemView.resources.configuration.screenWidthDp / 48
                }
*/

            if (itemView.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                binding.textSize = itemView.resources.displayMetrics.widthPixels / 12.0f
            } else {
                binding.textSize = itemView.resources.displayMetrics.heightPixels / 28.0f
            }

             val w : Double

            if (itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                w = itemView.resources.configuration.screenWidthDp / 1.8
                binding.gamesListRow.layoutParams.width = w.toInt()
                val pattern = "yy-MM-dd HH:mm"
                val simpleDateFormat = SimpleDateFormat(pattern, Locale.UK)
                binding.formattedSavedDate = simpleDateFormat.format(item.dateSaved)
            } else {
                itemView.resources.displayMetrics.widthPixels
                w = itemView.resources.displayMetrics.widthPixels.toDouble()
                val pattern = "EEEE dd MMMM yyyy 'at' HH:mm" // EEEE has to be exactly 4 characters to be e.g. Saturday
                val simpleDateFormat = SimpleDateFormat(pattern, Locale.UK)
                binding.formattedSavedDate = simpleDateFormat.format(item.dateSaved)

            }

            val wDiv = (w / 8).toInt()
            val wMod = ((w - (8 * wDiv)) / 2).toInt() //((w % 7) / 2).toInt()

            binding.G0.setGuidelineBegin(wMod)
            binding.G1.setGuidelineBegin(wMod + wDiv)
            binding.G2.setGuidelineBegin(wMod + (2 * wDiv))
            binding.G3.setGuidelineBegin(wMod + (3 * wDiv))
            binding.G4.setGuidelineBegin(wMod + (4 * wDiv))
            binding.G5.setGuidelineBegin(wMod + (5 * wDiv))
            binding.G6.setGuidelineBegin(wMod + (6 * wDiv))
            binding.G7.setGuidelineBegin(wMod + (7 * wDiv))

            binding.grid = grid
            binding.tray = tray

            binding.toggleLetterAsterixClickListener = toggleListener
            binding.deleteClickListener = deleteListener
            binding.sendClickListener = sendListener
            binding.restoreClickListener = restoreListener
            binding.executePendingBindings()
        }


        private fun countTiles(tile: Tile) {
            if (tile.state == TileState.IN_TRAY) {
                trayCount += 1
            } else {
                gridCount += 1
            }
        }

        private fun setTile(tile: Tile, hideLetter: Boolean) {
            if (tile.state == TileState.IN_TRAY) {
                addToTray(tile)
            } else {
                addToGrid(tile, hideLetter)
            }
        }

        private fun addToGrid(tile: Tile, hideLetter: Boolean) {
            if (hideLetter){
                grid[(tile.tilePos.row * 7) + tile.tilePos.col] = Tile("*", TileState.RIGHT, tile.tilePos)
            } else {
                grid[(tile.tilePos.row * 7) + tile.tilePos.col] = tile
            }
        }

        private fun addToTray(tile: Tile) {
            tray[trayCount]  = tile
            trayCount += 1
        }
    }


    class GamesListDiffCallback : DiffUtil.ItemCallback<Game>() {

        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem == newItem
        }
    }

    class GameAsterixToggleListener(val clickListener: (game: Game) -> Unit) {
        fun onClick(game: Game) = clickListener (game)
    }

    class GameRestoreListener(val clickListener: (uid: Int) -> Unit) {
        fun onClick(uid: Int) = clickListener (uid)
    }

    class GameSendListener(val clickListener: (uid: Int) -> Unit) {
        fun onClick(uid: Int) = clickListener (uid)
    }

    class GameDeleteListener(val clickListener: (game: Game) -> Unit) {
        fun onClick(game: Game) = clickListener(game)
    }

}


