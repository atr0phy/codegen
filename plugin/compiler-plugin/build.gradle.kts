plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    val compilerApiVersion = providers.gradleProperty("uuidv7CompilerApiVersion")
        .getOrElse("2.4.10")
    val ideCompilerJar = providers.gradleProperty("uuidv7IdeCompilerJar")
    val ideCompilerLibDir = providers.gradleProperty("uuidv7IdeCompilerLibDir")
    if (ideCompilerLibDir.isPresent) {
        compileOnly(fileTree(ideCompilerLibDir.get()) {
            include("kotlin-plugin.jar")
            include("kotlinc*.jar")
        })
    } else if (ideCompilerJar.isPresent) {
        compileOnly(files(ideCompilerJar.get()))
    } else {
        // Build against the oldest supported compiler API. The resulting JAR is
        // tested against every compiler version listed in the compatibility matrix.
        compileOnly("org.jetbrains.kotlin:kotlin-compiler:$compilerApiVersion")
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

publishing {
    publications {
        create<MavenPublication>("compilerPlugin") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "kefs"
            url = rootProject.layout.projectDirectory.dir("../.kefs/repository").asFile.toURI()
        }
    }
}
