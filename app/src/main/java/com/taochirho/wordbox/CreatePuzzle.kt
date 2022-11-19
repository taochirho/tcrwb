package com.taochirho.wordbox


import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity


import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.taochirho.wordbox.model.GameCreateModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.taochirho.wordbox.application.TOAST_MSGS
import com.taochirho.wordbox.application.Wordbox
import com.taochirho.wordbox.model.GameCreateModelFactory
import com.taochirho.wordbox.ui.theme.TCRCreatePuzzleTheme



class CreatePuzzle : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TCRCreatePuzzleTheme {
                CreatePuzzleScreen()
            }
        }
    }

    @Composable
    fun CreatePuzzleScreen(

        vm: GameCreateModel = ViewModelProvider(
            viewModelStore,
            GameCreateModelFactory(application as Wordbox)
        ).get(GameCreateModel::class.java)


    ) {
        val letterCount by vm.letterCount.observeAsState(0)

        vm.toastMsg.observe(this, Observer {
            it.getContentIfNotHandled()?.let {
                when (it) {
                    TOAST_MSGS.OUT_OF_RANGE -> Toast.makeText(
                        this,
                        R.string.toast_enter_between,
                        Toast.LENGTH_SHORT
                    ).show()

                    TOAST_MSGS.NOT_A_NUMBER -> Toast.makeText(
                        this,
                        R.string.toast_only_digits,
                        Toast.LENGTH_SHORT
                    ).show()

                    TOAST_MSGS.NO_TILES -> Toast.makeText(
                        this,
                        R.string.toast_game_not_saved,
                        Toast.LENGTH_SHORT
                    ).show()

                    TOAST_MSGS.GAME_SAVED -> Toast.makeText(
                        this,
                        R.string.toast_game_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.surface
        )
        {
            Scaffold(vm, letterCount)
        }
    }

    @Composable
    fun Scaffold(
        vm: GameCreateModel,
        letterCount: Int
    ) {
        val scaffoldState = rememberScaffoldState(rememberDrawerState(DrawerValue.Closed))

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
                                    color = MaterialTheme.colors.onSurface

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
                                            color = MaterialTheme.colors.onSurface,
                                            shadow = Shadow(
                                                color = MaterialTheme.colors.background,
                                                offset = Offset(3.0f, 6.0f),
                                                blurRadius = 2f
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    },
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
                                    color = MaterialTheme.colors.surface,
                                    offset = Offset(3.0f, 6.0f),
                                    blurRadius = 2f
                                ),
                                fontSize = with(LocalDensity.current) {
                                    if (constraints.maxHeight > constraints.maxWidth) {
                                        (constraints.maxWidth / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                            vm.uiState.cols
                                        } else {
                                            vm.uiState.rows
                                        }
                                    } else {
                                        (constraints.maxHeight / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                            vm.uiState.cols
                                        } else {
                                            vm.uiState.rows
                                        }
                                    }.toSp()
                                }
                            )

                            TCRSquareGrid(
                                modifier = Modifier
                                    .size(gridWidthDP, gridWidthDP),

                                cols = vm.uiState.cols,
                                rows = vm.uiState.rows
                            ) {
                                List(vm.uiState.rows * vm.uiState.cols)
                                {

                                    val input: Char by vm.getEnteredLetter(it)
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
                        Row(Modifier.fillMaxWidth(1f), Arrangement.SpaceAround) {
                            Button(onClick = { vm.unShuffleLetters() }) {
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
                                    offset = Offset(3.0f, 6.0f),
                                    blurRadius = 2f
                                ),
                                fontSize = with(LocalDensity.current) {
                                    if (constraints.maxHeight > constraints.maxWidth) {
                                        (constraints.maxWidth / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                            vm.uiState.cols
                                        } else {
                                            vm.uiState.rows
                                        }
                                    } else {
                                        (constraints.maxHeight / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                            vm.uiState.cols
                                        } else {
                                            vm.uiState.rows
                                        }
                                    }.toSp()
                                }
                            )

                            TCRSquareGrid(
                                modifier = Modifier
                                    .size(gridWidthDP, gridWidthDP),

                                cols = vm.uiState.cols,
                                rows = vm.uiState.rows
                            ) {
                                List(vm.uiState.rows * vm.uiState.cols)
                                {

                                    val input: Char by vm.getEnteredLetter(it)
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
                            Button(onClick = { vm.unShuffleLetters() }) {
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
                                offset = Offset(3.0f, 6.0f),
                                blurRadius = 2f
                            ),
                            fontSize = with(LocalDensity.current) {
                                if (constraints.maxHeight > constraints.maxWidth) {
                                    (constraints.maxWidth / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                        vm.uiState.cols
                                    } else {
                                        vm.uiState.rows
                                    }
                                } else {
                                    (constraints.maxHeight / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                        vm.uiState.cols
                                    } else {
                                        vm.uiState.rows
                                    }
                                }.toSp()
                            }
                        )

                        TCRSquareGrid(
                            modifier = Modifier
                                .size(gridWidthDP, gridWidthDP),
                            cols = vm.uiState.cols,
                            rows = vm.uiState.rows
                        ) {
                            List(vm.uiState.rows * vm.uiState.cols)
                            {
                                val input: Char by vm.getLetter(it).observeAsState('\u0020')

                                GridCell(
                                    input.toString(),
                                    it,
                                    onIndexedValueChange = { input: TextFieldValue, index: Int ->
                                        vm.processInput(input, index)
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
                            val gameDuration: Int by vm.gameDuration.observeAsState(0)
                            GameTimeEntry(gameDuration = gameDuration, onDurationChange = {input: String -> vm.onDurationChange(input) } )

                        }

                        Row(Modifier.fillMaxWidth(1f), Arrangement.SpaceAround) {
                            Button(onClick = {
                                if (vm.letterCount.value!! > 0) {
                                    vm.saveGame()
                                } else {
                                    vm.setToastMsg(TOAST_MSGS.NO_TILES)

                                }
                            })
                            {
                                Text(stringResource(R.string.save_game))
                            }
                            Button(onClick = { vm.shuffleLetters() }) {
                                Text(stringResource(R.string.shuffle))
                            }
                            Button(onClick = { vm.clearLetters() }) {
                                Text(stringResource(R.string.clear))
                            }

                        }
                    }

                    Row(Modifier.fillMaxWidth(1f), Arrangement.SpaceAround) {
                        Button(onClick = {
                            if (vm.letterCount.value!! > 0) {
                                Toast.makeText(
                                    applicationContext,
                                    R.string.toast_game_saved,
                                    Toast.LENGTH_SHORT
                                ).show()
                                vm.saveGame()
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    R.string.toast_game_not_saved,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                        {
                            Text(stringResource(R.string.save_game))
                        }
                        Button(onClick = { vm.shuffleLetters() }) {
                            Text(stringResource(R.string.shuffle))
                        }
                        Button(onClick = { vm.clearLetters() }) {
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
                                offset = Offset(3.0f, 6.0f),
                                blurRadius = 2f
                            ),
                            fontSize = with(LocalDensity.current) {
                                if (constraints.maxHeight > constraints.maxWidth) {
                                    (constraints.maxWidth / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                        vm.uiState.cols
                                    } else {
                                        vm.uiState.rows
                                    }
                                } else {
                                    (constraints.maxHeight / 1.2f) / if (vm.uiState.cols > vm.uiState.rows) {
                                        vm.uiState.cols
                                    } else {
                                        vm.uiState.rows
                                    }
                                }.toSp()
                            }
                        )

                        TCRSquareGrid(
                            modifier = Modifier

                                .size(gridWidthDP, gridWidthDP),
                            cols = vm.uiState.cols,
                            rows = vm.uiState.rows
                        ) {
                            List(vm.uiState.rows * vm.uiState.cols)
                            {
                                val input: Char by vm.getLetter(it).observeAsState('\u0020')
                                GridCell(
                                    input.toString(),
                                    it,
                                    onIndexedValueChange = { input: TextFieldValue, index: Int ->
                                        vm.processInput(input, index)
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
                        val gameDuration: Int by vm.gameDuration.observeAsState(0)
                        GameTimeEntry(gameDuration = gameDuration, onDurationChange = {input: String -> vm.onDurationChange(input) } )
                    }


                    Column(
                        Modifier
                            .fillMaxHeight(1f)
                            .fillMaxWidth(0.6f)
                            .padding(0.dp, 0.dp, 36.dp, 0.dp),
                        Arrangement.SpaceAround
                    ) {
                        Button(onClick = { vm.saveGame() }) {
                            Text(stringResource(R.string.save_game))
                        }
                        Button(onClick = { vm.shuffleLetters() }) {
                            Text(stringResource(R.string.shuffle))
                        }
                        Button(onClick = { vm.clearLetters() }) {
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
            modifier = modifier,
            content = content
        ) { measurables, constraints ->

            val gridWidth =
                if (constraints.maxHeight > constraints.maxWidth) {
                    constraints.maxWidth
                } else {
                    constraints.maxHeight
                }

            val cellWidth = gridWidth / cols
            val cellHeight = gridWidth / rows

            val cellConstraints = Constraints(cellWidth, cellWidth, cellHeight, cellHeight)

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
                .background(MaterialTheme.colors.background)
                .padding(2.dp),

            )
        {

            Surface(
                color = MaterialTheme.colors.background,
                elevation = 36.dp

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




