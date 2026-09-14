package example

import dev.uuidv7id.UuidV7Id
import java.util.UUID

@UuidV7Id
@JvmInline
value class TenantId(val value: UUID) {
    companion object {
        fun hasCompanion(): Boolean = true
    }
}