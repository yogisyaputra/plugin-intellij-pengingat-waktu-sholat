plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "id.ysy-dev"
version = "0.0.1"

repositories { mavenCentral() }

intellij {
    version.set("2024.2") // IntelliJ platform versi stabil
    type.set("IC")        // IC = Community Edition atau "IU" kalau mau target Ultimate
    plugins.set(listOf()) // belum perlu plugin tambahan
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

}


tasks {
    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("")
        changeNotes.set("Initial skeleton.")
    }
    compileKotlin { kotlinOptions.jvmTarget = "17" }
    compileJava {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}
