import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Gerçek AdMob kimlikleri yalnız local.properties'te durur, repoya girmez ve yalnız release'e geçer.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { load(it) }
}
val testAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerUnitId = "ca-app-pub-3940256099942544/9214589741"

android {
    namespace = "com.furkanbarissonmezisik.tesbihatpro"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.furkanbarissonmezisik.tesbihatpro"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdMobAppId
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$testBannerUnitId\"")
            buildConfigField(
                "String",
                "UMP_TEST_DEVICE_HASH",
                "\"${localProperties.getProperty("ump.testDeviceHash", "")}\"",
            )
        }
        release {
            manifestPlaceholders["admobAppId"] =
                localProperties.getProperty("admob.appId", testAdMobAppId)
            buildConfigField(
                "String",
                "ADMOB_BANNER_UNIT_ID",
                "\"${localProperties.getProperty("admob.bannerUnitId", testBannerUnitId)}\"",
            )
            buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"\"")
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}