plugins {
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenLocal()
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1.4")
        bundledPlugin("com.intellij.java")
        instrumentationTools()
    }
    implementation("io.jactl:jactl:2.0.1-SNAPSHOT")
}
