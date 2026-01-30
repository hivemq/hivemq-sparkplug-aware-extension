import org.gradle.crypto.checksum.Checksum

plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.oci)
    alias(libs.plugins.license)
    alias(libs.plugins.checksum)
    alias(libs.plugins.release)
}

group = "com.hivemq.extensions"
description = "HiveMQ Sparkplug Aware Extension"

hivemqExtension {
    name = "Sparkplug Aware Extension"
    author = "HiveMQ"
    priority = 1000
    startPriority = 1000
    sdkVersion = libs.versions.hivemq.extensionSdk

    resources {
        from("LICENSE")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.compileJava {
    javaCompiler = javaToolchains.compilerFor {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.tahu)
    implementation(libs.jackson)
    implementation(libs.jackson.databind)
    implementation(libs.protobuf)

    // tahu-core:1.0.14 pulls in vulnerable transitive dependencies that cannot be updated by upgrading tahu
    // (newer tahu versions require Java 17)
    constraints {
        implementation(libs.commonsLang)
        implementation(libs.logback.core)
        implementation(libs.logback.classic)
    }
}

tasks.register<Checksum>("checksum") {
    checksumAlgorithm = Checksum.Algorithm.SHA256
    inputFiles.from(tasks.hivemqExtensionZip)
    outputDirectory = layout.buildDirectory.dir("hivemq-extension")
}

oci {
    registries {
        dockerHub {
            optionalCredentials()
        }
    }
    imageMapping {
        mapModule("com.hivemq", "hivemq-enterprise") {
            toImage("hivemq/hivemq4")
        }
    }
    imageDefinitions {
        register("main") {
            allPlatforms {
                dependencies {
                    runtime("com.hivemq:hivemq-enterprise:latest") { isChanging = true }
                }
                layer("main") {
                    contents {
                        permissions("opt/hivemq/", 0b111_111_101)
                        permissions("opt/hivemq/extensions/", 0b111_111_101)
                        into("opt/hivemq/extensions") {
                            permissions("*/", 0b111_111_101)
                            permissions("*/conf/sparkplug.properties", 0b110_110_100)
                            permissions("*/hivemq-extension.xml", 0b110_110_100)
                            from(zipTree(tasks.hivemqExtensionZip.flatMap { it.archiveFile }))
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
                implementation(libs.mockito)
            }
        }
        "integrationTest"(JvmTestSuite::class) {
            dependencies {
                compileOnly(libs.jetbrains.annotations)
                implementation(libs.assertj)
                implementation(libs.hivemq.mqttClient)
                implementation(libs.testcontainers)
                implementation(libs.testcontainers.hivemq)
                implementation(libs.testcontainers.junitJupiter)
                implementation(libs.gradleOci.junitJupiter)
                implementation(libs.tahu)
                runtimeOnly(libs.logback.classic)
            }
            oci.of(this) {
                imageDependencies {
                    runtime(project).tag("latest")
                }
            }
        }
    }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}

release {
    buildTasks = listOf("clean", "hivemqExtensionZip", "checksum")
    scmAdapters = listOf(net.researchgate.release.GitAdapter::class.java)
    git {
        requireBranch.set("")
    }
}

// configure reproducible builds
tasks.withType<AbstractArchiveTask>().configureEach {
    // normalize file permissions for reproducibility
    // files: 0644 (rw-r--r--), directories: 0755 (rwxr-xr-x)
    filePermissions {
        unix("0644")
    }
    dirPermissions {
        unix("0755")
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure consistent compilation across different JDK versions
    options.compilerArgs.addAll(listOf(
        // include parameter names for reflection (improves consistency)
        "-parameters"
    ))
}
