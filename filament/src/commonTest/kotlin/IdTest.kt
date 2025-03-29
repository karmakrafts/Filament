import dev.karmakrafts.filament.Thread
import kotlin.test.Test
import kotlin.test.assertNotEquals

class IdTest {
    @Test
    fun `Check current thread ID`() {
        val id = Thread.id
        println("Current thread ID: $id")
        assertNotEquals(0UL, id)
    }

    @Test
    fun `Check separate thread ID`() {
        Thread {
            val id = Thread.id
            println("Separate thread ID: $id")
            assertNotEquals(0UL, id)
        }.join()
    }
}