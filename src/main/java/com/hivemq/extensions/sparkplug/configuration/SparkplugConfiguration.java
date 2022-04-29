/*
 * Copyright 2021-present HiveMQ GmbH
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
package com.hivemq.extensions.sparkplug.configuration;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reads a property file containing influxdb properties
 * and provides some utility methods for working with {@link Properties}.
 *
 * @author Christoph Schäbel
 * @author Anja Helmbrecht-Schaar
 */
public class SparkplugConfiguration extends PropertiesReader {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugConfiguration.class);

    private static final @NotNull String SPARKPLUG_VERSION = "sparkplug.version";
    private static final @NotNull String SPARKPLUG_VERSION_DEFAULT = "spBv1.0";
    private static final @NotNull String SPARKPLUG_SYSTOPIC = "sparkplug.systopic";
    private static final @NotNull String SPARKPLUG_SYSTOPIC_DEFAULT = "$sparkplug/certificates/";

    public SparkplugConfiguration(@NotNull final File configFilePath) {
        super(configFilePath);
    }


    @Override
    public @NotNull String getFilename() {
        return "sparkplug.properties";
    }


    public @NotNull String getSparkplugSysTopic() {
        return validateStringProperty(SPARKPLUG_SYSTOPIC, SPARKPLUG_SYSTOPIC_DEFAULT);
    }

    /**
     * Fetch property with given <b>key</b>. If the fetched {@link String} is <b>null</b> the <b>defaultValue</b> will be returned.
     *
     * @param key          Key of the property.
     * @param defaultValue Default value as fallback, if property has no value.
     * @return the actual value of the property if it is set, else the <b>defaultValue</b>.
     */
    private String validateStringProperty(@NotNull final String key, @NotNull final String defaultValue) {
        checkNotNull(key, "Key to fetch property must not be null");
        checkNotNull(defaultValue, "Default value for property must not be null");
        final String value = getProperty(key);
        if (value == null) {
            if (!defaultValue.isEmpty()) {
                log.warn("No '{}' configured , using default: {}", key, defaultValue);
            }
            return defaultValue;
        }
        return value;
    }

    /**
     * Fetch property with given <b>key</b>.
     * If the fetched {@link String} value is not <b>null</b> convert the value to an int and check validation constraints if given flags are <b>false</b> before returning the value.
     *
     * @param key             Key of the property
     * @param defaultValue    Default value as fallback, if property has no value
     * @param zeroAllowed     use <b>true</b> if property can be zero
     * @param negativeAllowed use <b>true</b> is property can be negative int
     * @return the actual value of the property if it is set and valid, else the <b>defaultValue</b>
     */
    private int validateIntProperty(@NotNull final String key, final int defaultValue, final boolean zeroAllowed, final boolean negativeAllowed) {
        checkNotNull(key, "Key to fetch property must not be null");

        final String value = properties != null ? properties.getProperty(key) : null;
        if (value == null) {
            log.warn("No '{}' configured, using default: {}", key, defaultValue);
            return defaultValue;
        }

        final int valueAsInt;
        try {
            valueAsInt = Integer.parseInt(value);
        } catch (final NumberFormatException e) {
            log.warn("Value for the property '{}' is not a number, original value {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }

        if (!zeroAllowed && valueAsInt == 0) {
            log.warn("Value for the property '{}' can't be zero. Using default: {}", key, defaultValue);
            return defaultValue;
        }

        if (!negativeAllowed && valueAsInt < 0) {
            log.warn("Value for the property '{}' can't be negative. Using default: {}", key, defaultValue);
            return defaultValue;
        }

        return valueAsInt;
    }

    public @NotNull String getSparkplugVersion() {
        return validateStringProperty(SPARKPLUG_VERSION, SPARKPLUG_VERSION_DEFAULT);
    }
}