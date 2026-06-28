// minecraft-plugin-healthbar — floating health bar above damaged entities.
// Depends on core as `compileOnly`: core is a separate plugin on the server,
// so we never bundle it. This plugin only uses core for the shared config dir
// (EcosystemData) and logging — it touches no database or shared service.
// The shaded jar only relocates third-party libs (none yet).

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":minecraft-plugin-core"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    // relocate("com.example.shadedlib", "com.mrfermz.mcplugins.healthbar.libs.shadedlib")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
