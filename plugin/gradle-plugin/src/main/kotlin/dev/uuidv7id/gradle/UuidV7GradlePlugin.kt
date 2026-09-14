package dev.uuidv7id.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * 利用プロジェクトのGradle設定とcompiler pluginを接続するプラグイン。
 *
 * このクラスはコンパイル処理そのものを実装しない。利用中のKotlinバージョンに合う
 * compiler-plugin artifactを選び、runtime依存関係と一緒に各Kotlin compilationへ渡す。
 */
class UuidV7GradlePlugin : KotlinCompilerPluginSupportPlugin {
    // getPluginArtifact()にはProject引数がないため、apply時に受け取ったProjectを保持する。
    private lateinit var target: Project

    override fun apply(target: Project) {
        this.target = target
    }

    // main/testなど、プロジェクト内のすべてのKotlin compilationで有効にする。
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    // compiler-plugin側のCompilerPluginRegistrar.pluginIdと一致させる。
    override fun getCompilerPluginId(): String = PLUGIN_ID

    @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
    override fun getPluginArtifact(): SubpluginArtifact {
        // 利用プロジェクトが実際に選択したcompiler versionを取得する。
        // 例: Kotlin 2.4.20なら、compiler専用artifactの2.4.20-0.1.1を選ぶ。
        val compilerVersion = target.extensions
            .getByType(KotlinBaseExtension::class.java)
            .compilerVersion
            .get()

        return SubpluginArtifact(
            groupId = PLUGIN_GROUP,
            artifactId = "compiler-plugin",
            version = "$compilerVersion-$PLUGIN_VERSION",
        )
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        // 生成されたbytecodeがnextUuidV7()を呼ぶため、利用側のruntime classpathへ追加する。
        kotlinCompilation.defaultSourceSet.dependencies {
            implementation("$PLUGIN_GROUP:runtime:$PLUGIN_VERSION")
        }

        // compiler pluginへ渡す設定値は今のところない。
        return kotlinCompilation.target.project.provider { emptyList() }
    }

    private companion object {
        const val PLUGIN_ID = "dev.uuidv7id"
        const val PLUGIN_GROUP = "dev.uuidv7id"
        const val PLUGIN_VERSION = "0.1.1"
    }
}
