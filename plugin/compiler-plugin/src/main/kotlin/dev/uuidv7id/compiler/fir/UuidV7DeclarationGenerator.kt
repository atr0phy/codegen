package dev.uuidv7id.compiler.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.getContainingDeclaration
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * `@UuidV7Id` が付いたクラスに、コンパイラから見える宣言を追加するFIR拡張。
 *
 * 例えば `value class UserId(val value: UUID)` に対して、ソースコード上に次の宣言が
 * 書かれているかのようなFIRシンボルを生成する。
 *
 * ```kotlin
 * companion object {
 *     fun generate(): UserId
 * }
 * ```
 *
 * FIRは名前解決・型検査・IDE補完などに使われるため、ここで宣言を追加すると
 * `UserId.generate()` を通常のKotlinコードとして参照できる。ただし関数本体は作らない。
 * 実行可能な本体は後段の [dev.uuidv7id.compiler.ir.UuidV7IrGenerationExtension] が生成する。
 */
class UuidV7DeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    /**
     * クラスの内側に生成し得るクラス名を、FIRへ事前申告する。
     * `Companion` を申告しないと、後続の生成コールバックが呼ばれない。
     */
    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> = if (classSymbol.hasUuidV7IdAnnotation()) {
        setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
    } else {
        emptySet()
    }

    /** `@UuidV7Id` 対象にcompanion objectが存在しない場合だけ、それを生成する。 */
    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        val regularOwner = owner as? FirRegularClassSymbol ?: return null
        if (!owner.hasUuidV7IdAnnotation()) return null

        // companionObjectSymbolはコンパイラ内部APIなので、明示的なOptInが必要。
        // ユーザーが自分でcompanion objectを書いている場合は、それをそのまま利用する。
        @OptIn(SymbolInternals::class)
        if (regularOwner.companionObjectSymbol != null) return null

        // Keyをoriginとして記録することで、後から「このプラグインが生成した宣言」と判定できる。
        return createCompanionObject(regularOwner, Key).symbol
    }

    /**
     * companion objectに生成し得るメンバー名を申告する。
     *
     * `generate` は既存・生成companionの両方に追加する。`<init>` は、このプラグインが
     * companion自体を生成した場合だけ申告し、後でprivate constructorを追加する。
     */
    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> {
        if (!classSymbol.isCompanion || classSymbol.annotatedContainingClass() == null) return emptySet()

        val names = mutableSetOf(GENERATE_NAME)
        val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
        if (origin?.key == Key) names += SpecialNames.INIT
        return names
    }

    /** 申告済みの `generate(): 対象ID型` の関数シンボルを作る。 */
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        if (callableId.callableName != GENERATE_NAME) return emptyList()
        val companion = context?.owner ?: return emptyList()
        val idClass = companion.annotatedContainingClass() ?: return emptyList()

        // 戻り値はアノテーションが付いた外側のクラス。引数と本体は持たない。
        val function = createMemberFunction(
            companion,
            Key,
            GENERATE_NAME,
            idClass.constructType(emptyArray()),
        )
        return listOf(function.symbol)
    }

    /** 自動生成したcompanion objectへ通常と同じprivateなデフォルトconstructorを追加する。 */
    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val origin = context.owner.origin as? FirDeclarationOrigin.Plugin
        if (origin?.key != Key) return emptyList()
        return listOf(createDefaultPrivateConstructor(context.owner, Key).symbol)
    }

    /** predicateベースの検索対象として `@UuidV7Id` を登録する。 */
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(HAS_UUID_V7_ID)
    }

    // 毎回annotation配列を直接走査せず、FIRが構築したpredicate indexで判定する。
    private fun FirClassSymbol<*>.hasUuidV7IdAnnotation(): Boolean =
        session.predicateBasedProvider.matches(HAS_UUID_V7_ID, this)

    /** このシンボルが対象IDクラスのcompanionなら、その外側のIDクラスを返す。 */
    private fun FirClassSymbol<*>.annotatedContainingClass(): FirClassSymbol<*>? {
        if (!isCompanion) return null
        val containingClass = getContainingDeclaration(session) as? FirClassSymbol<*> ?: return null
        return containingClass.takeIf { it.hasUuidV7IdAnnotation() }
    }

    /** このプラグインが生成した宣言をFIR/IR間で識別する印。 */
    object Key : GeneratedDeclarationKey()

    companion object {
        /** FIRとIRの両方で共有する生成メソッド名。 */
        val GENERATE_NAME: Name = Name.identifier("generate")

        // FQCNで指定するため、利用側のimport名やaliasには影響されない。
        private val HAS_UUID_V7_ID = DeclarationPredicate.create {
            annotated(setOf(FqName("dev.uuidv7id.UuidV7Id")))
        }
    }
}
