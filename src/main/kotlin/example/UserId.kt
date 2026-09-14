package example

import dev.uuidv7id.UuidV7Id
import java.util.UUID

// コンパイル時に `companion object` と `fun generate(): UserId` が追加される。
@UuidV7Id
@JvmInline
value class UserId(val value: UUID)
