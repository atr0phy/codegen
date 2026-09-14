plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.10")
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("uuidV7Id") {
            id = "dev.uuidv7id"
            displayName = "UUID v7 ID compiler plugin"
            description = "Adds UUID v7 generate() factories to annotated Kotlin value classes"
            implementationClass = "dev.uuidv7id.gradle.UuidV7GradlePlugin"
        }
    }
}
