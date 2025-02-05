package com.maedjyukghoti.tictactoe

import com.github.michaelbull.result.*
import com.maedjyukghoti.tictactoe.display.*
import com.maedjyukghoti.tictactoe.logic.*
import com.maedjyukghoti.tictactoe.logic.players.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds

fun getopt(args: Array<String>): Map<String, List<String>> =
    args.fold(mutableListOf()) { acc: MutableList<MutableList<String>>, s: String ->
        acc.apply {
            if (s.startsWith('-')) add(mutableListOf(s))
            else last().add(s)
        }
    }.associate { it[0] to it.drop(1) }

fun main(args: Array<String>) = runBlocking {
    val gameOptions: Result<GameOptions, InvalidGameOptions> = GameOptions.parse(getopt(args))
    val initialState: AppState = gameOptions.fold(
        success = { validOptions -> AppState.Game.createNewGame(validOptions) },
        failure = { _ -> AppState.FatalError }
    )
    val state: MutableStateFlow<AppState> = MutableStateFlow(initialState)

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    val ui: UserInterface = UserInterface.CLI(coroutineScope)

    launch {
        state.zip(ui.userIntent, ::Pair)
            .takeWhile { (appState, _) -> appState != AppState.Exit }
            .map { (_, userIntent) -> userIntent }
            .collect { userIntent -> state.value = state.value.handleAction(userIntent) }
    }

    launch {
        state.takeWhile { appState -> appState != AppState.Exit }
            .filter { appState -> appState == AppState.FatalError }
            .collect { _ ->
                delay(5.seconds)
                state.value = AppState.Exit
            }
    }

    launch {
        state.takeWhile { appState -> appState != AppState.Exit }
            .collect { appState -> ui.render(appState) }
    }

    launch {
        state.filter { appState -> appState == AppState.Exit }
            .take(1)
            .collect {
                coroutineScope.cancel()
                ui.destroy()
            }
    }

    Unit // main must return Unit to compile correctly as a main function
}