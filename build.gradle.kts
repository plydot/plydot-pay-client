plugins {
    kotlin("jvm") version "1.9.25"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

import com.vanniktech.maven.publish.SonatypeHost

group = "com.plydot"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:none", "-quiet")
        charSet = "UTF-8"
        encoding = "UTF-8"
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    if (
        project.hasProperty("signing.keyId") ||
        project.hasProperty("signingKey") ||
        project.hasProperty("signingInMemoryKey") ||
        System.getenv("GPG_PRIVATE_KEY") != null
    ) {
        signAllPublications()
    }

    coordinates("com.plydot", "plydot-pay-client", version.toString())

    pom {
        name.set("Plydot Pay Client")
        description.set("Java/Kotlin HTTP client for third-party Plydot Pay integrators")
        inceptionYear.set("2026")
        url.set("https://pay.plydot.dev")

        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }

        developers {
            developer {
                id.set("plydot")
                name.set("Plydot")
                email.set("wilson@plydot.network")
            }
        }

        scm {
            connection.set("scm:git:git@github.com:plydot/plydot-pay-client.git")
            developerConnection.set("scm:git:git@github.com:plydot/plydot-pay-client.git")
            url.set("https://github.com/plydot/plydot-pay-client")
        }
    }
}

