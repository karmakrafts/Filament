import io.karma.filament.Thread
import kotlin.test.Test

class CreateJoinTest {
    @Test
    fun `Create thread and join`() {
        val thread = Thread {}
        thread.join()
    }
}