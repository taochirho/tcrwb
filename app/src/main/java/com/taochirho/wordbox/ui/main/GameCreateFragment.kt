package com.taochirho.wordbox.ui.main

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.taochirho.wordbox.R
import com.taochirho.wordbox.application.TOAST_MSGS
import com.taochirho.wordbox.databinding.GameCreateFragmentBinding
import com.taochirho.wordbox.model.WordBoxViewModel
import com.taochirho.wordbox.ux.views.theme.TCRCreatePuzzleTheme

class GameCreateFragment : Fragment() {


    private val wordboxVM: WordBoxViewModel by activityViewModels()
    private var _binding: GameCreateFragmentBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GameCreateFragmentBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                // In Compose world
                TCRCreatePuzzleTheme {
                    CreatePuzzleScreen()
                }
            }
        }
        return view
    }

    @Composable
    fun CreatePuzzleScreen()
    {
        val letterCount by wordboxVM.letterCount.observeAsState(0)

        wordboxVM.toastMsg.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                when (it) {
                    TOAST_MSGS.OUT_OF_RANGE -> Toast.makeText(
                        activity,
                        R.string.toast_enter_between,
                        Toast.LENGTH_SHORT
                    ).show()

                    TOAST_MSGS.NOT_A_NUMBER -> Toast.makeText(
                        activity,
                        R.string.toast_only_digits,
                        Toast.LENGTH_SHORT
                    ).show()

                    TOAST_MSGS.NO_TILES -> Toast.makeText(
                        activity,
                        R.string.toast_game_not_saved,
                        Toast.LENGTH_SHORT
                    ).show()

                    TOAST_MSGS.GAME_SAVED -> Toast.makeText(
                        activity,
                        R.string.toast_game_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.surface
        )
        {
            Scaffold(wordboxVM, letterCount)
        }
    }

    @Composable
    fun Scaffold(
        wordboxVM: WordBoxViewModel,
        letterCount: Int
    ) {
        val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))
        val density = resources.displayMetrics.density
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .padding(0.dp, 2.dp, 0.dp, 2.dp),
                                verticalArrangement = Arrangement.SpaceAround
                            ) {
                                Text(
                                    stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.h5,
                                    color = MaterialTheme.colors.onPrimary

                                )
                                Text(
                                    stringResource(R.string.app_subtitle_create),
                                    style = MaterialTheme.typography.h6,
                                    color = MaterialTheme.colors.onSecondary

                                )
                            }
                            Column(
                                Modifier
                                    .fillMaxHeight()
                                    .padding(0.dp, 0.dp, 16.dp, 0.dp),
                                verticalArrangement = Arrangement.Center
                            )
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Letters  ",
                                        style = MaterialTheme.typography.h6,
                                        color = MaterialTheme.colors.onSecondary
                                    )

                                    Text(
                                        text = "$letterCount",
                                        style = MaterialTheme.typography.h4.copy(
                                            color = MaterialTheme.colors.onPrimary,
                                            shadow = Shadow(
                                                color = MaterialTheme.colors.background,
                                                offset = Offset(0.5F / density, 1.0F / density),
                                                blurRadius = 0.5F / density
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                )
            },

            drawerContent = {
                if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Column(
                        modifier = Modifier.fillMaxSize(1f), verticalArrangement = Arrangement.Top
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .padding(2.dp)


                        ) {

                            val gridWidthDP = with(LocalDensity.current) {
                                if (constraints.maxHeight > constraints.maxWidth) {
                                    constraints.maxWidth
                                } else {
                                    constraints.maxHeight
                                }.toDp()
                            }
                            val tcrTextStyle = MaterialTheme.typography.body1.copy(
                                color = MaterialTheme.colors.onSurface,
                                shadow = Shadow(
                                    color =  MaterialTheme.colors.surface,
                                    offset = Offset(0.5F / density, 1.0F / density),
                                    blurRadius = 0.5F / density
                                ),
                                fontSize = with(LocalDensity.current) {
                                    if (constraints.maxHeight > constraints.maxWidth) {
                                        (constraints.maxWidth / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                            wordboxVM.uiState.cols
                                        } else {
                                            wordboxVM.uiState.rows
                                        }
                                    } else {
                                        (constraints.maxHeight / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                            wordboxVM.uiState.cols
                                        } else {
                                            wordboxVM.uiState.rows
                                        }
                                    }.toSp()
                                }
                            )

                            TCRSquareGrid(
                                modifier = Modifier
                                    .size(gridWidthDP, gridWidthDP),

                                cols = wordboxVM.uiState.cols,
                                rows = wordboxVM.uiState.rows
                            ) {
                                List(wordboxVM.uiState.rows * wordboxVM.uiState.cols)
                                {

                                    val input: Char by wordboxVM.getEnteredLetter(it)
                                        .observeAsState('\u0020')

                                    GridCell(
                                        input.toString(),
                                        it,
                                        onIndexedValueChange = { _, _: Int -> },
                                        tcrTextStyle
                                    )
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(1f), Arrangement.SpaceAround) {
                            Button(onClick = { wordboxVM.unShuffleLetters() }) {
                                Text(stringResource(R.string.unshuffle))
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(1f),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        BoxWithConstraints(
                            modifier = Modifier
                                .padding(2.dp)

                        ) {

                            val gridWidthDP = with(LocalDensity.current) {
                                if (constraints.maxHeight > constraints.maxWidth) {
                                    constraints.maxWidth
                                } else {
                                    constraints.maxHeight
                                }.toDp()
                            }
                            val tcrTextStyle = MaterialTheme.typography.body1.copy(
                                color = MaterialTheme.colors.onSurface,
                                shadow = Shadow(
                                    color = MaterialTheme.colors.surface,
                                    offset = Offset(0.5F / density, 1.0F / density),
                                    blurRadius = 0.5F / density
                                ),
                                fontSize = with(LocalDensity.current) {
                                    if (constraints.maxHeight > constraints.maxWidth) {
                                        (constraints.maxWidth / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                            wordboxVM.uiState.cols
                                        } else {
                                            wordboxVM.uiState.rows
                                        }
                                    } else {
                                        (constraints.maxHeight / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                            wordboxVM.uiState.cols
                                        } else {
                                            wordboxVM.uiState.rows
                                        }
                                    }.toSp()
                                }
                            )

                            TCRSquareGrid(
                                modifier = Modifier
                                    .size(gridWidthDP, gridWidthDP),

                                cols = wordboxVM.uiState.cols,
                                rows = wordboxVM.uiState.rows
                            ) {
                                List(wordboxVM.uiState.rows * wordboxVM.uiState.cols)
                                {

                                    val input: Char by wordboxVM.getEnteredLetter(it)
                                        .observeAsState('\u0020')

                                    GridCell(
                                        input.toString(),
                                        it,
                                        onIndexedValueChange = { TextFieldValue, index: Int -> },
                                        tcrTextStyle
                                    )
                                }
                            }
                        }
                        Column(
                            Modifier
                                .fillMaxHeight(1f)
                                .padding(64.dp, 0.dp, 0.dp, 0.dp),
                            Arrangement.Center
                        ) {
                            Button(onClick = { wordboxVM.unShuffleLetters() }) {
                                Text(stringResource(R.string.unshuffle))
                            }
                        }
                    }
                }
            },

            ) { padding ->

            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {

                Column(
                    modifier = Modifier.fillMaxSize(1f), verticalArrangement = Arrangement.Top
                ) {

                    BoxWithConstraints(
                        modifier = Modifier
                            .padding(padding)

                    ) {

                        val gridWidthDP = with(LocalDensity.current) {
                            if (constraints.maxHeight > constraints.maxWidth) {
                                constraints.maxWidth
                            } else {
                                constraints.maxHeight
                            }.toDp()
                        }

                        val tcrTextStyle = MaterialTheme.typography.body1.copy(
                            color = MaterialTheme.colors.onSurface,
                            shadow = Shadow(
                                color = MaterialTheme.colors.surface,
                                offset = Offset(0.5F / density, 1.0F / density),
                                blurRadius = 0.5F / density
                            ),
                            fontSize = with(LocalDensity.current) {
                                if (constraints.maxHeight > constraints.maxWidth) {
                                    (constraints.maxWidth / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                        wordboxVM.uiState.cols
                                    } else {
                                        wordboxVM.uiState.rows
                                    }
                                } else {
                                    (constraints.maxHeight / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                        wordboxVM.uiState.cols
                                    } else {
                                        wordboxVM.uiState.rows
                                    }
                                }.toSp()
                            }
                        )

                        TCRSquareGrid(
                            modifier = Modifier
                                .size(gridWidthDP, gridWidthDP),
                            cols = wordboxVM.uiState.cols,
                            rows = wordboxVM.uiState.rows
                        ) {
                            List(wordboxVM.uiState.rows * wordboxVM.uiState.cols)
                            {
                                val input: Char by wordboxVM.getLetter(it).observeAsState('\u0020')

                                GridCell(
                                    input.toString(),
                                    it,
                                    onIndexedValueChange = { input: TextFieldValue, index: Int ->
                                        wordboxVM.processInput(input, index)
                                    },
                                    tcrTextStyle
                                )
                            }
                        }
                    }

                    Column(
                        Modifier
                            .fillMaxHeight()
                            .padding(20.dp, 2.dp, 0.dp, 2.dp),
                        verticalArrangement = Arrangement.SpaceAround) {
                        Row(Modifier.fillMaxWidth(0.5f), Arrangement.Start) {
                            val gameDuration: Int by wordboxVM.gameDuration.observeAsState(0)
                            GameTimeEntry(gameDuration = gameDuration, onDurationChange = {input: String -> wordboxVM.onDurationChange(input) } )

                        }

                        Row(Modifier.fillMaxWidth(1f), Arrangement.SpaceAround) {
                            Button(onClick = {
                                if (wordboxVM.letterCount.value!! > 0) {
                                    wordboxVM.saveGame()
                                } else {
                                    wordboxVM.setToastMsg(TOAST_MSGS.NO_TILES)

                                }
                            })
                            {
                                Text(stringResource(R.string.save_game))
                            }
                            Button(onClick = { wordboxVM.shuffleLetters() }) {
                                Text(stringResource(R.string.shuffle))
                            }
                            Button(onClick = { wordboxVM.clearLetters() }) {
                                Text(stringResource(R.string.clear))
                            }

                        }
                    }

                    Row(Modifier.fillMaxWidth(1f), Arrangement.SpaceAround) {
                        Button(onClick = {

                            if (wordboxVM.letterCount.value!! > 0) {
                                Toast.makeText(
                                    activity,
                                    R.string.toast_game_saved,
                                    Toast.LENGTH_SHORT
                                ).show()
                                wordboxVM.saveGame()
                            } else {
                                Toast.makeText(
                                    activity,
                                    R.string.toast_game_not_saved,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                        {
                            Text(stringResource(R.string.save_game))
                        }
                        Button(onClick = { wordboxVM.shuffleLetters() }) {
                            Text(stringResource(R.string.shuffle))
                        }
                        Button(onClick = { wordboxVM.clearLetters() }) {
                            Text(stringResource(R.string.clear))
                        }

                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .padding(padding)

                    ) {

                        val gridWidthDP = with(LocalDensity.current) {
                            if (constraints.maxHeight > constraints.maxWidth) {
                                constraints.maxWidth
                            } else {
                                constraints.maxHeight
                            }.toDp()
                        }

                        val tcrTextStyle = MaterialTheme.typography.body1.copy(
                            color = MaterialTheme.colors.onSurface,
                            shadow = Shadow(
                                color = MaterialTheme.colors.surface,
                                offset = Offset(0.5F / density, 1.0F / density),
                                blurRadius = 0.5F / density
                            ),
                            fontSize = with(LocalDensity.current) {
                                if (constraints.maxHeight > constraints.maxWidth) {
                                    (constraints.maxWidth / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                        wordboxVM.uiState.cols
                                    } else {
                                        wordboxVM.uiState.rows
                                    }
                                } else {
                                    (constraints.maxHeight / 1.6f) / if (wordboxVM.uiState.cols > wordboxVM.uiState.rows) {
                                        wordboxVM.uiState.cols
                                    } else {
                                        wordboxVM.uiState.rows
                                    }
                                }.toSp()
                            }
                        )

                        TCRSquareGrid(
                            modifier = Modifier

                                .size(gridWidthDP, gridWidthDP),
                            cols = wordboxVM.uiState.cols,
                            rows = wordboxVM.uiState.rows
                        ) {
                            List(wordboxVM.uiState.rows * wordboxVM.uiState.cols)
                            {
                                val input: Char by wordboxVM.getLetter(it).observeAsState('\u0020')
                                GridCell(
                                    input.toString(),
                                    it,
                                    onIndexedValueChange = { input: TextFieldValue, index: Int ->
                                        wordboxVM.processInput(input, index)
                                    },
                                    tcrTextStyle
                                )
                            }
                        }
                    }

                    Column(
                        Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight(1f)
                            .padding(0.dp, 0.dp, 12.dp, 0.dp),
                        Arrangement.SpaceAround
                    )  {
                        val gameDuration: Int by wordboxVM.gameDuration.observeAsState(0)
                        GameTimeEntry(gameDuration = gameDuration, onDurationChange = {input: String -> wordboxVM.onDurationChange(input) } )
                    }


                    Column(
                        Modifier
                            .fillMaxHeight(1f)
                            .fillMaxWidth(0.6f)
                            .padding(0.dp, 0.dp, 36.dp, 0.dp),
                        Arrangement.SpaceAround
                    ) {
                        Button(onClick = { wordboxVM.saveGame() }) {
                            Text(stringResource(R.string.save_game))
                        }
                        Button(onClick = { wordboxVM.shuffleLetters() }) {
                            Text(stringResource(R.string.shuffle))
                        }
                        Button(onClick = { wordboxVM.clearLetters() }) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GameTimeEntry(gameDuration:Int, onDurationChange: (String) -> Unit ) {

        val numberTextStyle = MaterialTheme.typography.body1.copy(
            color = MaterialTheme.colors.onSurface,
            fontSize = 36.sp
        )

        OutlinedTextField(
            value = gameDuration.toString() ,
            onValueChange = onDurationChange,
            label = { Text(stringResource(R.string.target_time)) },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            textStyle = numberTextStyle,
        )
    }

    @Composable
    fun TCRSquareGrid(
        modifier: Modifier,
        cols: Int,
        rows: Int,
        content: @Composable () -> Unit,
    ) {
        Layout(
            modifier = modifier.aspectRatio(1f),
            content = content
        ) { measurables, constraints ->

            val gridWidth =
                if (constraints.maxHeight > constraints.maxWidth) {
                    (constraints.maxWidth * 0.88).toInt()
                } else {
                    (constraints.maxHeight * 0.88).toInt()
                }

            val cellWidth =
                if (constraints.maxHeight > constraints.maxWidth) {
                    gridWidth / cols
                } else {
                    gridWidth / rows
                }


            val cellConstraints = Constraints(cellWidth, cellWidth, cellWidth, cellWidth)

            val placeables = measurables.map { measurable ->
                measurable.measure(cellConstraints)
            }

            // Set the size of the layout as big as it can
            layout(gridWidth, gridWidth) {

                var i = 0
                placeables.forEach { placeable ->
                    placeable.place(
                        (i % cols) * placeable.width,
                        (i / cols) * placeable.height
                    )
                    i++
                }
            }
        }
    }

    @Composable
    fun GridCell(
        input: String,
        index: Int,
        onIndexedValueChange: (TextFieldValue, Int) -> Unit,
        tcrTextStyle: TextStyle
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(1f)
                .background(MaterialTheme.colors.primaryVariant)
                .aspectRatio(1f)
                .padding(2.dp),
        )
        {
            Surface(
                color = MaterialTheme.colors.surface,
                elevation = 4.dp
            ) {
                var selection by remember { mutableStateOf(TextRange.Zero) }
                var composition by remember { mutableStateOf<TextRange?>(null) }

                val textFieldValue = TextFieldValue(
                    input,
                    selection = selection.constrain(0, input.length),
                    composition = composition?.constrain(0, input.length)
                )

                BasicTextField(
                    modifier = Modifier
                        .padding(0.dp)
                        .align(
                            Alignment.Center
                        ),

                    value = textFieldValue,
                    onValueChange = {

                        selection = it.selection
                        composition = it.composition

                        if (((textFieldValue.text != it.text) && it.text.length < 3)) {
                            onIndexedValueChange(it, index)
                        }
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    textStyle = tcrTextStyle
                )
            }
        }
    }


private fun TextRange.constrain(i: Int, length: Int): TextRange {

    return TextRange(
        when {
            this.start < i -> i
            this.start > length -> length
            else -> this.start
        },
        when {
            this.end < i -> i
            this.end > length -> length
            else -> this.end
        }
    )
}

override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
