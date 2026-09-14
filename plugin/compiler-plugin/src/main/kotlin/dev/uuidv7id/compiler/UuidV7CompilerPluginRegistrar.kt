package dev.uuidv7id.compiler

import dev.uuidv7id.compiler.ir.UuidV7IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

internal const val UUID_V7_PLUGIN_ID = "dev.uuidv7id"

/**
 * Kotlinコンパイラへ、このプラグインの拡張処理を登録する入口。
 *
 * このクラス名は `META-INF/services/...CompilerPluginRegistrar` に記載されており、
 * Gradleからcompiler-plugin JARが渡されるとKotlinコンパイラがServiceLoader経由で生成する。
 * アプリケーションの実行時に呼ばれるクラスではない。
 */
class UuidV7CompilerPluginRegistrar : CompilerPluginRegistrar() {
    // Gradle plugin側のgetCompilerPluginId()と同じ値でなければならない。
    override val pluginId: String = UUID_V7_PLUGIN_ID

    // FIRを使うK2コンパイラ向けのプラグインであることを宣言する。
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // フロントエンド(FIR)でcompanion objectとgenerate()の「宣言」を追加する。
        // これにより、型検査やIDE補完からUserId.generate()が見えるようになる。
        FirExtensionRegistrarAdapter.registerExtension(UuidV7FirExtensionRegistrar())

        // バックエンド(IR)で、FIRが作ったgenerate()の「実装本体」を組み立てる。
        IrGenerationExtension.registerExtension(UuidV7IrGenerationExtension())
    }
}
