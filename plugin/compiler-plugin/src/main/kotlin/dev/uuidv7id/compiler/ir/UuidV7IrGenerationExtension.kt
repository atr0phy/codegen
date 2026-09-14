package dev.uuidv7id.compiler.ir

import dev.uuidv7id.compiler.fir.UuidV7DeclarationGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * FIRで宣言した `generate()` に、JVMコードへ変換できる関数本体を与えるIR拡張。
 *
 * 生成する処理は、Kotlinコードで表すと次と同じ形になる。
 *
 * ```kotlin
 * fun generate(): UserId {
 *     val uuid = dev.uuidv7id.internal.nextUuidV7()
 *     val id = UserId(uuid)
 *     return id
 * }
 * ```
 *
 * リフレクションは使わない。コンパイル中に関数呼び出しとconstructor呼び出しをIRへ埋め込み、
 * そのIRから通常のKotlinコードと同様にJVM bytecodeが生成される。
 */
class UuidV7IrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // コンパイル対象モジュールのIRツリーを走査し、FIR側で生成したgenerate()を探す。
        moduleFragment.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                // 宣言を含み得る要素だけ再帰する。式の内部まで無条件に走査する必要はない。
                when (element) {
                    is IrDeclaration,
                    is IrFile,
                    is IrModuleFragment -> element.acceptChildrenVoid(this)
                    else -> Unit
                }
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                // 名前だけではユーザー定義のgenerate()と区別できない。
                // FIRで付けたKeyも確認し、このプラグイン自身が生成した関数だけを書き換える。
                val origin = declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin
                if (origin?.pluginKey == UuidV7DeclarationGenerator.Key &&
                    declaration.name == UuidV7DeclarationGenerator.GENERATE_NAME
                ) {
                    // generate()の親はcompanion、その親が戻り値となるUserIdなどのIDクラス。
                    val companion = declaration.parent as IrClass
                    val idClass = companion.parentAsClass
                    val constructor = idClass.primaryConstructor
                        ?: error("@UuidV7Id requires a primary constructor: ${idClass.name}")

                    // runtime moduleにある実関数をシンボルとして解決する。
                    // finderForSourceを使うことで、このソースから参照可能なclasspathを検索する。
                    val nextUuidV7 = pluginContext.finderForSource(declaration.file).findFunctions(NEXT_UUID_V7).singleOrNull()
                        ?: error("UUID v7 runtime function was not found on the compilation classpath")

                    // 空だったgenerate()のbodyに、UUID生成 → IDのconstructor呼び出し → returnを構築する。
                    declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).irBlockBody {
                        val uuid = irCall(nextUuidV7)
                        val id = irCall(constructor.symbol).apply {
                            // value classのprimary constructorが受け取る先頭引数へUUIDを渡す。
                            arguments[0] = uuid
                        }
                        +irReturn(id)
                    }
                }

                // 将来、関数内に生成対象を追加した場合にも探索できるよう子要素へ進む。
                declaration.acceptChildrenVoid(this)
            }
        })
    }

    private companion object {
        // importの有無に左右されないよう、runtime関数を完全修飾名で指定する。
        val NEXT_UUID_V7 = CallableId(
            FqName("dev.uuidv7id.internal"),
            Name.identifier("nextUuidV7"),
        )
    }
}
