plugins {
    kotlin("jvm") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.0"
}

group = "dev.danielmillar"
version = "2.0.0-beta.1"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
    maven("https://oss.sonatype.org/content/groups/public/") { name = "sonatype" }
    maven("https://repo.skriptlang.org/releases") { name = "skriptlang-repo" }
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/"){ name = "infernal-repo" }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    compileOnly("com.github.SkriptLang:Skript:2.13.2")

    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")
    implementation("com.infernalsuite.asp:loaders:4.0.0-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("org.spongepowered:configurate-yaml:4.2.0-GeyserMC-SNAPSHOT")
    implementation("org.spongepowered:configurate-extra-kotlin:4.2.0-GeyserMC-SNAPSHOT")
}

val targetJavaVersion = 25
kotlin {
    jvmToolchain(targetJavaVersion)
    compilerOptions {
        javaParameters = true // Handles parameter names for both Kotlin and Java
        freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        relocate("com.infernalsuite.asp.loaders", "dev.danielmillar.asp.loaders")
        relocate("org.jetbrains.kotlin", "dev.danielmillar.kotlin")
        relocate("org.jetbrains.kotlinx", "dev.danielmillar.kotlinx")
        relocate("org.spongepowered.configurate", "dev.danielmillar.configurate")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
