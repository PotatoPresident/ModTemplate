plugins {
    id("fabric-loom") version "0.10.+"
    id("org.ajoberstar.grgit") version "4.1.1"
    id("maven-publish")
}

val modId: String by project
val modName: String by project
val modVersion: String by project
val mavenGroup: String by project

base.archivesName.set(modId)
// Minecraft version has space after for some reason
version = "$modVersion-${libs.versions.minecraft.get()}${getVersionMetadata()}".replace(" ", "")
group = mavenGroup

repositories {

}

dependencies {
    // To change the versions see the libs.versions.toml

    // Fabric
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn.mappings) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Fabric API
    modImplementation(libs.fabric.api)
}

tasks {
    processResources {
        inputs.property("id", modId)
        inputs.property("name", modName)
        inputs.property("version", modVersion)

        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to modId,
                    "version" to modVersion,
                    "name" to modName,
                    "fabricApi" to libs.versions.fabric.api.get(),
                )
            )
        }
    }

    jar {
        from("LICENSE")
    }

    java {
        withSourcesJar()
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // add all the jars that should be included when publishing to maven
            artifact(tasks.jar) {
                builtBy(tasks.remapJar)
            }
            artifact(tasks.getByName("sourcesJar")) {
                builtBy(tasks.remapSourcesJar)
            }
        }
    }

    // select the repositories you want to publish to
    repositories {
        // mavenLocal()
    }
}

fun getVersionMetadata(): String {
    if (System.getenv("GITHUB_WORKFLOW") == "Release") return ""

    val buildId = System.getenv("GITHUB_RUN_NUMBER")

    // CI builds only
    if (buildId != null) {
        return "+build.$buildId"
    }

    try {
        val grgit = org.ajoberstar.grgit.Grgit.open()

        if (grgit != null) {
            val head = grgit.head()
            var id = head.abbreviatedId

            // Flag the build if the build tree is not clean
            if (!grgit.status().isClean) {
                id += "-dirty"
            }

            return "+rev.$id"
        }
    } catch (e: java.lang.IllegalStateException) {
        // No git repo
    }

    // No tracking information could be found about the build
    return "+unknown"
}
