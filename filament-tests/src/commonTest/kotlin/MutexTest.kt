import io.karma.filament.Mutex
import io.karma.filament.Thread
import io.karma.filament.guarded
import kotlin.test.Test
import kotlin.test.assertEquals

class MutexTest {
    val mutex: Mutex = Mutex()
    var theValue: Int = 0

    @Test
    fun `Set value from another thread`() {
        val thread = Thread {
            mutex.guarded {
                theValue = 4444
            }
        }
        thread.join()
        mutex.guarded {
            assertEquals(4444, theValue)
        }
    }
}