rootProject.name = "rankquest-cli"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.60.2"
}

refreshVersions {
}
