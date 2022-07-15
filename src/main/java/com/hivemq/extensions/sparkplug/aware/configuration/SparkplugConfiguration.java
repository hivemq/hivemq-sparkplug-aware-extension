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
 * @author Anja Helmbrecht-Schaar
 */
public class SparkplugConfiguration extends PropertiesReader {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugConfiguration.class);

    private static final @NotNull String SPARKPLUG_VERSION = "sparkplug.version";
    private static final @NotNull String SPARKPLUG_VERSION_DEFAULT = "spBv1.0";
    private static final @NotNull String SPARKPLUG_SYSTOPIC = "sparkplug.systopic";
    private static final @NotNull String SPARKPLUG_SYSTOPIC_DEFAULT = "$sparkplug/certificates/";

    private static final @NotNull String SPARKPLUG_COMPRESSION = "sparkplug.compression";
    private static final @NotNull String SPARKPLUG_COMPRESSION_DEFAULT = "false";

    private static final @NotNull String SPARKPLUG_JSON_LOG_ENABLED = "sparkplug.json.log";
    private static final @NotNull String SPARKPLUG_JSON_LOG_DEFAULT = "false";


    private static final @NotNull String SPARKPLUG_SYSTOPIC_MSGEXPIRY = "sparkplug.systopic.msgExpiry";
    private static final @NotNull Long SPARKPLUG_SYSTOPIC_MSGEXPIRY_DEFAULT = Long.MAX_VALUE;

    public SparkplugConfiguration(@NotNull final File configFilePath) {
        super(configFilePath);
    }
    public @NotNull Long getSparkplugSystopicMsgexpiry() {
        return validateLongProperty(SPARKPLUG_SYSTOPIC_MSGEXPIRY, SPARKPLUG_SYSTOPIC_MSGEXPIRY_DEFAULT, true, false);
    }

    @Override
    public @NotNull String getFilename() {
        return "sparkplug.properties";
    }


    public @NotNull String getSparkplugSysTopic() {
        return validateStringProperty(SPARKPLUG_SYSTOPIC, SPARKPLUG_SYSTOPIC_DEFAULT);
    }

    public @NotNull Boolean getCompression() {
        return validateBooleanProperty(SPARKPLUG_COMPRESSION, SPARKPLUG_COMPRESSION_DEFAULT);
    }

    public @NotNull Boolean getJsonLogEnabled() {
        return validateBooleanProperty(SPARKPLUG_JSON_LOG_ENABLED, SPARKPLUG_JSON_LOG_DEFAULT);
    }

    private Boolean validateBooleanProperty(String key, String defaultValue) {
        checkNotNull(key, "Key to fetch property must not be null");
        checkNotNull(defaultValue, "Default value for property must not be null");
        final String value = getProperty(key);
        if (value == null) {
            if (!defaultValue.isEmpty()) {
                log.warn("No '{}' configured , using default: {}", key, defaultValue);
            }
            return Boolean.parseBoolean(defaultValue);
        }
        return Boolean.parseBoolean(value);
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
    private long validateLongProperty(@NotNull final String key, final long defaultValue, final boolean zeroAllowed, final boolean negativeAllowed) {
        checkNotNull(key, "Key to fetch property must not be null");

        final String value = properties != null ? properties.getProperty(key) : null;
        if (value == null) {
            log.warn("No '{}' configured, using default: {}", key, defaultValue);
            return defaultValue;
        }

        final long valueAsLong;
        try {
            valueAsLong = Long.parseLong(value);
        } catch (final NumberFormatException e) {
            log.warn("Value for the property '{}' is not a number, original value {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }

        if (!zeroAllowed && valueAsLong == 0) {
            log.warn("Value for the property '{}' can't be zero. Using default: {}", key, defaultValue);
            return defaultValue;
        }

        if (!negativeAllowed && valueAsLong < 0) {
            log.warn("Value for the property '{}' can't be negative. Using default: {}", key, defaultValue);
            return defaultValue;
        }

        return valueAsLong;
    }

    public @NotNull String getSparkplugVersion() {
        return validateStringProperty(SPARKPLUG_VERSION, SPARKPLUG_VERSION_DEFAULT);
    }
}