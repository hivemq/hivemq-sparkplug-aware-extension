plugins {
    alias(libs.plugins.hivemq.extension)
    alias(libs.plugins.defaults)
    alias(libs.plugins.license)
}

group = "com.hivemq.extensions.sparkplug.aware"
description = "HiveMQ Sparkplug Aware Extension"

hivemqExtension {
    name.set("Sparkplug Aware Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(1000)
    mainClass.set("$group.SparkplugAwareMain")
    sdkVersion.set("$version")

    resources {
        from("LICENSE")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(libs.tahu)
    implementation(libs.jackson.mapper.asl)
    implementation(libs.jackson)
    implementation(libs.jackson.databind)
    implementation(libs.protobuf)
    implementation(libs.guava)
    implementation(libs.jetbrains.annotations)
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

/* ******************** debugging ******************** */

tasks.prepareHivemqHome {
    hivemqHomeDirectory.set(file("/your/path/to/hivemq-4.X.X"))
}

tasks.runHivemqWithExtension {
    environment["HIVEMQ_LOG_LEVEL"] = "INFO"
    debugOptions {
        enabled.set(false)
    }
}