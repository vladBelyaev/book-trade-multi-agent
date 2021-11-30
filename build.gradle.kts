import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
}

group = "ua.nure.bieliaiev.multiagent"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jade.tilab.com/maven/")
}

dependencies {
    implementation(fileTree(baseDir = "libs"))
}

sourceSets {
    main {
        java.srcDir("src/main/code")
    }
    test {
        java.srcDir("src/test/code")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}
