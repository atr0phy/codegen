package dev.uuidv7id.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Kotlinコンパイラの `-P plugin:<id>:<option>=<value>` を受け取る入口。
 *
 * 現在のプラグインには設定項目がないが、compiler pluginとして発見されるために
 * `META-INF/services/...CommandLineProcessor` とこの実装が必要になる。
 */
class UuidV7CommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = UUID_V7_PLUGIN_ID

    // 将来、生成メソッド名などを設定可能にする場合はここへCliOptionを追加する。
    override val pluginOptions: Collection<CliOption> = emptyList()

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        // pluginOptionsが空なので、ここに到達するのは未定義のオプションが渡された場合だけ。
        error("Unexpected option: ${option.optionName}")
    }
}
