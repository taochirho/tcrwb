package com.taochirho.testvm.ui.main

import android.view.View
import android.view.View.DragShadowBuilder

class TCRShadowBuilder // Defines the constructor for myDragShadowBuilder
(private val v: View) : DragShadowBuilder(v) {
   /* override fun onProvideShadowMetrics(size: Point, touch: Point) {
        size[view.width * 3] = view.height * 3
        touch[0] = 0
    }

    override fun onDrawShadow(canvas: Canvas) {

        canvas.scale(3f, 3f)
        v.draw(canvas)

    }*/

}