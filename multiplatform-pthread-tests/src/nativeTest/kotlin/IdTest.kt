import io.karma.pthread.Thread
import kotlin.test.Test
import kotlin.test.assertNotEquals

/**
 * @author Alexander Hinze
 * @since 05/01/2025
 */
class IdTest {
    @Test
    fun `Check current thread ID`() {
        val id = Thread.id
        println("Current thread ID: $id")
        assertNotEquals(0UL, id)
    }

    @Test
    fun `Check separate thread ID`() {
        Thread.create {
            val id = Thread.id
            println("Separate thread ID: $id")
            assertNotEquals(0UL, id)
        }.join()
    }
}