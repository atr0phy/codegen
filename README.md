# UUID v7 ID compiler plugin prototype

This project demonstrates a Kotlin K2 FIR + IR compiler plugin. Annotating a
JVM value class with `@UuidV7Id` generates a companion object when necessary
and adds a real `generate()` member:

```kotlin
@UuidV7Id
@JvmInline
value class UserId(val value: UUID)

val id: UserId = UserId.generate()
```

FIR makes the generated companion and function visible to name resolution.
IR supplies a body equivalent to `UserId(nextUuidV7())`. The `runtime` artifact
contains both the source annotation and the small function called by generated
code. That function delegates UUID generation to Kotlin's UUID v7 API.

## Artifact layout

Consumers apply only one Gradle plugin:

```kotlin
plugins {
    kotlin("jvm") version "2.4.20"
    id("dev.uuidv7id") version "0.1.1"
}
```

The Gradle plugin adds the other two artifacts automatically. Runtime follows
the library version, while compiler-plugin artifacts include the exact compiler
version they were built for:

```text
dev.uuidv7id:gradle-plugin:0.1.1
    ├── compiler classpath: dev.uuidv7id:compiler-plugin:2.4.20-0.1.1
    └── application classpath: dev.uuidv7id:runtime:0.1.1
```

The annotation and generated-code runtime deliberately share one artifact.
Combining either of them with the Gradle or compiler plugin would put Gradle or
compiler implementation classes on the application classpath.

## Kotlin compatibility

Gradle reads the compilation's actual `compilerVersion` and resolves the
matching compiler-plugin artifact. This also handles builds that configure a
compiler version different from the Kotlin Gradle Plugin version through the
Build Tools API.

| Plugin | Kotlin compiler | Compiler artifact | Status |
|---|---|---|---|
| 0.1.1 | 2.4.10 | `2.4.10-0.1.1` | tested |
| 0.1.1 | 2.4.20 | `2.4.20-0.1.1` | tested |

Run either compatibility check by selecting the consumer compiler version:

```shell
./gradlew test \
  -PconsumerKotlinVersion=2.4.10 \
  -Puuidv7CompilerApiVersion=2.4.10 \
  -Puuidv7CompilerArtifactVersion=2.4.10-0.1.1

./gradlew test \
  -PconsumerKotlinVersion=2.4.20 \
  -Puuidv7CompilerApiVersion=2.4.20 \
  -Puuidv7CompilerArtifactVersion=2.4.20-0.1.1
```

Build a compiler artifact by selecting both its compiler API and published
version:

```shell
./gradlew -p plugin :compiler-plugin:publish \
  -Puuidv7CompilerApiVersion=2.4.20 \
  -Puuidv7CompilerArtifactVersion=2.4.20-0.1.1
```

IDE analysis is a separate compiler host. The KEFS project configuration uses
`<kotlin-version>-<lib-version>`, so an IDE compiler such as
`2.4.20-ij262-52` resolves `compiler-plugin:2.4.20-ij262-52-0.1.1`. Build that
artifact against the jars loaded by the IDE itself, rather than the separate
command-line compiler under `plugins/Kotlin/kotlinc`. For a Toolbox installation:

```shell
./gradlew -p plugin \
  :compiler-plugin:clean \
  :compiler-plugin:publishAllPublicationsToKefsRepository \
  -Puuidv7IdeCompilerLibDir="$HOME/.local/share/JetBrains/Toolbox/apps/intellij-idea-ultimate/plugins/Kotlin/lib" \
  -Puuidv7CompilerArtifactVersion=2.4.20-ij262-52-0.1.1
```

Set `uuidv7CompilerArtifactVersion` to the exact coordinate requested by KEFS.
The public Gradle plugin and runtime remain version `0.1.1`. Restart IntelliJ
after replacing an artifact that its Kotlin plugin has already loaded.

Run the sample and tests with:

```shell
./gradlew run
./gradlew test
```

The prototype currently assumes a JVM value class with exactly one public
primary-constructor parameter of type `java.util.UUID`. Adding FIR diagnostics
for invalid annotated declarations is the next useful extension.
