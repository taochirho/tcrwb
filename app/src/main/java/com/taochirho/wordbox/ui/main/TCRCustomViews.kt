package com.taochirho.wordbox.ui.main

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.GAME_STATUS
import com.taochirho.wordbox.database.Game
import com.taochirho.wordbox.database.Tile
import com.taochirho.wordbox.database.TilePos
import com.taochirho.wordbox.database.TileState
import java.util.*

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

class GameGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var h = 0F
    private var w = 0F

    private var mGameArray: Array<Array<Tile>>

    private var mShowLetters = true

    private var leftBorder = 0F
    private var topBorder = 0F
    private var rightBorder = 0F
    private var bottomBorder = 0F
    private var tileBorder = 0F

    var verticalInside = 0F
    var horizontalInside = 0F

    var gridBackgroundColor = Color.BLUE
    var letterColor = Color.YELLOW
    var tileBorderColor = Color.BLACK
    var tileRightColor = Color.GREEN
    var tileNearlyColor = Color.CYAN
    var tileWrongColor = Color.RED

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)

    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 120f
        typeface = Typeface.create("", Typeface.BOLD)

    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.GameGridView,
            0, 0
        ).apply {

            try {
                /* leftBorder = TypedValue.applyDimension(
                         TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameGridView_left_border, 4f),
                 context.getResources().getDisplayMetrics()
                 )
                 topBorder = TypedValue.applyDimension(
                     TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameGridView_top_border, 4f),
                     context.getResources().getDisplayMetrics()
                 )
                 rightBorder = TypedValue.applyDimension(
                     TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameGridView_right_border, 4f),
                     context.getResources().getDisplayMetrics()
                 )
                 bottomBorder = TypedValue.applyDimension(
                     TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameGridView_bottom_border, 4f),
                     context.getResources().getDisplayMetrics()
                 )
                 tileBorder = TypedValue.applyDimension(
                     TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameGridView_grid_tile_border, 4f),
                     context.getResources().getDisplayMetrics()
                 )
                 verticalInside = TypedValue.applyDimension(
                     TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameGridView_vertical_inside, 4f),
                     context.getResources().getDisplayMetrics()
                 )
                 horizontalInside = TypedValue.applyDimension(
                     TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameGridView_horizontal_inside, 4f),
                     context.getResources().getDisplayMetrics()
                 )

                gridBackgroundColor = getColor(R.styleable.GameGridView_grid_background, Color.DKGRAY)
                letterColor = getColor(R.styleable.GameGridView_grid_letter_color, Color.YELLOW)
                tileBorderColor = getColor(R.styleable.GameGridView_grid_tile_border_color, Color.BLACK)
                tileRightColor = context.getColorFromAttr(R.attr.tile_right)
                tileNearlyColor = context.getColorFromAttr(R.attr.tile_nearly_right)
                tileWrongColor = context.getColorFromAttr(R.attr.tile_wrong)

*/
            } finally {
                recycle()
            }
        }

        mGameArray = gameToArray(
            Game(
                0,
                15,
                0,
                900000L, // default 15 minutes (and 1 sec)
                Date(),
                "Word Box",
                "Welcome",
                GAME_STATUS.C,
                arrayOf(
                    Tile("W", TileState.RIGHT, TilePos(0, 0)),
                    Tile("E", TileState.RIGHT, TilePos(0, 1)),
                    Tile("L", TileState.RIGHT, TilePos(0, 2)),
                    Tile("C", TileState.RIGHT, TilePos(0, 3)),
                    Tile("O", TileState.RIGHT, TilePos(0, 4)),
                    Tile("M", TileState.RIGHT, TilePos(0, 5)),
                    Tile("E", TileState.RIGHT, TilePos(0, 6)),
                    Tile("T", TileState.NEARLY_RIGHT, TilePos(2, 1)),
                    Tile("W", TileState.WRONG, TilePos(3, 0)),
                    Tile("O", TileState.WRONG, TilePos(3, 1)),
                    Tile("R", TileState.WRONG, TilePos(3, 2)),
                    Tile("D", TileState.WRONG, TilePos(3, 3)),
                    Tile("B", TileState.WRONG, TilePos(3, 4)),
                    Tile("A", TileState.WRONG, TilePos(4, 5)),
                    Tile("X", TileState.WRONG, TilePos(5, 6)),
                )
            )
        )
    }



    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        h = height.toFloat()
        w = width.toFloat()
    }

    override fun onDraw(canvas: Canvas?) {

        val internalCellWidth = (w - (leftBorder + rightBorder) - (6 * verticalInside)) / 7
        val internalCellHeight = (h - (topBorder + bottomBorder) - (6 * horizontalInside)) / 7

        if (canvas != null) {
            with(canvas) {
                paint.color = gridBackgroundColor
                textPaint.color = letterColor

                drawRect(0F, 0F, w, h, paint)

                for (i in 0..6) {
                    for (j in 0..6) {
                        paint.color = tileBorderColor

                        drawRect(  // effectively draws tile border
                            leftBorder + (i * (internalCellWidth + verticalInside)),
                            topBorder + (j * (internalCellHeight + horizontalInside)),
                            leftBorder + internalCellWidth + (i * (internalCellWidth + verticalInside)),
                            topBorder + internalCellHeight + (j * (internalCellHeight + horizontalInside)),
                            paint
                        )

                        when (mGameArray[i][j].state) {
                            TileState.RIGHT -> paint.color = tileRightColor
                            TileState.NEARLY_RIGHT -> paint.color = tileNearlyColor
                            TileState.WRONG -> paint.color = tileWrongColor
                            else -> paint.color = gridBackgroundColor
                        }

                        val letter =
                            if (mShowLetters) {
                                mGameArray[i][j].letter
                            } else {
                                if ( mGameArray[i][j].letter.isNullOrEmpty()){
                                     ""
                                } else {
                                    "*"
                                }
                            }

                        drawRect(
                            leftBorder + tileBorder + (i * (internalCellWidth + verticalInside)),
                            topBorder + tileBorder + (j * (internalCellHeight + horizontalInside)),
                            leftBorder - tileBorder + internalCellWidth + (i * (internalCellWidth + verticalInside)),
                            topBorder - tileBorder + internalCellHeight + (j * (internalCellHeight + horizontalInside)),
                            paint
                        )

                        if (letter != null) {
                            drawText(
                                letter,
                                leftBorder + (internalCellWidth / 2) + i * (internalCellWidth + verticalInside),
                                topBorder + (internalCellHeight * 0.75F) + j * (internalCellHeight + horizontalInside),
                                textPaint
                            )
                        }
                    }
                }
            }
        }
        super.onDraw(canvas)
    }

    fun setGridGameArray(game: Game, showLetters: Boolean) {
        mGameArray = gameToArray(game)
        mShowLetters = showLetters
        invalidate()
        requestLayout()
    }

    private fun gameToArray(game: Game): Array<Array<Tile>> {
        val rows = 7
        val cols = 7

        val array = Array(rows) { Array(cols) { Tile(null, TileState.EMPTY) } }

        for (tile in game.gameTiles) {

            if (tile.state != TileState.IN_TRAY) {
                array[tile.tilePos.col][tile.tilePos.row] = tile
            }
        }
        return array
    }
}

class GameTrayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private var mGameTray: Array<Tile>

    var trayBackgroundColor = Color.GRAY
    var trayTileBackgroundColor = Color.DKGRAY
    var trayTileBorderColor = Color.BLACK
    var trayTextColour = Color.BLUE

    var trayTilespacing = 0F
    var trayTileBorder = 0F

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.GameTrayView,
            0, 0
        ).apply {

            try {
/*
                trayTileBorderColor =
                    getColor(R.styleable.GameTrayView_tray_tile_border_color, Color.BLACK)
                trayTextColour =
                    getColor(R.styleable.GameTrayView_tray_letter_color, Color.BLUE)

                trayTilespacing = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameTrayView_tray_tile_spacing, 4f),
                    context.getResources().getDisplayMetrics()
                )
                trayTileBorder = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, getDimension(R.styleable.GameTrayView_tray_tile_border, 4f),
                    context.getResources().getDisplayMetrics()
                )

                trayTileBackgroundColor =  context.getColorFromAttr(R.attr.tile_tray)*/

            } finally {
                recycle()
            }
        }

        mGameTray = gameToTray(
            Game(
                0,
                7,
                0,
                900000L, // default 15 minutes (and 1 sec)
                Date(),
                "Word Box",
                "Welcome",
                GAME_STATUS.C,
                arrayOf(
                    Tile("W", TileState.IN_TRAY, TilePos(0, 0)),
                    Tile("E", TileState.IN_TRAY, TilePos(0, 0)),
                    Tile("L", TileState.IN_TRAY, TilePos(0, 0)),
                    Tile("C", TileState.IN_TRAY, TilePos(0, 0)),
                    Tile("O", TileState.IN_TRAY, TilePos(0, 0)),
                    Tile("M", TileState.IN_TRAY, TilePos(0, 0)),
                    Tile("E", TileState.IN_TRAY, TilePos(0, 0)),
                )
            ), 7
        )
    }

    /*  fun getGridGame() : Game {
          return mGame
      }
      */



    fun setGridGameArray(game: Game, tileCount: Int) {
        mGameTray = gameToTray(game, tileCount)
        invalidate()
        requestLayout()
    }

    private fun gameToTray(game: Game, onTray: Int): Array<Tile> {

        val array = Array(onTray) { Tile("X", TileState.IN_TRAY, TilePos(0, 0)) }
        var i = 0

        for (tile in game.gameTiles) {
            if (tile.state == TileState.IN_TRAY) {
                array[i].letter = tile.letter
                i++
            }
        }
        return array
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 55.0f
        typeface = Typeface.create("", Typeface.BOLD)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        textSize = 120f
        typeface = Typeface.create("", Typeface.BOLD)
        color = trayTextColour
    }
    private var h = 0F
    private var w = 0F

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        h = height.toFloat()
        w = width.toFloat()
    }

    override fun onDraw(canvas: Canvas?) {

        val trayCellWidth = (w - (8 * trayTilespacing)) / 7

        if (canvas != null) {
            with(canvas) {

                paint.color = trayBackgroundColor
                drawRect(0F, 0F, w, h, paint)

                var i = 0
                var j = 0

                for (tile in mGameTray) {

                    val letter = mGameTray[i].letter

                    if (i > 6) {
                        j++
                        i = 0 // start next row in tray after 7 tiles
                    }

                    paint.color = trayTileBorderColor

                    drawRect(  // effectively draws tile border
                        trayTilespacing + (i * (trayCellWidth + trayTilespacing)),
                        trayTilespacing + (j * (trayCellWidth + trayTilespacing)),
                        trayTilespacing + trayCellWidth + (i * (trayCellWidth + trayTilespacing)),
                        trayTilespacing + trayCellWidth + (j * (trayCellWidth + trayTilespacing)),
                        paint
                    )

                    paint.color = trayTileBackgroundColor
                    drawRect(  // draw tile background
                        trayTilespacing + (i * (trayCellWidth + trayTilespacing)) + trayTileBorder,
                        trayTilespacing + (j * (trayCellWidth + trayTilespacing)) + trayTileBorder,
                        trayTilespacing + trayCellWidth + (i * (trayCellWidth + trayTilespacing)) - trayTileBorder,
                        trayTilespacing + trayCellWidth + (j * (trayCellWidth + trayTilespacing)) - trayTileBorder,
                        paint
                    )


                    if (letter != null) {
                        drawText(
                            letter,
                            trayTilespacing + (i * (trayCellWidth + trayTilespacing)) + trayTileBorder + (trayCellWidth * 0.5F),
                            trayTilespacing + (j * (trayCellWidth + trayTilespacing)) + trayTileBorder + (trayCellWidth * 0.75F),
                            textPaint
                        )
                    }

                    i++
                }
            }
        }

        super.onDraw(canvas)
    }
}
