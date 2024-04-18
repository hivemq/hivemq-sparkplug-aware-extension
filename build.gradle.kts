import org.gradle.crypto.checksum.Checksum

plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
    alias(libs.plugins.checksum)
}

group = "com.hivemq.extensions.sparkplug.aware"
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

task<Checksum>("checksum") {
    dependsOn("hivemqExtensionZip")
    checksumAlgorithm.set(Checksum.Algorithm.SHA256)
    inputFiles.setFrom(
        files(
            tasks.hivemqExtensionZip.get().outputs.files
        )
    )
    outputDirectory.set(file("${layout.buildDirectory.get()}/hivemq-extension"))
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.tahu)
    implementation(libs.jackson.mapper.asl)
    implementation(libs.jackson)
    implementation(libs.jackson.databind)
    implementation(libs.protobuf)
    implementation(libs.guava)
    implementation(libs.commonsLang)
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        withType<JvmTestSuite> {
            useJUnitJupiter(libs.versions.junit.jupiter)
        }
        "test"(JvmTestSuite::class) {
            dependencies {
                implementation(libs.mockito)
            }
        }
    }
}

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}
