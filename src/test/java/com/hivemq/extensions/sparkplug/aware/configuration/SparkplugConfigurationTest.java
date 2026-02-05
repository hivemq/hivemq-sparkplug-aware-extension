/*
 * Copyright 2018-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.extensions.sparkplug.aware.configuration;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SparkplugConfigurationTest {

    @TempDir
    private @NotNull Path tempDir;

    @Test
    void shouldReadConfigurationFromFile() {
        final var extensionFolder = new File("src/hivemq-extension");
        final var configuration = new SparkplugConfiguration(new File(extensionFolder, "conf"), "config.properties");
        assertThat(configuration.readPropertiesFromFile()).isTrue();
    }

    @Test
    void shouldReturnDefaultSparkplugVersion() throws Exception {
        final var configuration = createConfiguration(List.of());
        assertThat(configuration.getSparkplugVersion()).isEqualTo("spBv1.0");
    }

    @Test
    void shouldReturnConfiguredSparkplugVersion() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.version=spBv2.0"));
        assertThat(configuration.getSparkplugVersion()).isEqualTo("spBv2.0");
    }

    @Test
    void shouldReturnDefaultSysTopic() throws Exception {
        final var configuration = createConfiguration(List.of());
        assertThat(configuration.getSparkplugSysTopic()).isEqualTo("$sparkplug/certificates/");
    }

    @Test
    void shouldReturnConfiguredSysTopic() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.systopic=$custom/systopic/"));
        assertThat(configuration.getSparkplugSysTopic()).isEqualTo("$custom/systopic/");
    }

    @Test
    void shouldReturnDefaultCompression() throws Exception {
        final var configuration = createConfiguration(List.of());
        assertThat(configuration.getCompression()).isFalse();
    }

    @Test
    void shouldReturnCompressionEnabled() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.compression=true"));
        assertThat(configuration.getCompression()).isTrue();
    }

    @Test
    void shouldReturnCompressionDisabled() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.compression=false"));
        assertThat(configuration.getCompression()).isFalse();
    }

    @Test
    void shouldReturnDefaultJsonLogEnabled() throws Exception {
        final var configuration = createConfiguration(List.of());
        assertThat(configuration.getJsonLogEnabled()).isFalse();
    }

    @Test
    void shouldReturnJsonLogEnabled() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.json.log=true"));
        assertThat(configuration.getJsonLogEnabled()).isTrue();
    }

    @Test
    void shouldReturnJsonLogDisabled() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.json.log=false"));
        assertThat(configuration.getJsonLogEnabled()).isFalse();
    }

    @Test
    void shouldReturnDefaultMessageExpiry() throws Exception {
        final var configuration = createConfiguration(List.of());
        assertThat(configuration.getSparkplugSystopicMsgexpiry()).isEqualTo(4294967296L);
    }

    @Test
    void shouldReturnConfiguredMessageExpiry() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.systopic.msgExpiry=3600"));
        assertThat(configuration.getSparkplugSystopicMsgexpiry()).isEqualTo(3600L);
    }

    @Test
    void shouldReturnDefaultMessageExpiryForInvalidValue() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.systopic.msgExpiry=notAnumber"));
        assertThat(configuration.getSparkplugSystopicMsgexpiry()).isEqualTo(4294967296L);
    }

    @Test
    void shouldReturnDefaultMessageExpiryForNegativeValue() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.systopic.msgExpiry=-100"));
        assertThat(configuration.getSparkplugSystopicMsgexpiry()).isEqualTo(4294967296L);
    }

    @Test
    void shouldReturnZeroMessageExpiry() throws Exception {
        // zero is allowed for message expiry
        final var configuration = createConfiguration(List.of("sparkplug.systopic.msgExpiry=0"));
        assertThat(configuration.getSparkplugSystopicMsgexpiry()).isEqualTo(0L);
    }

    @Test
    void shouldReturnFilename() throws Exception {
        final var configuration = createConfiguration(List.of());
        assertThat(configuration.getFilename()).isEqualTo("config.properties");
    }

    @Test
    void shouldReturnFalseForNonExistentConfigFile() {
        final var nonExistentDir = new File("/non/existent/path");
        final var configuration = new SparkplugConfiguration(nonExistentDir, "config.properties");
        assertThat(configuration.readPropertiesFromFile()).isFalse();
    }

    @Test
    void shouldHandleMultipleProperties() throws Exception {
        final var configuration = createConfiguration(List.of("sparkplug.version=spBv2.0",
                "sparkplug.systopic=$custom/path/",
                "sparkplug.compression=true",
                "sparkplug.json.log=true",
                "sparkplug.systopic.msgExpiry=7200"));

        assertThat(configuration.getSparkplugVersion()).isEqualTo("spBv2.0");
        assertThat(configuration.getSparkplugSysTopic()).isEqualTo("$custom/path/");
        assertThat(configuration.getCompression()).isTrue();
        assertThat(configuration.getJsonLogEnabled()).isTrue();
        assertThat(configuration.getSparkplugSystopicMsgexpiry()).isEqualTo(7200L);
    }

    private @NotNull SparkplugConfiguration createConfiguration(final @NotNull List<String> properties)
            throws Exception {
        final var file = tempDir.resolve("config.properties");
        Files.write(file, properties);
        final var configuration = new SparkplugConfiguration(tempDir.toFile(), "config.properties");
        configuration.readPropertiesFromFile();
        return configuration;
    }
}
