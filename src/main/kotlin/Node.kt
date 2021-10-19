
data class Node<out T> (
    val value: T,
    val left: Node<T>?,
    val right: Node<T>?
)

data class Choice(val moveRequest: MoveRequest, val value: Int)