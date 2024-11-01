import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("org.jetbrains.intellij.platform.module")
}

java {
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

//intellijPlatform {
//    pluginVerification {
//        ides {
//            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.4")
//            //local(file("/path/to/ide/"))
//            recommended()
//            select {
//                types = listOf(IntelliJPlatformType.IntellijIdeaCommunity)
//                channels = listOf(ProductRelease.Channel.RELEASE)
//                sinceBuild = "232"
//                untilBuild = "241.*"
//            }
//        }
//    }
//}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1.4")
        bundledPlugin("com.intellij.java")
        instrumentationTools()
        implementation("io.jactl:jactl:2.0.1-SNAPSHOT")
        pluginVerifier()
    }
}
