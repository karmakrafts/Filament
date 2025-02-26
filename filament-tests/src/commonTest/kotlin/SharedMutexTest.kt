import io.karma.filament.SharedMutex
import io.karma.filament.Thread
import io.karma.filament.guarded
import io.karma.filament.guardedWrite
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedMutexTest {
    private val mutex: SharedMutex = SharedMutex()
    private var theValue: Int = 0

    @Test
    fun `Set value from another thread`() {
        val thread = Thread {
            mutex.guardedWrite {
                theValue = 4444
            }
        }
        thread.join()
        val thread2 = Thread {
            mutex.guarded {
                assertEquals(4444, theValue)
            }
        }
        mutex.guarded {
            assertEquals(4444, theValue)
        }
        thread2.join()
    }
}