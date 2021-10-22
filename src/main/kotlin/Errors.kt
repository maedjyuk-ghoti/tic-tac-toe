sealed interface GameError {
    fun getMessage(): String
}

sealed class InputError : GameError {
    object MissingInput : InputError() {
        override fun getMessage(): String {
            return "No input received"
        }
    }

    class InvalidAction(val input: String) : InputError() {
        override fun getMessage(): String {
            return "Invalid input"
        }
    }

    class InvalidCoordinates(val input: String) : InputError() {
        override fun getMessage(): String {
            return "Invalid Coordinates"
        }
    }
}

sealed class MoveError : GameError {
    object NoAvailableMoves : MoveError() {
        override fun getMessage(): String {
            return "No available moves"
        }
    }

    object NoWinningMove : MoveError() {
        override fun getMessage(): String {
            return "No winning move"
        }
    }

    class InvalidCoordinates(val coordinates: Coordinates) : MoveError() {
        override fun getMessage(): String {
            return "Invalid coordinates"
        }
    }

    object CoordinateTaken : MoveError() {
        override fun getMessage(): String {
            return "That square has already been played"
        }

    }
}

sealed class UndoError: GameError {
    object NoMovesToUndo : UndoError() {
        override fun getMessage(): String {
            return "No moves to undo"
        }
    }

    object RequestTooLarge : UndoError() {
        override fun getMessage(): String {
            return "Requested more moves than are present"
        }
    }
}