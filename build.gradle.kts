import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("java")
}

version = "1.1.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8;
    targetCompatibility = JavaVersion.VERSION_1_8;
}

repositories {
    mavenLocal()
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

//tasks.withType(Test) { testLogging.showStandardStreams = true }
tasks.test {
    testLogging {
        showStandardStreams = true
    }
}


dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1.4")
        bundledPlugin("com.intellij.java")
        instrumentationTools()
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }
    implementation("io.jactl:jactl:2.2.0-SNAPSHOT")
    implementation(project(":jps-plugin"))
    testImplementation("junit:junit:4.13.1")
    testRuntimeOnly("junit:junit:4.13")
}

intellijPlatform {
    pluginConfiguration {
        id = "io.jactl.intellij-jactl-plugin"
        name="Jactl"
        version="1.1.0-SNAPSHOT"
        ideaVersion {
            sinceBuild = "241"
            untilBuild = provider { null }
        }
        vendor {
            name  = "Jactl"
            email = "jactl.lang@gmail.com"
            url   = "https://jactl.io"
        }

    }
//    pluginVerification {
//        ides {
//            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.4")
//            //local(file("/path/to/ide/"))
//            recommended()
//            select {
//                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
//                channels = listOf(ProductRelease.Channel.RELEASE)
//                sinceBuild = "241"
//                //untilBuild = "242.*"
//            }
//        }
//    }
}

