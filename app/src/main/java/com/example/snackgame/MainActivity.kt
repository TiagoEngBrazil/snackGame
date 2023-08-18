package com.example.snackgame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.snackgame.ui.theme.Shapes
import com.example.snackgame.ui.theme.SnackGameTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = Game(lifecycleScope)

        setContent {
            SnackGameTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background) {
                    Snack(game)
                }
            }
        }
    }

}

data class State(val food: Pair<Int, Int>, val snack: List<Pair<Int, Int>>)

class Game(private val scope: CoroutineScope) {

    private val mutex: Mutex = Mutex()
    private val mutableState =
        MutableStateFlow(State(food = Pair(5, 5), snack = listOf(Pair(7, 7))))
    val state = mutableState

    var move: Pair<Int, Int> = Pair(1, 0)
        set(value: Pair<Int, Int>) {
            scope.launch {
                mutex.withLock {
                    field = value
                }
            }
        }

    init {
        scope.launch {
            var snackLenth = 4

            while (true) {
                delay(150)
                mutableState.update {
                    val newPosition: Pair<Int, Int> = it.snack.first().let { poz: Pair<Int, Int> ->
                        mutex.withLock {
                            Pair(
                                (poz.first + move.first) % BOARD_SIZE,
                                (poz.second + move.second) % BOARD_SIZE
                            )
                        }
                    }

                    if (newPosition == it.food) {
                        snackLenth++
                    }

                    if (it.snack.contains(newPosition)) {
                        snackLenth = 4
                    }

                    it.copy(
                        food = if (newPosition == it.food) Pair(

                            Random.nextInt(BOARD_SIZE),
                            Random.nextInt(BOARD_SIZE))
                        else it.food,

                        snack = listOf(newPosition) + it.snack.take(snackLenth - 1)
                    )
                }
            }
        }
    }

    companion object {
        const val BOARD_SIZE = 16
    }

}

@Composable
fun Snack(game: Game) {
    val state = game.state.collectAsState(initial = null)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        state.value?.let {
            Board(it)
        }
        Buttons {
            game.move = it
        }
    }
}

@Composable
fun Board(state: State) {
    BoxWithConstraints(Modifier.padding(16.dp)) {
        val tileSize = maxWidth / Game.BOARD_SIZE

        Box(Modifier
            .size(maxWidth)
            .border(2.dp, Color.Blue
            ))

        Box(Modifier
            .offset(x = tileSize * state.food.first, y = tileSize * state.food.second)
            .size(tileSize)
            .background(
                Color.Blue, CircleShape))

        state.snack.forEach {
            Box(modifier = Modifier
                .offset(x = tileSize * it.first, y = tileSize * it.second)
                .size(tileSize)
                .background(
                    Color.Blue, Shapes.small))
        }
    }
}

@Composable
fun Buttons(onDirectionchange: (Pair<Int, Int>) -> Unit) {
    val buttonSize = Modifier.size(64.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Button(onClick = { onDirectionchange(Pair(0, -1)) }, modifier = buttonSize) {

            Icon(Icons.Default.KeyboardArrowUp, null)
        }
        Row {
            Button(onClick = { onDirectionchange(Pair(-1, 0)) }, modifier = buttonSize) {

                Icon(Icons.Default.KeyboardArrowLeft, null)
            }
            Spacer(modifier = buttonSize)
            Button(onClick = { onDirectionchange(Pair(1, 0)) }, modifier = buttonSize) {

                Icon(Icons.Default.KeyboardArrowRight, null)
            }
            Button(onClick = { onDirectionchange(Pair(0, 1)) }, modifier = buttonSize) {

                Icon(Icons.Default.KeyboardArrowDown, null)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SnackGameTheme {

    }
}

