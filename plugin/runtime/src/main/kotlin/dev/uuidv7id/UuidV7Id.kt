package dev.uuidv7id

/**
 * UUID v7 ID生成の対象であることをcompiler pluginへ伝えるアノテーション。
 *
 * 対象は、`java.util.UUID`を1つ受け取るprimary constructorを持つJVM value class。
 * companion objectがなければ生成し、既存または生成したcompanionへ
 * `generate(): 対象クラス` を追加する。
 *
 * このアノテーション自身が実行時にコードを生成するわけではない。Kotlinコンパイル中に
 * compiler pluginが検出し、FIRへ宣言、IRへ実装を追加する。
 */
@Target(AnnotationTarget.CLASS)
// compiler pluginがコンパイル時に読めればよく、実行時のreflectionでは使用しない。
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
public annotation class UuidV7Id
