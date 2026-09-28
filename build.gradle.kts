plugins {
    // Android plugins are applied in :app only, so `-PcoreOnly` works without Google Maven.
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
