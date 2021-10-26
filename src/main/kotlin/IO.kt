import com.github.michaelbull.result.toResultOr
import com.github.michaelbull.result.Result

class IO<A>(val unsafeRun: () -> A) {
    fun <B> map(f: (A) -> B): IO<B> = IO { f(unsafeRun()) }
    fun <B> flatMap(f: (A) -> IO<B>): IO<B> = IO { f(unsafeRun()).unsafeRun() }
}

fun putLine(s: String): IO<Unit> = IO { println(s) }

fun put(s: String): IO<Unit> = IO { print(s) }

fun getLine(): IO<Result<String, InputError>> = IO { readLine().toResultOr { InputError.MissingInput } }