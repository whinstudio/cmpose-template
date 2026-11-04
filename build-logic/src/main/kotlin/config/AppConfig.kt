package config

object AppConfig {
    const val applicationId = "com.kturker.multiplatform"
    const val appName = "Multiplatform"

    const val compileSdkVersion = 37
    const val minSdkVersion = 30

    const val targetSdkVersion = 37

    const val versionCode = 1

    private const val versionMajor = 0
    private const val versionMinor = 0
    private const val versionPatch = 1

    val versionName
        get() = "$versionMajor.$versionMinor.$versionPatch"
}