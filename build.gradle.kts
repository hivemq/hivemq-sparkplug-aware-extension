plugins {
    id("com.hivemq.extension")
    id("com.github.hierynomus.license")
    id("com.github.sgtsilvio.gradle.utf8")
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
    implementation("org.eclipse.tahu:tahu-core:${property("tahu.version")}")
    implementation("org.codehaus.jackson:jackson-mapper-asl:${property("mapper.version")}")
    implementation("com.fasterxml.jackson.core:jackson-core:${property("fasterxml.version")}")
    implementation( "com.fasterxml.jackson.core:jackson-databind:${property("fasterxml.version")}")
    implementation("com.google.protobuf:protobuf-java:${property("protobuf.version")}")
    implementation("com.google.guava:guava:${property("guava.version")}")
    implementation("org.jetbrains:annotations:${property("jetbrainsAnnotations.version")}")
    implementation("org.apache.commons:commons-lang3:${property("commons-lang3.version")}")
}

/* ******************** test ******************** */

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${property("junit-jupiter.version")}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${property("junit-jupiter.version")}")
    testImplementation("org.mockito:mockito-core:${property("mockito.version")}")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}


/* ******************** checks ******************** */

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