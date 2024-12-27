import io.karma.pthread.Thread
import kotlin.test.Test

/**
 * @author Alexander Hinze
 * @since 27/12/2024
 */
class CreateJoinTest {
    @Test
    fun `Create thread and join`() {
        val thread = Thread.create {}
        thread.join()
    }
}