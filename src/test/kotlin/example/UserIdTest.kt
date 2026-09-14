package example

import kotlin.test.Test
import kotlin.test.assertEquals

class UserIdTest {
    @Test
    fun `generated factory returns a UUID v7`() {
        val generated = UserId.generate()

        assertEquals(7, generated.value.version())
    }
}
