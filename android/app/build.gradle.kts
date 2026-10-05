plugins { id("com.android.application") }
android {
    namespace = "jp.personal.naspass"
    compileSdk = 35
    defaultConfig {
        applicationId = "jp.personal.naspass"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "jp.personal.naspass.DeviceTestRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
}
