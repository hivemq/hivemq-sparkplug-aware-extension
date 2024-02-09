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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.parameter.*;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.intializer.InitializerRegistry;
import com.hivemq.extensions.sparkplug.aware.configuration.SparkplugConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * This is the main class of the extension,
 * which is instantiated either during the HiveMQ start up process (if extension is enabled)
 * or when HiveMQ is already started by enabling the extension.
 *
 * @author Anja Helmbrecht-Schaar
 * @since 4.0.0
 */
public class SparkplugAwareMain implements ExtensionMain {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SparkplugAwareMain.class);
    private @Nullable SparkplugConfiguration configuration;

    @Override
    public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput) {

        try {
            final File extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();
            //read & validate configuration
            if (!configurationValidated(extensionStartOutput, extensionHomeFolder) || configuration == null) {
                return;
            }

            addPublishModifier();

            final ExtensionInformation extensionInformation = extensionStartInput.getExtensionInformation();
            log.info("Started {}:{}", extensionInformation.getName(), extensionInformation.getVersion());

            log.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_TOPIC);
            log.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_DBIRTH_MQTT_RETAIN);
            log.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_TOPIC);
            log.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_NBIRTH_MQTT_RETAIN);
            log.debug("Add Awareness: {} ", AwareRequirements.CONFORMANCE_MQTT_AWARE_NDEATH_TIMESTAMP);


        } catch (final Exception e) {
            log.error("Exception thrown at extension start: ", e);
        }
    }

    @Override
    public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput) {

        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        log.info("Stopped {}:{}", extensionInformation.getName(), extensionInformation.getVersion());
    }

    private void addPublishModifier() {
        final InitializerRegistry initializerRegistry = Services.initializerRegistry();
        final SparkplugPublishInboundInterceptor sparkplugPublishInboundInterceptor =
                new SparkplugPublishInboundInterceptor(configuration, Services.publishService());
        final SparkplugPublishOutboundInterceptor sparkplugPublishOutboundInterceptor =
                new SparkplugPublishOutboundInterceptor(configuration);
        final SparkplugSubscribeInterceptor sparkplugSubscribeInterceptor =
                new SparkplugSubscribeInterceptor(configuration);

        initializerRegistry.setClientInitializer(
                (initializerInput, clientContext) -> {
                    clientContext.addPublishInboundInterceptor(sparkplugPublishInboundInterceptor);
                    clientContext.addPublishOutboundInterceptor(sparkplugPublishOutboundInterceptor);
                    clientContext.addSubscribeInboundInterceptor(sparkplugSubscribeInterceptor);
                });
    }

    private boolean configurationValidated(
            final @NotNull ExtensionStartOutput extensionStartOutput, final @NotNull File extensionHomeFolder) {
        boolean isValid;
        configuration = new SparkplugConfiguration(new File(extensionHomeFolder, "conf"));
        try {
            isValid = configuration.readPropertiesFromFile();
            configuration.getSparkplugVersion();
            configuration.getSparkplugSysTopic();
        } catch (Exception any) {
            isValid = false;
            log.error("Could not read properties", any);
        }

        if (!isValid) {
            extensionStartOutput.preventExtensionStartup("Could not read properties");
        }
        return isValid;
    }
}