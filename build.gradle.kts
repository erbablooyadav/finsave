// ─────────────────────────────────────────────────────────────────────────────
// FinSave — Root Build Configuration
// All plugin versions are managed via gradle/libs.versions.toml
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
