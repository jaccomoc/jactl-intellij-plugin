import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
        testFramework(TestFrameworkType.Platform)
    }
    implementation("io.jactl:jactl:2.0.1-SNAPSHOT")
    implementation(project(":jps-plugin"))
    testImplementation("junit:junit:4.13")
    testRuntimeOnly("junit:junit:4.13")

}
