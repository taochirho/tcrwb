package com.taochirho.wordbox.ui.main

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
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

    private var w = 0F
    private var h = 0F
    private var internalCellWidth = 0F
    private var internalCellHeight = 0F

    private var mGameArray: Array<Array<Tile>>

    private var mShowLetters = true

    private var leftBorder = 0F
    private var topBorder = 0F
    private var rightBorder = 0F
    private var bottomBorder = 0F
    private var tileBorder = 0F
    private var gridTileCornerRadius = 0F

    private var verticalInside = 0F
    private var horizontalInside = 0F

    private var gridBackgroundColor = Color.BLUE
    private var textColor = Color.WHITE
    private var textShadowColor = Color.DKGRAY

    private var tileEmptyColor = Color.DKGRAY
    private var tileBorderColor = Color.BLACK
    private var tileRightColor = Color.GREEN
    private var tileNearlyColor = Color.CYAN
    private var tileWrongColor = Color.RED

    private val tilePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("", Typeface.BOLD)
    }
    private var letterHeightNormal: Int = 0
    private var letterWidthNormal: Int = 0
    private var letterWidthL: Int = 0
    private var letterWidthM: Int = 0
    private var letterWidthI: Int = 0
    private var starHeight: Int = 0
    private var starWidth: Int = 0


    init {

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
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.GameGridView,
            0, 0
        ).apply {

            try {
                leftBorder = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_left_border, 4f),
                    context.resources.displayMetrics
                )
                topBorder = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_top_border, 4f),
                    context.resources.displayMetrics
                )
                rightBorder = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_right_border, 4f),
                    context.resources.displayMetrics
                )
                bottomBorder = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_bottom_border, 4f),
                    context.resources.displayMetrics
                )
                tileBorder = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_grid_tile_border, 4f),
                    context.resources.displayMetrics
                )
                verticalInside = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_vertical_inside, 4f),
                    context.resources.displayMetrics
                )
                horizontalInside = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_horizontal_inside, 4f),
                    context.resources.displayMetrics
                )
                gridTileCornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameGridView_grid_tile_corner_radius, 4f),
                    context.resources.displayMetrics
                )

                textColor = context.getColorFromAttr(R.attr.colorOnSurface)
                textShadowColor = context.getColorFromAttr(R.attr.colorSurface)
                tileBorderColor = context.getColorFromAttr(R.attr.colorOnSurface)
                tileEmptyColor = context.getColorFromAttr(R.attr.colorSurface)

                gridBackgroundColor = context.getColorFromAttr(R.attr.colorPrimaryDark)
                tileRightColor = context.getColorFromAttr(R.attr.tile_right)
                tileNearlyColor = context.getColorFromAttr(R.attr.tile_nearly_right)
                tileWrongColor = context.getColorFromAttr(R.attr.tile_wrong)

            } finally {
                recycle()
            }
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        w = width.toFloat()
        h = height.toFloat()

        internalCellWidth = (w - (leftBorder + rightBorder) - (6 * verticalInside)) / 7
        internalCellHeight = (h - (topBorder + bottomBorder) - (6 * horizontalInside)) / 7

        textPaint.textSize =
            internalCellHeight.coerceAtMost(internalCellWidth) - (tileBorder * 2)

        val bounds = Rect()
        textPaint.getTextBounds("X", 0, 1, bounds)
        letterHeightNormal = bounds.height()
        letterWidthNormal = bounds.width()
        textPaint.getTextBounds("L", 0, 1, bounds)
        letterWidthL = bounds.width()
        textPaint.getTextBounds("M", 0, 1, bounds)
        letterWidthM = bounds.width()
        textPaint.getTextBounds("I", 0, 1, bounds)
        letterWidthI = bounds.width()
        textPaint.getTextBounds("*", 0, 1, bounds)
        starWidth = bounds.width()
        starHeight = bounds.height()
    }

    override fun onDraw(canvas: Canvas?) {

        if (canvas != null) {
            with(canvas) {
                tilePaint.color = gridBackgroundColor

                drawRect(0F, 0F, w, h, tilePaint)

                for (i in 0..6) {
                    for (j in 0..6) {

                        val left = leftBorder + (i * (internalCellWidth + verticalInside))
                        val top = topBorder + (j * (internalCellHeight + horizontalInside))
                        val right = left + internalCellWidth
                        val bottom = top + internalCellHeight

                        when (mGameArray[i][j].state) {
                            TileState.RIGHT -> {
                                tilePaint.color = tileRightColor
                                borderPaint.color = tileBorderColor

                            }
                            TileState.NEARLY_RIGHT -> {
                                tilePaint.color = tileNearlyColor
                                borderPaint.color = tileBorderColor
                            }
                            TileState.WRONG -> {
                                tilePaint.color = tileWrongColor
                                borderPaint.color = tileBorderColor
                            }
                            else -> {
                                tilePaint.color = tileEmptyColor
                                borderPaint.color = tileEmptyColor
                            }
                        }
                        drawRoundRect(
                            left,
                            top,
                            right,
                            bottom,
                            gridTileCornerRadius,
                            gridTileCornerRadius,
                            borderPaint
                        ) // effectively draws tile border

                        var offset = 0F // to adjust position of star

                        val letter =
                            if (mShowLetters) {
                                mGameArray[i][j].letter
                            } else {
                                if (mGameArray[i][j].letter.isNullOrEmpty()) {
                                    ""
                                } else {
                                    offset = internalCellHeight * 0.3F
                                    "*"
                                }
                            }

                        drawRoundRect(
                            left + tileBorder,
                            top + tileBorder,
                            right - tileBorder,
                            bottom - tileBorder,
                            gridTileCornerRadius,
                            gridTileCornerRadius,
                            tilePaint
                        )

                        if (letter != null) { // M and I don't centre with "normal" letter width
                            val letterWidth = when (letter) {
                                "*" -> starWidth
                                "L" -> letterWidthL
                                "M" -> letterWidthM
                                "I" -> letterWidthI
                                else -> letterWidthNormal
                            }

                            val letterHeight = if (letter == "*") {
                                starHeight
                            } else {
                                letterHeightNormal
                            }

                            textPaint.color = textShadowColor
                            drawText(
                                letter,
                                4 + left - tileBorder + ((internalCellWidth - letterWidth) * 0.5F),
                                4 + top + internalCellHeight - ((internalCellHeight - letterHeight) * 0.5F) + offset, // otherwise star at top of box
                                textPaint
                            )

                            textPaint.color = textColor
                            drawText(
                                letter,
                                left - tileBorder + ((internalCellWidth - letterWidth) * 0.5F),
                                top + internalCellHeight - ((internalCellHeight - letterHeight) * 0.5F) + offset, // otherwise star at top of box
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
    private var h = 0F
    private var w = 0F
    private var trayCellWidth = 0F

    private var trayBackgroundColor = Color.RED
    private var trayTileBackgroundColor = Color.BLUE
    private var trayTileBorderColor = Color.MAGENTA
    private var textColor = Color.WHITE
    private var textShadowColor = Color.DKGRAY

    private var trayTileSpacing = 0F
    private var trayTileBorder = 0F
    private var trayTileCornerRadius = 0F

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        typeface = Typeface.create("", Typeface.BOLD)
    }
    private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
        typeface = Typeface.create("", Typeface.BOLD)
    }
    private var letterHeightNormal: Int = 0
    private var letterWidthNormal: Int = 0
    private var letterWidthL: Int = 0
    private var letterWidthM: Int = 0
    private var letterWidthI: Int = 0
    private var starHeight: Int = 0
    private var starWidth: Int = 0


    init {
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

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.GameTrayView,
            0, 0
        ).apply {
            try {

                trayBackgroundColor = context.getColorFromAttr(R.attr.colorPrimaryDark)
                trayTileBackgroundColor = context.getColorFromAttr(R.attr.colorSurface)
                trayTileBorderColor = context.getColorFromAttr(R.attr.colorOnSurface)
                textColor = context.getColorFromAttr(R.attr.colorOnSurface)
                textShadowColor =  context.getColorFromAttr(R.attr.tile_wrong)

                trayTileSpacing = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameTrayView_tray_tile_spacing, 4f),
                    context.resources.displayMetrics
                )
                trayTileBorder = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameTrayView_tray_tile_border, 4f),
                    context.resources.displayMetrics
                )
                trayTileCornerRadius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    getDimension(R.styleable.GameTrayView_tray_tile_corner_radius, 4f),
                    context.resources.displayMetrics
                )
            } finally {
                recycle()
            }
        }
    }

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val cellWidth = (widthSize - (8 * trayTileSpacing)) / 7

        var rows = mGameTray.count() / 7
        val modRows = mGameTray.count() % 7

        if (modRows > 0) rows += 1

        val height = (((cellWidth + trayTileSpacing) * rows) + trayTileSpacing).toInt()

        setMeasuredDimension(widthSize, height)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        h = height.toFloat()
        w = width.toFloat()
        trayCellWidth = (w - (8 * trayTileSpacing)) / 7


        textPaint.textSize = trayCellWidth - (trayTileBorder * 2.0F)

        val bounds = Rect()
        textPaint.getTextBounds("X", 0, 1, bounds)
        letterHeightNormal = bounds.height()
        letterWidthNormal = bounds.width()
        textPaint.getTextBounds("L", 0, 1, bounds)
        letterWidthL = bounds.width()
        textPaint.getTextBounds("M", 0, 1, bounds)
        letterWidthM = bounds.width()
        textPaint.getTextBounds("I", 0, 1, bounds)
        letterWidthI = bounds.width()
        textPaint.getTextBounds("*", 0, 1, bounds)
        starWidth = bounds.width()
        starHeight = bounds.height()
    }

    override fun onDraw(canvas: Canvas?) {

        val trayCellWidth = (w - (8 * trayTileSpacing)) / 7

        if (canvas != null) {
            with(canvas) {

                paint.color = trayBackgroundColor

                drawRect(0F, 0F, w, h, paint)

                var i = 0
                var j = 0

                for (tile in mGameTray) {

                    if (i > 6) {
                        j++
                        i = 0 // start next row in tray after 7 tiles
                    }

                    val left = trayTileSpacing + (i * (trayCellWidth + trayTileSpacing))
                    val top = trayTileSpacing + (j * (trayCellWidth + trayTileSpacing))
                    val right = left + trayCellWidth
                    val bottom = top + trayCellWidth

                    paint.color = trayTileBorderColor
                    drawRoundRect(
                        left,
                        top,
                        right,
                        bottom,
                        trayTileCornerRadius,
                        trayTileCornerRadius,
                        paint
                    ) // effectively draws tile border

                    paint.color = trayTileBackgroundColor

                    drawRoundRect(  // draw tile background
                        left + trayTileBorder,
                        top + trayTileBorder,
                        right - trayTileBorder,
                        bottom - trayTileBorder,
                        trayTileCornerRadius,
                        trayTileCornerRadius,
                        paint
                    )

                    if (tile.letter != null) {
                         // M and I don't centre with "normal" letter width
                            val letterWidth = when (tile.letter) {
                                "*" -> starWidth
                                "L" -> letterWidthL
                                "M" -> letterWidthM
                                "I" -> letterWidthI
                                else -> letterWidthNormal
                            }

                            val letterHeight = if (tile.letter == "*") {
                                starHeight
                            } else {
                                letterHeightNormal
                            }

                        textPaint.color = textShadowColor
                        drawText(
                            tile.letter!!,
                            left + 4 + ((trayCellWidth - letterWidth) * 0.5F),
                            top + 4 + trayCellWidth - ((trayCellWidth - letterHeight) * 0.5F),
                            textPaint
                        )

                        textPaint.color = textColor
                        drawText(
                            tile.letter!!,
                            left + ((trayCellWidth - letterWidth) * 0.5F),
                            top + trayCellWidth - ((trayCellWidth - letterHeight) * 0.5F),
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
