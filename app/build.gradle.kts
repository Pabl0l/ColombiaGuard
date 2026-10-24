plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("org.jetbrains.kotlin.plugin.compose")
	id("com.google.devtools.ksp")
}

android {
	namespace = "com.colombiaguard"
	compileSdk = 36

	defaultConfig {
		applicationId = "com.colombiaguard"
		minSdk = 29
		targetSdk = 36
		versionCode = 2
		versionName = "1.1.0"
	}

	buildTypes {
		release {
			isMinifyEnabled = true
			isShrinkResources = true
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
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
	}

	testOptions {
		unitTests.isIncludeAndroidResources = true
		unitTests.isReturnDefaultValues = true
	}
}

ksp {
	arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
	// Compose BOM
	val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
	implementation(composeBom)

	// Compose
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-graphics")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.activity:activity-compose:1.9.3")
	implementation("androidx.navigation:navigation-compose:2.8.5")
	implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

	// Room
	implementation("androidx.room:room-runtime:2.6.1")
	implementation("androidx.room:room-ktx:2.6.1")
	ksp("androidx.room:room-compiler:2.6.1")

	// Core
	implementation("androidx.core:core-ktx:1.15.0")

	// Coroutines
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

	// Debug
	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")

	// Tests unitarios (JVM)
	testImplementation("junit:junit:4.13.2")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
	testImplementation("org.robolectric:robolectric:4.14.1")
	testImplementation("androidx.test:core:1.6.1")
	testImplementation("androidx.room:room-testing:2.6.1")
}
