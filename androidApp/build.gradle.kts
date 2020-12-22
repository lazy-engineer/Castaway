import dependencies.App
import dependencies.Library

plugins {
	id("com.android.application")
	kotlin("android")
	kotlin("android.extensions")
	id("koin")
}

dependencies {
	implementation(project(":shared"))
	implementation(project(":castawayplayer"))

	implementation(Library.viewmodelKtx)
	implementation(Library.activityKtx)
	implementation(Library.fragmentKtx)
	implementation(Library.material)
	implementation(Library.appcompat)
	implementation(Library.constraintlayout)
	implementation(Library.media)
	implementation(Library.koin)
	implementation(Library.koinExt)
	implementation(Library.koinAndroid)
	implementation(Library.koinViewmodel)
}

android {
	compileSdkVersion(App.compileSdk)
	defaultConfig {
		applicationId = "io.github.lazyengineer.castaway.androidApp"
		minSdkVersion(App.minSdk)
		targetSdkVersion(App.targetSdk)
		versionCode = App.versionCode
		versionName = App.versionName
	}

	buildTypes {
		getByName("release") {
			isMinifyEnabled = false
		}
	}

	compileOptions {
		sourceCompatibility(JavaVersion.VERSION_1_8)
		targetCompatibility(JavaVersion.VERSION_1_8)
	}

	kotlinOptions {
		jvmTarget = "1.8"
	}
}