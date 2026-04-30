pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FinSave"

// ── Core Modules ───────────────────────────────────────────────
include(":core:common")
include(":core:ui")

// ── Architecture Modules ───────────────────────────────────────
include(":domain")
include(":data")

// ── Feature Modules ────────────────────────────────────────────
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:transactions")
include(":feature:accounts")
include(":feature:categories")
include(":feature:budget")
include(":feature:insights")
include(":feature:splitter")
include(":feature:settings")

// ── Application Module ─────────────────────────────────────────
include(":app")
