pluginManagement {
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

rootProject.name = "uuid-v7-id-plugin"

include("compiler-plugin")
include("gradle-plugin")
include("runtime")
