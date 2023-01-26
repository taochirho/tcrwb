package com.taochirho.wordbox.ui.main

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.database.*
import com.taochirho.wordbox.databinding.GamesListRowBinding
import java.text.SimpleDateFormat
import java.util.*


class GamesListAdapter(
    private val deleteListener: GameDeleteListener,
    private val sendListener: GameSendListener,
    private val restoreListener: GameRestoreListener,
    private val toggleListener: GameStarToggleListener
) : ListAdapter<Game, GamesListAdapter.ViewHolder>(GamesListDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, toggleListener, deleteListener, sendListener, restoreListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: GamesListRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = GamesListRowBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }

       fun bind(
           item: Game,
           toggleListener: GameStarToggleListener,
           deleteListener: GameDeleteListener,
           sendListener: GameSendListener,
           restoreListener: GameRestoreListener
        ) {

           binding.game = item


           binding.theGrid.visibility = View.GONE
           binding.theTray.visibility = View.GONE
           binding.divider1.visibility = View.GONE

           // clearGrid()

            val counts = countTiles(item.gameTiles) // counts is a pair.  First is count on grid, second count on tray

            if (counts.first > 0 && counts.second > 0) { // i.e tiles in both grid and tray
                binding.theGrid.visibility = View.VISIBLE
                binding.theTray.visibility = View.VISIBLE
                binding.divider1.visibility = View.VISIBLE

                binding.theGrid.setGridGameArray(item, item.status != GAME_STATUS.RA)
                binding.theTray.setGridGameArray(item, counts.second)
            } else {
                if (counts.first > 0) { // i.e. all tiles in grid
                    binding.theGrid.setGridGameArray(item, item.status != GAME_STATUS.RA)
                    binding.theGrid.visibility = View.VISIBLE

                } else {  // i.e. all tiles in tray
                    binding.theTray.setGridGameArray(item, counts.second)
                    binding.theTray.visibility = View.VISIBLE

                }
            }

/*

            if (itemView.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                binding.textSize = itemView.resources.displayMetrics.widthPixels / 12.0f
            } else {
                binding.textSize = itemView.resources.displayMetrics.heightPixels / 28.0f
            }
*/

            if (itemView.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {

                binding.gamesListRow.layoutParams.width = (itemView.resources.configuration.screenWidthDp / 1.8).toInt()
                val pattern = "yy-MM-dd HH:mm"
                val simpleDateFormat = SimpleDateFormat(pattern, Locale.UK)
                binding.formattedSavedDate = simpleDateFormat.format(item.dateSaved)
            } else {
                itemView.resources.displayMetrics.widthPixels
                binding.gamesListRow.layoutParams.width = itemView.resources.displayMetrics.widthPixels
                val pattern =
                    "EEEE dd MMMM yyyy 'at' HH:mm" // EEEE has to be exactly 4 characters to be e.g. Saturday
                val simpleDateFormat = SimpleDateFormat(pattern, Locale.UK)
                binding.formattedSavedDate = simpleDateFormat.format(item.dateSaved)

            }

            binding.toggleLetterStarClickListener = toggleListener
            binding.deleteClickListener = deleteListener
            binding.sendClickListener = sendListener
            binding.restoreClickListener = restoreListener
            binding.executePendingBindings()
        }


        private fun countTiles(tiles: Array<Tile>) : Pair<Int, Int> {
            var gridCount = 0
            var trayCount = 0

            for (tile in tiles) {
                if (tile.state == TileState.IN_TRAY) {
                    trayCount += 1
                } else {
                    gridCount += 1
                }
            }
            return Pair(gridCount, trayCount)

        }

     /*   private fun clearGrid() {
            var i = 0
            var j = 0

            for (i in 0..6) {
                for (j in 0..6) {
                    grid[(i * 7) + j] = Tile("", TileState.EMPTY, TilePos(i, j, -1))
                }
            }
        }*/

      /*  private fun setTile(tile: Tile, hideLetter: Boolean) {
            if (tile.state == TileState.IN_TRAY) {
                addToTray(tile)
            } else {
                //addToGrid(tile, hideLetter)
            }
        }*/

       /* private fun addToGrid(tile: Tile, hideLetter: Boolean) {

            if (hideLetter) {
                grid[(tile.tilePos.row * 7) + tile.tilePos.col] =
                    Tile("*", TileState.RIGHT, tile.tilePos)
            } else {
                grid[(tile.tilePos.row * 7) + tile.tilePos.col] = tile
            }
        }*/

      /*  private fun addToTray(tile: Tile) {
           // tray[trayCount] = tile
            trayCount += 1
        }*/
    }


    class GamesListDiffCallback : DiffUtil.ItemCallback<Game>() {

        override fun areItemsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: Game, newItem: Game): Boolean {
            return oldItem == newItem
        }
    }

    class GameStarToggleListener(val clickListener: (game: Game) -> Unit) {
        fun onClick(game: Game) = clickListener(game)
    }

    class GameRestoreListener(val clickListener: (uid: Int) -> Unit) {
        fun onClick(uid: Int) = clickListener(uid)
    }

    class GameSendListener(val clickListener: (game: Game) -> Unit) {
        fun onClick(game: Game) = clickListener(game)
    }

    class GameDeleteListener(val clickListener: (game: Game) -> Unit) {
        fun onClick(game: Game) = clickListener(game)
    }

}


