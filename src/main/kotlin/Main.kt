import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

const val DEPTH = 3
private val LOGGER: Logger = LogManager.getLogger("Main")

fun main() = runBlocking {
    val state = FullGameState(null, GameStage.ME_UP, toCardsSet("KC", "4S", "6C"), toCardsSet("AD", "5S", "6H"), toCards("2C", "5D"))
    LOGGER.info(state.getBestMove(DEPTH))
}

fun input(prompt: String): String {
    print(prompt)
    return readln()
}