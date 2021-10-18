import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

fun getNextMove(board: Board, player: Player): String {
    // get all of my moves
    getWinningCoordinates(board, player)

    TODO("Finish Implementing")
}

fun getWinningCoordinates(board: Board, player: Player): Result<Coordinates, Throwable> {
    val myMoves = board.moves.filter { request -> request.player == player }
    val moves = myMoves.associateBy { request -> request.coordinates }
    val magicNumber = List(board.bounds) { it }.fold(0) { acc, i -> acc + i }

    for (i in 0..board.bounds) {
        val columnSquares = moves.filter { (coordinates, _) -> coordinates.x == i}
        if (columnSquares.count() == board.bounds - 1) {
            val partialMagicNumber = columnSquares.map { (coordinates, _) -> coordinates.y }.fold(0) { acc, n -> acc + n }
            val missingY = magicNumber - partialMagicNumber
            val coordinates = Coordinates(i, missingY)
            if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
        }

        val rowSquares = moves.filter { (coordinates, _) -> coordinates.y == i}
        if (rowSquares.count() == board.bounds - 1) {
            val partialMagicNumber = rowSquares.map { (coordinates, _) -> coordinates.x }.fold(0) { acc, n -> acc + n }
            val missingX = magicNumber - partialMagicNumber
            val coordinates = Coordinates(missingX, i)
            if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
        }
    }

    val diagonalSquares = moves.filter { (coordinates, _) -> (coordinates.x + coordinates.y) == (board.bounds - 1) }
    if (diagonalSquares.count() == board.bounds - 1) {
        val partialMagicNumberX = diagonalSquares.map { (coordinates, _) -> coordinates.x }.fold(0) { acc, n -> acc + n }
        val missingX = magicNumber - partialMagicNumberX
        val partialMagicNumberY = diagonalSquares.map { (coordinates, _) -> coordinates.y }.fold(0) { acc, n -> acc + n }
        val missingY = magicNumber - partialMagicNumberY
        val coordinates = Coordinates(missingX, missingY)
        if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
    }

    val antiDiagonalSquares = moves.filter { (coordinates, _) -> coordinates.x == coordinates.y }
    if (antiDiagonalSquares.count() == board.bounds - 1) {
        val partialMagicNumberX = antiDiagonalSquares.map { (coordinates, _) -> coordinates.x }.fold(0) { acc, n -> acc + n }
        val missingX = magicNumber - partialMagicNumberX
        val partialMagicNumberY = antiDiagonalSquares.map { (coordinates, _) -> coordinates.y }.fold(0) { acc, n -> acc + n }
        val missingY = magicNumber - partialMagicNumberY
        val coordinates = Coordinates(missingX, missingY)
        if (!board.moves.map(MoveRequest::coordinates).contains(coordinates)) return Ok(coordinates)
    }

    return Err(Throwable("No winning move"))
}