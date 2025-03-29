import dev.karmakrafts.filament.SharedMutex
import dev.karmakrafts.filament.Thread
import dev.karmakrafts.filament.guarded
import dev.karmakrafts.filament.guardedWrite
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

    @Test
    fun `Create multiple shared mutexes in a loop`() {
        for (index in 0..<1000) {
            val thread = Thread {
                mutex.guardedWrite {
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