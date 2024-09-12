plugins {
    id("org.jetbrains.intellij.platform") version "2.0.1"
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
        //pluginModule(implementation(project("jps-plugin")))
        instrumentationTools()
    }
    implementation("io.jactl:jactl:2.0.1-SNAPSHOT")
    implementation(project(":jps-plugin"))
}
