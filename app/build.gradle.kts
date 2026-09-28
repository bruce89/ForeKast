plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.bruze.forekast"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.bruze.forekast"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "0.4.0"
        buildConfigField("boolean", "DEMO", providers.gradleProperty("forekastDemo").orElse("false").get())
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isReturnDefaultValues = true }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

android.sourceSets.getByName("androidTest").assets.directories.add("src/test/resources")

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.ktor.mock)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)
    implementation(libs.ktor.core)
    implementation(libs.ktor.okhttp)
    implementation(libs.ktor.content)
    implementation(libs.ktor.json)
    testImplementation(libs.ktor.mock)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.compose.test.accessibility)
    androidTestImplementation(libs.android.junit)
    androidTestImplementation(libs.android.runner)
    androidTestImplementation(libs.espresso.core)
}

// Optional, explicit local CA for a debug build behind a trusted development proxy.
// Nothing is bundled or changed unless this property is supplied; release never uses it.
providers.gradleProperty("forekastLocalCa").orNull?.let { certificatePath ->
    val certificate = rootProject.file(certificatePath)
    val generated = layout.buildDirectory.dir("generated/localCaRes")
    val generateLocalCa = tasks.register("generateLocalDebugCa") {
        inputs.file(certificate)
        outputs.dir(generated)
        doLast {
            val directory = generated.get().asFile
            directory.resolve("raw").mkdirs()
            directory.resolve("xml").mkdirs()
            certificate.copyTo(directory.resolve("raw/local_debug_ca.cer"), overwrite = true)
            directory.resolve("xml/network_security_config.xml").writeText("""
                <network-security-config>
                    <base-config cleartextTrafficPermitted="false">
                        <trust-anchors><certificates src="system" /></trust-anchors>
                    </base-config>
                    <debug-overrides>
                        <trust-anchors><certificates src="@raw/local_debug_ca" /></trust-anchors>
                    </debug-overrides>
                </network-security-config>
            """.trimIndent())
        }
    }
    android.sourceSets.getByName("debug").res.directories.add(generated.get().asFile.absolutePath)
    tasks.matching { it.name == "preDebugBuild" }.configureEach { dependsOn(generateLocalCa) }
}
