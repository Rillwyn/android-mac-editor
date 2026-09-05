import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "io.github.Rillwyn.androidmaceditor"
    compileSdk = 37

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        versionCode = 17
        versionName = "0.2.5"
        applicationId = "io.github.Rillwyn.androidmaceditor"
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    signingConfigs {
        if (localProps.containsKey("storeFile")) {
            create("release") {
                storeFile = file(localProps["storeFile"] as String)
                storePassword = localProps["storePassword"] as String
                keyAlias = localProps["keyAlias"] as String
                keyPassword = localProps["keyPassword"] as String
                storeType = localProps["storeType"] as? String ?: "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles("proguard-rules.pro")
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    // libxposed 模块元数据存放在 META-INF/xposed/ 下，
    // 与 example 一致：仅保留这些条目、丢弃依赖库自带的 META-INF 文件
    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.viewpager2)

    // libxposed Modern Xposed API（API 101/102）
    // compileOnly：模块代码编译期引用 io.github.libxposed.api.*，运行时由框架注入
    compileOnly(libs.libxposed.api)
    // service：模块 App 进程内与 Xposed 框架通信（Remote Preferences / 激活检测 / 作用域）
    implementation(libs.libxposed.service)
}
