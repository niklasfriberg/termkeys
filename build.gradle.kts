plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.github.niklasfriberg"
version = "0.2.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Bump to match the IDE you actually run. Reworked terminal is default on 2024.3+.
        intellijIdeaCommunity("2024.3")
        bundledPlugin("org.jetbrains.plugins.terminal")
        instrumentationTools()
    }
}

intellijPlatform {
    // No settings UI to index, and the headless run trips on JDK 25 in this environment.
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }
}

kotlin {
    jvmToolchain(21)
}
