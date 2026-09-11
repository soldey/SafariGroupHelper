import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    java
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("com.gradleup.shadow") version "9.6.0"
    kotlin("jvm") version "2.4.10"
}

val minecraftVersion: String by project
val minecraftDependency: String by project
val fabricLoaderVersion: String by project
val fabricApiVersion: String by project
val fabricKotlinVersion: String by project
val javaVersion: String by project
val moulConfigVersion: String by project
val moulConfigMinecraftVersion: String by project
val modMenuVersion: String by project
val hypixelModApiVersion: String by project
val hypixelModApiFabricVersion: String by project
val modVersion: String by project
val mavenGroup: String by project
val archivesBaseName: String by project

group = mavenGroup
version = modVersion
base.archivesName.set(archivesBaseName)

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net") {
        content { includeGroupAndSubgroups("net.fabricmc") }
    }
    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }
    maven("https://repo.hypixel.net/repository/Hypixel") {
        content { includeGroup("net.hypixel") }
    }
    maven("https://maven.notenoughupdates.org/releases") {
        content { includeGroupAndSubgroups("org.notenoughupdates") }
    }
}

/** Bundled into the final jar and relocated, so it cannot clash with other mods' copies. */
val shadowImpl: Configuration = configurations.create("shadowImpl") {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")

    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    // The settings GUI, the same library SkyHanni uses.
    shadowImpl("org.notenoughupdates.moulconfig:modern-$moulConfigMinecraftVersion:$moulConfigVersion") {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }

    // Optional at runtime: the modmenu entrypoint is only loaded when ModMenu is installed.
    implementation("maven.modrinth:modmenu:$modMenuVersion")

    // Optional at runtime: provided by the hypixel-mod-api mod when the player has it installed.
    compileOnly("net.hypixel:mod-api:$hypixelModApiVersion")
    runtimeOnly("maven.modrinth:hypixel-mod-api:$hypixelModApiFabricVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

loom {
    runs {
        named("client") {
            isIdeConfigGenerated = true
            vmArgs("-Xmx2G")
        }
        removeIf { it.name == "server" }
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "minecraft" to minecraftDependency,
        "fabricLoader" to fabricLoaderVersion,
        "fabricKotlin" to fabricKotlinVersion,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(javaVersion))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(javaVersion.toInt())
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_$archivesBaseName" }
    }
}

// Copied under a different name so the `exclude("/LICENSE")` below, which drops MoulConfig's
// copy, does not also drop ours - Gradle matches excludes against the source name.
val licenseForJar = tasks.register<Copy>("licenseForJar") {
    from("LICENSE")
    into(layout.buildDirectory.dir("license"))
    rename { "LICENSE.txt" }
}

// The shadowed jar is the real mod jar; the plain one has no bundled dependencies.
tasks.shadowJar {
    archiveClassifier.set("")
    configurations = listOf(shadowImpl)
    exclude("META-INF/versions/**")
    exclude("META-INF/*.kotlin_module")
    // MoulConfig puts its own licence at the root; ours belongs there instead, and its copy is
    // kept under licenses/ so the jar states clearly what is bundled and under what terms.
    exclude("/LICENSE")
    mergeServiceFiles()
    relocate("io.github.notenoughupdates.moulconfig", "me.kmsold.safarigrouphelper.deps.moulconfig")
    from(licenseForJar)
    from("licenses") { into("licenses") }
}

tasks.jar {
    archiveClassifier.set("nodeps")
    destinationDirectory.set(layout.buildDirectory.dir("nodeps"))
}

tasks.assemble { dependsOn(tasks.shadowJar) }
