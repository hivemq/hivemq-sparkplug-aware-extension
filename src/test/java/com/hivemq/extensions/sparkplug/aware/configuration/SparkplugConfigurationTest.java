package com.hivemq.extensions.sparkplug.aware.configuration;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SparkplugConfigurationTest {

    @Test
    void shouldReadConfigurationFromFile() {
        final File extensionFolder = new File("src/hivemq-extension");
        final boolean valid = new SparkplugConfiguration(new File(extensionFolder, "conf")).readPropertiesFromFile();

        assertTrue(valid);
    }
}