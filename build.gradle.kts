import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val verKotlin = "1.5.21"
val verVertx = "4.1.2"
val verJunitJupiter = "5.7.1"
val verAsyncHttp = "2.11.0"

plugins {
    kotlin("jvm").version("1.5.21")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.vertx:vertx-core:$verVertx")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$verKotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$verKotlin")

    implementation("org.asynchttpclient:async-http-client:$verAsyncHttp")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$verJunitJupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$verJunitJupiter")
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}

tasks.withType<KotlinCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()

    version = "0.0.1"

    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.5"
        languageVersion = "1.5"
    }
}
