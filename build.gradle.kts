plugins {
    kotlin("jvm") version "2.3.0-RC3"
    id("com.gradleup.shadow") version "9.3.0"
}

group = "dev.danielmillar"
version = "1.1.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.skriptlang.org/releases") {
        name = "skriptlang-repo"
    }
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/"){
        name = "infernal-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.github.SkriptLang:Skript:2.13.2")
    compileOnly("com.infernalsuite.asp:api:4.0.0-SNAPSHOT")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.infernalsuite.asp:loaders:4.0.0-SNAPSHOT")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
}

val targetJavaVersion = 25
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
