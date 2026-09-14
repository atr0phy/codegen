package dev.uuidv7id.compiler

import dev.uuidv7id.compiler.fir.UuidV7DeclarationGenerator
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

/** FIRフェーズで使用する拡張をまとめて登録する。 */
class UuidV7FirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        // `+::...` は、各FirSessionにDeclarationGeneratorを生成するための登録DSL。
        +::UuidV7DeclarationGenerator
    }
}
