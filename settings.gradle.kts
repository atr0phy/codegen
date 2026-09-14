pluginManagement {
    val consumerKotlinVersion = providers.gradleProperty("consumerKotlinVersion")
        .getOrElse("2.4.10")

    plugins {
        id("org.jetbrains.kotlin.jvm") version consumerKotlinVersion
    }

    includeBuild("plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "codegen"

includeBuild("plugin") {
    dependencySubstitution {
        substitute(module("dev.uuidv7id:compiler-plugin")).using(project(":compiler-plugin"))
        substitute(module("dev.uuidv7id:runtime")).using(project(":runtime"))
    }
}
