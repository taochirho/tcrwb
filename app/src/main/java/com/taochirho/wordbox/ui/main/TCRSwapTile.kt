package com.taochirho.wordbox.ui.main


import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.taochirho.wordbox.model.GameModel


class TCRSwapTile : androidx.appcompat.widget.AppCompatTextView  {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)


  //  private var viewModel : GameModel? = null
  //  private lateinit var myParent : Fragment

    fun connectViewModel(viewModel: GameModel, parent: Fragment) {
   //     this.viewModel = viewModel
   //     myParent = parent
        viewModel.tilesSwapped.observe(parent.viewLifecycleOwner, tilesSwappedObserver)
        }

    val tilesSwappedObserver = Observer<Int> {
//        Log.w("tilesSwappedObserver", "$it")
        text = it.toString()

    }





}