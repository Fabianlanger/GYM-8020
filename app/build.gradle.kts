plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
}

/**
 * Version name (#88, docs/RELEASE.md). A version build (tag v1.2.3) is "1.2.3"; every other build is the last
 * version plus "-dev (build N)", e.g. "1.2.3-dev (build 140)". The last version is the newest v* tag in git;
 * without tags (shallow clone, no git) it falls back to [fallback], the last version when this was written.
 */
fun versionNameFor(releaseTag: String?, lastTag: String?, build: Int, fallback: String = "0.6.1"): String {
    val semver = Regex("""v(\d+\.\d+\.\d+)""")
    semver.matchEntire(releaseTag?.trim().orEmpty())?.let { return it.groupValues[1] }
    val last = semver.matchEntire(lastTag?.trim().orEmpty())?.groupValues?.get(1) ?: fallback
    return "$last-dev (build $build)"
}

// CI build number, so every build is a real update and visible under "About".
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

// The tag of a version build: RELEASE_TAG from the workflow (a pushed v* tag or the manual run's release input),
// else a pushed tag in GITHUB_REF. Null for dev builds.
val releaseTag: String? = System.getenv("RELEASE_TAG")?.takeIf { it.isNotBlank() }
    ?: System.getenv("GITHUB_REF")?.takeIf { it.startsWith("refs/tags/") }?.removePrefix("refs/tags/")

// The newest version tag reachable from this commit; null when git or the tags are not there.
val lastVersionTag: String? = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--match", "v*", "--abbrev=0")
        isIgnoreExitValue = true
    }.standardOutput.asText.get()
}.getOrNull()

android {
    namespace = "nl.ibs.gym8020"
    compileSdk = 35

    defaultConfig {
        // Own package, apart from IBS (map 1.0, "Naam van de app vóór publiek"); the code namespace stays.
        applicationId = "nl.fabianlanger.gym8020"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = versionNameFor(releaseTag, lastVersionTag, buildNumber)
    }

    signingConfigs {
        // A fixed debug key so every CI build installs over the previous one without losing data.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        // Own key (A-8), only when CI provides it from GitHub Secrets (docs/RELEASE.md): alias gym8020, and the key
        // password is the keystore password. It signs dev builds and versions alike, so they install over each other.
        val releaseStore = System.getenv("RELEASE_KEYSTORE")?.let(::file)?.takeIf { it.exists() }
        if (releaseStore != null) {
            create("release") {
                storeFile = releaseStore
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = "gym8020"
                keyPassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // Room schema history, used by the migration test (Robolectric reads the debug variant's assets).
    sourceSets.getByName("debug").assets.srcDir("$projectDir/schemas")

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// Full stack traces in the CI log when a test fails (artifacts are not readable from every environment).
tasks.withType<Test>().configureEach {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
