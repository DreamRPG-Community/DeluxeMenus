plugins {
    java
    id("com.gradleup.shadow") version("9.6.1")
    id("io.github.ben-manes.versions") version("0.61.0")
}

// Change to true when releasing
val release = true
val majorVersion = "1.14.2"
val minorVersion = if (release) "Paper112" else "DEV-" + System.getenv("BUILD_NUMBER")

group = "com.extendedclip"
version = "$majorVersion-$minorVersion"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.nexomc.com/releases/")
    maven("https://repo.oraxen.com/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-20190714.184133-413")
    compileOnly(libs.vault) { isTransitive = false }
    compileOnly(libs.papi) { isTransitive = false }
    compileOnly("org.jetbrains:annotations:26.1.0")

    implementation(libs.nashorn)
    implementation(libs.bstats)
    implementation("net.kyori:adventure-platform-bukkit:4.4.0")
    implementation("net.kyori:adventure-text-minimessage:4.21.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// These integrations depend on APIs which are absent from the Paper 1.12 build.
sourceSets.main {
    java {
        exclude("com/extendedclip/deluxemenus/hooks/CraftEngineHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/ExecutableBlocksHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/ExecutableItemsHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/HeadDatabaseHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/HeadDBHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/ItemsAdderHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/MMOItemsHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/NexoHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/OraxenHook.java")
        exclude("com/extendedclip/deluxemenus/hooks/SimpleItemGeneratorHook.java")
        exclude("com/extendedclip/deluxemenus/persistentmeta/**")
        exclude("com/extendedclip/deluxemenus/command/subcommand/MetaCommand.java")
        exclude("com/extendedclip/deluxemenus/requirement/HasMetaRequirement.java")
        exclude("com/extendedclip/deluxemenus/dupe/marker/impl/PDCMenuItemMarker.java")
        exclude("com/extendedclip/deluxemenus/utils/RegistryUtils.java")
    }
}

tasks {
    withType<JavaCompile> {
        options.release.set(21)
    }

    shadowJar {
        relocate("org.objectweb.asm", "com.extendedclip.deluxemenus.libs.asm")
        relocate("org.openjdk.nashorn", "com.extendedclip.deluxemenus.libs.nashorn")
        relocate("net.kyori", "com.extendedclip.deluxemenus.libs.adventure")
        relocate("org.bstats", "com.extendedclip.deluxemenus.libs.bstats")
        archiveFileName.set("DeluxeMenus-${rootProject.version}.jar")
    }

    processResources {
        exclude("paper-plugin.yml")
        filesMatching("plugin.yml") {
            expand("version" to rootProject.version)
        }
    }
}
