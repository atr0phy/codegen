plugins {
    kotlin("jvm") version "2.4.10" apply false
}

val uuidv7PluginVersion = providers.gradleProperty("uuidv7PluginVersion")
    .getOrElse("0.1.1")

allprojects {
    group = "dev.uuidv7id"
    version = uuidv7PluginVersion
}

project(":compiler-plugin") {
    val compilerApiVersion = providers.gradleProperty("uuidv7CompilerApiVersion")
        .getOrElse("2.4.10")
    version = providers.gradleProperty("uuidv7CompilerArtifactVersion")
        .getOrElse("$compilerApiVersion-$uuidv7PluginVersion")
}
