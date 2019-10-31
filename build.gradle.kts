import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.50"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.kittinunf.fuel:fuel:2.2.1")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.3.50")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0")
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.4.1")
    implementation("org.elasticsearch:elasticsearch:7.4.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
