import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dusseldorfKtorVersion = "1.4.3.2ff5308"
val ktorVersion = ext.get("ktorVersion").toString()
val mainClass = "no.nav.omsorgsdagermeldingapi.AppKt"
val kafkaEmbeddedEnvVersion = "2.4.0"
val kafkaVersion = "2.4.0" // Alligned med version fra kafka-embedded-env
val junitJupiterVersion = "5.7.0"
val kotlinxCoroutinesVersion = "1.4.2-native-mt"

plugins {
    kotlin("jvm") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

buildscript {
    // Henter ut diverse dependency versjoner, i.e. ktorVersion.
    apply("https://raw.githubusercontent.com/navikt/dusseldorf-ktor/2ff5308c07ac7c0ef8a127ea47fec8707d9f6944/gradle/dusseldorf-ktor.gradle.kts")
}
dependencies {
    // Server
    implementation ( "no.nav.helse:dusseldorf-ktor-core:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-jackson:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-metrics:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-health:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-ktor-auth:$dusseldorfKtorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")

    // Client
    implementation ( "no.nav.helse:dusseldorf-ktor-client:$dusseldorfKtorVersion")
    implementation ( "no.nav.helse:dusseldorf-oauth2-client:$dusseldorfKtorVersion")
    implementation ("io.lettuce:lettuce-core:5.2.1.RELEASE")
    implementation("com.github.fppt:jedis-mock:0.1.16")

    // Test
    testImplementation("no.nav.helse:dusseldorf-test-support:$dusseldorfKtorVersion")
    testImplementation("no.nav:kafka-embedded-env:$kafkaEmbeddedEnvVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:1.10.3-jdk8")
    testImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")


    // kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    testImplementation ("org.skyscreamer:jsonassert:1.5.0")
}

repositories {
    mavenLocal()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/navikt/dusseldorf-ktor")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }

    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("app")
    archiveClassifier.set("")
    manifest {
        attributes(
                mapOf(
                        "Main-Class" to mainClass
                )
        )
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.7"
}

tasks.register("createDependabotFile") {
    doLast {
        mkdir("$projectDir/dependabot")
        val file = File("$projectDir/dependabot/build.gradle")
        file.writeText( "// Do not edit manually! This file was created by the 'createDependabotFile' task defined in the root build.gradle.kts file.\n")
        file.appendText("dependencies {\n")
        project.configurations.getByName("runtimeClasspath").allDependencies
            .filter { it.group != rootProject.name && it.version != null }
            .forEach { file.appendText("    compile '${it.group}:${it.name}:${it.version}'\n") }
        project.configurations.getByName("testRuntimeClasspath").allDependencies
            .filter { it.group != rootProject.name && it.version != null }
            .forEach { file.appendText("    testCompile '${it.group}:${it.name}:${it.version}'\n") }
        file.appendText("}\n")
    }
}
