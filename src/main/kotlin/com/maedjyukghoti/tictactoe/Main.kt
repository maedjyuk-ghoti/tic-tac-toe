package com.maedjyukghoti.tictactoe

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.maedjyukghoti.tictactoe.display.UserInterface
import com.maedjyukghoti.tictactoe.logic.GameOptions
import com.maedjyukghoti.tictactoe.logic.InvalidGameOptions
import com.maedjyukghoti.tictactoe.logic.PlayerInfo
import com.maedjyukghoti.tictactoe.logic.players.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun getopt(args: Array<String>): Map<String, List<String>> =
    args.fold(mutableListOf()) { acc: MutableList<MutableList<String>>, s: String ->
        acc.apply {
            if (s.startsWith('-')) {
                add(mutableListOf(s))
            } else {
                last().add(s)
            }
        }
    }.associate { it[0] to it.drop(1) }

fun main(args: Array<String>) =
    runBlocking {
        val gameOptions: Result<GameOptions, InvalidGameOptions> = GameOptions.parse(getopt(args))
        val initialState: AppState =
            gameOptions.fold(
                success = { validOptions -> AppState.Game.createNewGame(validOptions) },
                failure = { _ -> AppState.FatalError },
            )
        val state: MutableStateFlow<AppState> = MutableStateFlow(initialState)

        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val ui: UserInterface = UserInterface.CLI(coroutineScope)

        // Keep forwarding user intents to the game state
        val userIntentToGameStateJob =
            launch {
                state.combine(ui.userIntent, ::Pair)
                    .takeWhile { (appState, _) -> appState != AppState.Exit }
                    .map { (_, userIntent) -> userIntent }
                    .distinctUntilChanged()
                    .collect { userIntent -> state.value = state.value.handleAction(userIntent) }
            }

        // Let any bots have a chance to play
        val botIntentToGameStateJob =
            launch {
                state.takeWhile { appState -> appState != AppState.Exit }
                    .collect { appState ->
                        println(appState)
                        (
                            (appState as? AppState.Game)
                                ?.getCurrentPlayer() as? Player.Bot
                        )
                            ?.getAction(appState)
                            ?.also { intent -> println(intent) }
                            ?.let { botIntent -> state.value = state.value.handleAction(botIntent) }
                    }
            }

        // Keep updating app state
        val fatalErrorWatchJob =
            launch {
                state.takeWhile { appState -> appState != AppState.Exit }
                    .filter { appState -> appState == AppState.FatalError }
                    .collect { _ ->
                        state.value = AppState.Exit
                    }
            }

        // Keep screen in sync with app state
        val renderJob =
            launch {
                state.takeWhile { appState -> appState != AppState.Exit }
                    .collect { appState -> ui.render(appState) }
            }

        val winnerJob =
            launch {
                state.takeWhile { appState -> appState != AppState.Exit }
                    .collect { appState ->
                        (appState as? AppState.Game)?.winner
                            ?.let { winner ->
                                if (winner != PlayerInfo.None) {
                                    state.value = AppState.Exit
                                }
                            }
                    }
            }

        // Make sure to cancel anything in the scopes we've created
        val exitWatchJob =
            launch {
                state.filter { appState -> appState == AppState.Exit }
                    .take(1)
                    .collect {
                        coroutineScope.cancel()
                        ui.destroy()
                    }
            }

        userIntentToGameStateJob.join()
        botIntentToGameStateJob.join()
        fatalErrorWatchJob.join()
        renderJob.join()
        winnerJob.join()
        exitWatchJob.join()
    }
