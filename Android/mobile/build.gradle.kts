/*
 * Copyright 2020-2025 Bart Vullings <dev@windkracht8.com>
 * This file is part of RugbyRefereeWatch
 * RugbyRefereeWatch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * RugbyRefereeWatch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.windkracht8.rugbyrefereewatch"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.windkracht8.rugbyrefereewatch"
        minSdk = 26
        targetSdk = 36
        versionCode = 367
        versionName = "m4.0"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-d"
            resValue("string", "app_name", "Rugby Referee Watch (debug)")
        }
        release {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
    }
}
dependencies {
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.animation:animation-graphics-android:1.9.0")
    implementation("androidx.compose.runtime:runtime-android:1.9.0")
    implementation("androidx.compose.material3:material3-android:1.3.2")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("com.garmin.connectiq:ciq-companion-app-sdk:2.3.0@aar")

    //actually only for debug, but release won't compile without it
    implementation("androidx.compose.ui:ui-tooling-preview:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.9.0")
}
