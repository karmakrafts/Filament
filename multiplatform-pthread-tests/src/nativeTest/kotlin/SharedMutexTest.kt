import io.karma.pthread.SharedMutex
import io.karma.pthread.Thread
import io.karma.pthread.guarded
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Alexander Hinze
 * @since 27/12/2024
 */
class SharedMutexTest {
    private val mutex: SharedMutex = SharedMutex.create()
    private var theValue: Int = 0

    @Test
    fun `Set value from another thread`() {
        val thread = Thread.create {
            mutex.guardedWrite {
                theValue = 4444
            }
        }
        thread.join()
        val thread2 = Thread.create {
            mutex.guarded {
                assertEquals(4444, theValue)
            }
        }
        mutex.guarded {
            assertEquals(4444, theValue)
        }
        thread2.join()
    }

    @AfterTest
    fun tearDown() {
        mutex.close() // Free mutex memory
    }
}