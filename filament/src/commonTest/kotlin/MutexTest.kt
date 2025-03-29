import dev.karmakrafts.filament.Mutex
import dev.karmakrafts.filament.Thread
import dev.karmakrafts.filament.guarded
import kotlin.test.Test
import kotlin.test.assertEquals

class MutexTest {
    private var theValue: Int = 0
    private val mutex = Mutex()

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

    @Test
    fun `Create multiple mutexes in a loop`() {
        for (index in 0..<1000) {
            val thread = Thread {
                mutex.guarded {
                    theValue = index
                }
            }
            thread.join()
            mutex.guarded {
                assertEquals(index, theValue)
            }
        }
    }
}