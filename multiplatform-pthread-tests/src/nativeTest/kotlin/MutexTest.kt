import io.karma.pthread.Mutex
import io.karma.pthread.Thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Alexander Hinze
 * @since 27/12/2024
 */
class MutexTest {
    private val mutex: Mutex = Mutex.create()
    private var theValue: Int = 0

    @Test
    fun `Set value from another thread`() {
        val thread = Thread.create {
            mutex.guarded {
                theValue = 4444
            }
        }
        thread.join()
        mutex.guarded {
            assertEquals(4444, theValue)
        }
    }

    @AfterTest
    fun tearDown() {
        mutex.close() // Free mutex memory
    }
}