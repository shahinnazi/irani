plugins {
    alias(libs.plugins.com.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("io.github.persiancalendar.appbuildplugin")
    id("com.google.gms.google-services")
}


android {
    packaging {
        resources {
            pickFirsts += setOf("/META-INF/versions/9/OSGI-INF/MANIFEST.MF")
            pickFirsts += setOf("org/bouncycastle/x509/CertPathReviewerMessages.properties")
            pickFirsts += setOf("org/bouncycastle/x509/CertPathReviewerMessages_de.properties")
        }
    }
    sourceSets {
        val buildDir = layout.buildDirectory.get().asFile
        val generatedAppSrcDir = buildDir.resolve("generated/source/appsrc/main")

        getByName("main") {
            kotlin.srcDir(generatedAppSrcDir)
        }
        getByName("release") {
            kotlin.srcDir("src/debug/kotlin")
        }
        getByName("debug") {
            kotlin.srcDir("src/debug/kotlin")
        }
    }

    compileSdk = 36

    buildFeatures {
        buildConfig = true
        compose = true
    }

    val gitInfo = providers.of(io.github.persiancalendar.gradle.GitInfoValueSource::class) {}.get()

    namespace = "com.shahin.iran"

    defaultConfig {
        ndk {
            abiFilters.addAll(listOf(
                "armeabi-v7a",
                "arm64-v8a",

                ))
        }
        applicationId = "com.shahin.iran"
        minSdk = 24
        targetSdk = 36
        versionCode = 1111
        versionName = "1111"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // It lowers the APK size and prevents crash in AboutScreen in API 21-23
        vectorDrawables.useSupportLibrary = true
        androidResources.localeFilters += listOf(
            "en", "fa", "ckb", "ar", "ur", "ps", "glk", "azb", "ja", "fr", "es", "tr", "kmr", "tg",
            "ne", "zh-rCN", "ru", "pt", "it", "ta", "de",
        )
        setProperty("archivesBaseName", "PersianCalendar-$versionName-$gitInfo")
    }

    signingConfigs {
        create("nightly") {
            storeFile = rootProject.file("nightly.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    testOptions.unitTests.all { it.useJUnitPlatform() }

    buildTypes {
        create("nightly") {
            signingConfig = signingConfigs.getByName("nightly")
            versionNameSuffix = "-${defaultConfig.versionName}-$gitInfo-nightly"
            applicationIdSuffix = ".nightly"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "DEVELOPMENT", "true")
        }

        getByName("debug") {
            versionNameSuffix = "-${defaultConfig.versionName}-$gitInfo"
            buildConfigField("boolean", "DEVELOPMENT", "true")
            applicationIdSuffix = ".debug"
        }

        getByName("release") {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("boolean", "DEVELOPMENT", "false")
            signingConfig = signingConfigs.getByName("nightly")
        }
    }
    flavorDimensions += listOf("api")

    packaging {
        resources.excludes += "DebugProbesKt.bin"
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    bundle {
        // We have in app locale change and don't want Google Play's dependency so better to disable
        language.enableSplit = false
    }

    val javaVersion = JavaVersion.VERSION_21

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions { jvmTarget = javaVersion.majorVersion }

    lint { disable += listOf("MissingTranslation") }
}

dependencies {
    // Project owned libraries
    implementation(libs.persiancalendar.calendar)
    implementation(libs.persiancalendar.praytimes)
    implementation(libs.persiancalendar.calculator)
    implementation(libs.persiancalendar.qr)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20231013")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    // https://github.com/cosinekitty/astronomy/releases/tag/v2.1.0
    implementation(libs.astronomy)

    // Google/JetBrains owned libraries (roughly platform libraries)
    implementation(libs.dynamicanimation)
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)
    implementation(libs.browser)
    implementation(libs.work.manager.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidbrowserhelper)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotlinx.html.jvm)
    implementation(libs.openlocationcode)
    implementation(libs.activity.ktx)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.github.ptrbrynt.KotlinBloc:core:3.0.1")
    implementation ("org.greenrobot:eventbus:3.2.0")
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material.icons.extended)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
//    implementation(libs.androidx.app.projected)
//    implementation(libs.androidx.app)
    debugImplementation(libs.compose.ui.tooling)
    implementation("androidx.lifecycle:lifecycle-service:2.6.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")




    testImplementation(libs.junit)

    testImplementation(kotlin("test"))

    testImplementation(libs.junit.platform.runner)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.bundles.mockito)

    testImplementation(libs.truth)

    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
}

tasks.named("preBuild").configure { dependsOn(getTasksByName("codegenerators", false)) }
