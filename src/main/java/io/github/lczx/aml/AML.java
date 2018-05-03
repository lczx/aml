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
import io.github.lczx.aml.tunnel.AMLTunnelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class AML {

    private static final Logger LOG = LoggerFactory.getLogger(AML.class);

    private static Class<? extends AMLTunnelService> serviceClass = AMLTunnelService.class;

    private AML() { }

    public static void setServiceClass(final Class<AMLTunnelService> serviceClass) {
        AML.serviceClass = serviceClass;
    }

    public static void startTunnelService(final Context context) {
        startTunnelService(context, null, null);
    }

    public static void startTunnelService(final Context context,
                                          final String[] targetPackageNames, final String[] moduleNames) {
        LOG.debug("Requested service start from {}, targets: {}", context, Arrays.toString(targetPackageNames));
        context.startService(
                new Intent(AMLTunnelService.ACTION_START, null, context, serviceClass)
                        .putExtra(AMLTunnelService.EXTRA_TARGET_PACKAGES, targetPackageNames)
                        .putExtra(AMLTunnelService.EXTRA_MODULE_NAMES, moduleNames));
    }

    public static void stopTunnelService(final Context context) {
        LOG.debug("Requested service stop from {}", context);
        context.startService(
                new Intent(AMLTunnelService.ACTION_STOP, null, context, serviceClass));
    }

    public static boolean isServiceRunning(final Context context) {
        // Since any service class must extend AMLTunnelService, it is no problem referring to it
        return AMLTunnelService.isActive();
    }

    public static void addStatusListener(/* ... */) {
        // TODO: Implement
    }

}
