plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.utf8")
}

group = "com.hivemq.extensions"
description = "HiveMQ Sparkplug Aware Extension"

hivemqExtension {
    name.set("Sparkplug Aware Extension")
    author.set("HiveMQ")
    priority.set(1000)
    startPriority.set(1000)
    mainClass.set("$group.sparkplug.SparkplugAwareMain")
    sdkVersion.set("$version")

    resources {
        from("LICENSE")
    }
}


dependencies {
    implementation("com.google.guava:guava:${property("guava.version")}")
    implementation("org.jetbrains:annotations:${property("jetbrainsAnnotations.version")}")
    implementation("org.apache.commons:commons-lang3:${property("commons-lang3.version")}")
}

/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

/* ******************** integration test ******************** */

dependencies {
    integrationTestImplementation("com.hivemq:hivemq-mqtt-client:${property("hivemq-mqtt-client.version")}")
    integrationTestImplementation("com.hivemq:hivemq-testcontainer-junit5:${property("hivemq-testcontainer.version")}")
    integrationTestRuntimeOnly("ch.qos.logback:logback-classic:${property("logback.version")}")
}

/* ******************** checks ******************** */

license {
    header = rootDir.resolve("HEADER")
    mapping("java", "SLASHSTAR_STYLE")
}

/* ******************** debugging ******************** */

tasks.prepareHivemqHome {
    hivemqHomeDirectory.set(file("/Users/ahelmbre/WorkingGroups/hivemq-4.7.3"))
}

tasks.runHivemqWithExtension {
    environment["HIVEMQ_LOG_LEVEL"] = "DEBUG"
    debugOptions {
        enabled.set(false)
    }
}