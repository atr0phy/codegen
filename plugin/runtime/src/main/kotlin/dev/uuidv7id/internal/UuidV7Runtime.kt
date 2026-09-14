package dev.uuidv7id.internal

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * compiler pluginが生成した `generate()` から呼ばれるruntime側の実装。
 *
 * UUID生成をIR上で直接再現せず通常のKotlin関数に置くことで、compiler plugin側は
 * この関数を呼ぶIRだけ生成すればよくなる。生成された呼び出しは静的に解決されるため、
 * reflectionは使用しない。
 */
@Suppress("unused") // ソース上の呼び出し元はなく、compiler pluginがIRへ呼び出しを追加する。
@OptIn(ExperimentalUuidApi::class)
fun nextUuidV7(): UUID = Uuid.generateV7().toJavaUuid()
