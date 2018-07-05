import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.3.4"
    id("kotlin-library")
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

intellij {
    version = "IC-2018.1"
    setPlugins("kotlin", "groovy", "properties", "gradle")
}

dependencies {
    testCompile("junit:junit:4.12")
}
