package com.taochirho.wordbox.ui.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.taochirho.wordbox.R


class TrayWatermark: ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?,
                @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("", Typeface.BOLD)
        textSize = 244F
        color = context.getColorFromAttr(R.attr.colorOnSurface)
    }

    private var wmX = 80F
    private var wmY = 220F
    private var lineHeight = 100F

    private val margin = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, context.resources.getDimension(R.dimen.watermarkMargin), context.resources.displayMetrics)

    private val offsetX = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, context.resources.getDimension(R.dimen.watermarkOffsetX), context.resources.displayMetrics)

    private val offsetY = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, context.resources.getDimension(R.dimen.watermarkOffsetY), context.resources.displayMetrics)



    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        val wb = context.resources.getString(R.string.wb)

        val widthLessMargins = width - (2F * margin)
        val heightLessMargins = height - (2F * margin)

        textPaint.textSize = 1.5F * (widthLessMargins / wb.length) // empirically seems a reasonable starting point


        var bounds = Rect()
        var finalBounds = Rect()

        textPaint.getTextBounds(wb, 0, wb.length, bounds)

        if (bounds.width() > widthLessMargins) {
            while (bounds.width() > widthLessMargins){
                finalBounds = bounds
                textPaint.textSize -= 5F
                textPaint.getTextBounds(wb, 0, wb.length, bounds)
            }
          } else {
            while (bounds.width() < widthLessMargins){
                finalBounds = bounds
                textPaint.textSize += 5F
                textPaint.getTextBounds(wb, 0, wb.length, bounds)
            }
        }

        // some screens the height of the text at this width is higher than the tray
        bounds = finalBounds

        while ((bounds.height() * 3F) > heightLessMargins){ // 2 lines plus line spacing
            finalBounds = bounds
            textPaint.textSize -= 5F
            textPaint.getTextBounds(wb, 0, wb.length, bounds)
        }


        lineHeight = 1.5F * finalBounds.height()
        wmY = finalBounds.height() + margin
        wmX = margin + (widthLessMargins - finalBounds.width()) / 2F // centers text
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas != null) {
            with(canvas) {
                textPaint.color = context.getColorFromAttr(R.attr.colorPrimary)
                drawText(context.resources.getString(R.string.wb), wmX + offsetX, wmY + offsetY, textPaint)
                textPaint.color = context.getColorFromAttr(R.attr.colorPrimaryDark)
                drawText(context.resources.getString(R.string.wb), wmX, wmY, textPaint)
                textPaint.color = context.getColorFromAttr(R.attr.colorPrimary)
                drawText(context.resources.getString(R.string.sol), wmX + offsetX, wmY + offsetY + lineHeight, textPaint)
                textPaint.color = context.getColorFromAttr(R.attr.colorPrimaryDark)
                drawText(context.resources.getString(R.string.sol), wmX, wmY + lineHeight, textPaint)
            }
            super.onDraw(canvas)
        }
    }
}
