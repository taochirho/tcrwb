package com.taochirho.wordbox.ui.main


import android.content.Context
import android.util.AttributeSet




class TCRSwapTile : androidx.appcompat.widget.AppCompatTextView  {
    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr)

/*

    fun connectViewModel(wordboxVM: WordBoxViewModel, parent: Fragment) {

        wordboxVM.tilesSwapped.observe(parent.viewLifecycleOwner, tilesSwappedObserver)
        }

    private val tilesSwappedObserver = Observer<Int> {
        text = it.toString()

    }

*/




}