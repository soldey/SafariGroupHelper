import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    java
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("com.gradleup.shadow") version "9.6.0"
    kotlin("jvm") version "2.4.10"
}

val fabricLoaderVersion: String by project
val fabricKotlinVersion: String by project
val javaVersion: String by project
val moulConfigVersion: String by project
val hypixelModApiVersion: String by project
val modVersion: String by project
val mavenGroup: String by project
val archivesBaseName: String by project

/**
 * Everything that differs between the Minecraft versions we build for. The sources are shared;
 * only these coordinates change, so adding a version is one line here - as soon as MoulConfig
 * publishes a build for it, which is what gates 26.3 today.
 */
data class McTarget(
    /** The exact Minecraft build to compile against. */
    val minecraft: String,
    /** The range written into fabric.mod.json, so patch releases of the same line are accepted. */
    val dependency: String,
    val fabricApi: String,
    /** The `modern-<x>` MoulConfig artifact. */
    val moulConfig: String,
    val modMenu: String,
    val hypixelModApiFabric: String,
)

val mcTargets = mapOf(
    "26.1" to McTarget(
        minecraft = "26.1.2",
        dependency = "~26.1",
        fabricApi = "0.155.2+26.1.2",
        moulConfig = "26.1",
        modMenu = "18.0.1",
        hypixelModApiFabric = "1.0.2+build.1+mc26.1",
    ),
    "26.2" to McTarget(
        minecraft = "26.2",
        dependency = "~26.2",
        fabricApi = "0.161.0+26.2",
        moulConfig = "26.2",
        modMenu = "20.0.2",
        // No 26.2 build exists; the 26.1 one is listed for 26.2 as well.
        hypixelModApiFabric = "1.0.2+build.1+mc26.1",
    ),
)

val mcTarget: String = (findProperty("mcTarget") as String?) ?: "26.1"
val target = mcTargets[mcTarget]
    ?: error("Unknown mcTarget '$mcTarget', pick one of ${mcTargets.keys.joinToString()}")

group = mavenGroup
// The Minecraft version is build metadata, which keeps the jars of one release apart.
version = "$modVersion+mc$mcTarget"
base.archivesName.set(archivesBaseName)

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
    withSourcesJar()
}

// Only the compat layer differs between versions; everything else is shared.
sourceSets.main {
    kotlin.srcDir("src/main/kotlin-$mcTarget")
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
    minecraft("com.mojang:minecraft:${target.minecraft}")

    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:${target.fabricApi}")
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")

    // The settings GUI, the same library SkyHanni uses.
    shadowImpl("org.notenoughupdates.moulconfig:modern-${target.moulConfig}:$moulConfigVersion") {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }

    // Optional at runtime: the modmenu entrypoint is only loaded when ModMenu is installed.
    implementation("maven.modrinth:modmenu:${target.modMenu}")

    // Optional at runtime: provided by the hypixel-mod-api mod when the player has it installed.
    compileOnly("net.hypixel:mod-api:$hypixelModApiVersion")
    runtimeOnly("maven.modrinth:hypixel-mod-api:${target.hypixelModApiFabric}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

loom {
    // MoulConfig needs a few private GuiGraphicsExtractor members; without this the settings
    // screen crashes with IllegalAccessError on any setup that has no other mod widening them.
    accessWidenerPath.set(file("src/main/resources/safarigrouphelper.classtweaker"))

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
        "minecraft" to target.dependency,
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
    // LGPL 3.0 is the GPL plus extra permissions, so both texts travel with the jar.
    from("LICENSE") { rename { "LICENSE.txt" } }
    from("COPYING") { rename { "COPYING.txt" } }
    into(layout.buildDirectory.dir("license"))
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
