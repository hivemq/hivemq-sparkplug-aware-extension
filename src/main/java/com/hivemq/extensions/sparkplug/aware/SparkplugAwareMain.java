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
package com.hivemq.extensions.sparkplug.aware;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.sparkplug.aware.configuration.ConfigResolver;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the HiveMQ Sparkplug Aware Extension.
 * <p>
 * This extension implements Sparkplug awareness by intercepting and modifying MQTT messages
 * to ensure compliance with Sparkplug specification requirements. The extension is instantiated
 * either during the HiveMQ startup process (if enabled) or dynamically when enabled on a running
 * HiveMQ broker.
 * <p>
 * Key functionality includes:
 * <ul>
 *     <li>Forwarding NBIRTH and DBIRTH messages to system topics with retained flag</li>
 *     <li>Updating timestamps in NDEATH messages to reflect actual disconnection time</li>
 *     <li>Preserving retained flag behavior for Sparkplug system topic subscriptions</li>
 * </ul>
 *
 * @author David Sondermann
 * @since 4.0.0
 */
public class SparkplugAwareMain implements ExtensionMain {

    private static final @NotNull String CONFIG_PATH = "conf/config.properties";
    private static final @NotNull String LEGACY_CONFIG_PATH = "conf/sparkplug.properties";

    private static final @NotNull Logger LOG = LoggerFactory.getLogger(SparkplugAwareMain.class);

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {
        try {
            // read & validate configuration
            final var extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();
            final var configResolver = new ConfigResolver(extensionHomeFolder.toPath(),
                    "Sparkplug Aware Extension",
                    CONFIG_PATH,
                    LEGACY_CONFIG_PATH);
            final var resolvedConfigFile = configResolver.get().toFile();
            final var configuration =
                    new SparkplugConfiguration(resolvedConfigFile.getParentFile(), resolvedConfigFile.getName());
            if (!configurationValidated(configuration)) {
                extensionStartOutput.preventExtensionStartup("Could not read properties");
                return;
            }

            addPublishModifier(configuration);

            final var extensionInformation = extensionStartInput.getExtensionInformation();
            LOG.info("Started {}:{}", extensionInformation.getName(), extensionInformation.getVersion());

            LOG.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_TOPIC);
            LOG.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_RETAIN);
            LOG.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_TOPIC);
            LOG.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_RETAIN);
            LOG.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_NDEATH_TIMESTAMP);
        } catch (final Exception e) {
            LOG.error("Exception thrown at extension start: ", e);
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {
        final var extensionInformation = extensionStopInput.getExtensionInformation();
        LOG.info("Stopped {}:{}", extensionInformation.getName(), extensionInformation.getVersion());
    }

    private boolean configurationValidated(final @NotNull SparkplugConfiguration configuration) {
        try {
            final var isValid = configuration.readPropertiesFromFile();
            configuration.getSparkplugVersion();
            configuration.getSparkplugSysTopic();
            return isValid;
        } catch (final Exception any) {
            LOG.error("Could not read properties", any);
            return false;
        }
    }

    private void addPublishModifier(final @NotNull SparkplugConfiguration configuration) {
        final var initializerRegistry = Services.initializerRegistry();
        final var sparkplugPublishInboundInterceptor =
                new SparkplugPublishInboundInterceptor(configuration, Services.publishService());
        final var sparkplugPublishOutboundInterceptor = new SparkplugPublishOutboundInterceptor(configuration);
        final var sparkplugSubscribeInterceptor = new SparkplugSubscribeInterceptor(configuration);

        initializerRegistry.setClientInitializer((initializerInput, clientContext) -> {
            clientContext.addPublishInboundInterceptor(sparkplugPublishInboundInterceptor);
            clientContext.addPublishOutboundInterceptor(sparkplugPublishOutboundInterceptor);
            clientContext.addSubscribeInboundInterceptor(sparkplugSubscribeInterceptor);
        });
    }
}
