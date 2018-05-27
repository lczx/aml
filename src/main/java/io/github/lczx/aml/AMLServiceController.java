/*
 * Copyright 2018 Luca Zanussi
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

package io.github.lczx.aml;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Utility class to interact with {@link AMLTunnelService}.
 */
public final class AMLServiceController {

    private static final Logger LOG = LoggerFactory.getLogger(AMLServiceController.class);
    private static final Bundle modules = new Bundle();

    private static Class<? extends AMLTunnelService> serviceClass = AMLTunnelService.class;

    private AMLServiceController() { }

    /**
     * Adds a module to the AML session to be started.
     *
     * <p> Modules will be applied to the next invocation of {@link #start(Context)}, duplicates are ignored.
     *
     * @param moduleName The class name of the module to be added
     */
    public static void addModule(final String moduleName) {
        addModule(moduleName, null);
    }

    /**
     * Adds a module with parameters to the AML session to be started.
     *
     * <p> Modules will be applied to the next invocation of {@link #start(Context)}, duplicates are ignored
     * (the latest passed in parameters will be used).
     *
     * @param moduleName The class name of the module to be added
     * @param parameters The module parameters bundle
     */
    public static void addModule(final String moduleName, final Bundle parameters) {
        AMLServiceController.modules.putBundle(moduleName, parameters);
    }

    /**
     * Sets the service class to be used in while sending intents to the service.
     *
     * <p> Use this if you are subclassing of {@link AMLTunnelService}.
     *
     * @param serviceClass The service class to use
     */
    public static void setServiceClass(final Class<AMLTunnelService> serviceClass) {
        AMLServiceController.serviceClass = serviceClass;
    }

    /**
     * Starts the tunnel service.
     *
     * @param context The {@link Context} to use when creating the intent and starting the service
     */
    public static void start(final Context context) {
        start(context, null);
    }

    /**
     * Starts the tunnel service with a list of target application packages.
     *
     * @param context The {@link Context} to use to create the intent and start the service
     * @param targetPackageNames An array of application packages that are allowed to use the VPN
     */
    public static void start(final Context context, final String[] targetPackageNames) {
        LOG.debug("Requested service start from {}, targets: {}", context, Arrays.toString(targetPackageNames));
        context.startService(
                new Intent(AMLTunnelService.ACTION_START, null, context, serviceClass)
                        .putExtra(AMLTunnelService.EXTRA_TARGET_PACKAGES, targetPackageNames)
                        .putExtra(AMLTunnelService.EXTRA_MODULE_PARAMETERS, modules));
    }

    /**
     * Stops the tunnel service.
     *
     * @param context The {@link Context} to use when creating the intent and stopping the service
     */
    public static void stop(final Context context) {
        LOG.debug("Requested service stop from {}", context);
        context.startService(
                new Intent(AMLTunnelService.ACTION_STOP, null, context, serviceClass));
    }

    /**
     * Returns the running status of the service.
     *
     * @return The result of {@link AMLTunnelService#isActive()}
     */
    public static boolean isRunning() {
        return AMLTunnelService.isActive();
    }

}
