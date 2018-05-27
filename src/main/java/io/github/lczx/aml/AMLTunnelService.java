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

import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import io.github.lczx.aml.hook.ReflectiveModuleLoader;
import io.github.lczx.aml.tunnel.AMLTunnel;
import io.github.lczx.aml.tunnel.SocketProtector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Reference AML's {@link VpnService}.
 *
 * <p> Supports actions to start and stop the service plus binding for system monitoring. In addition,
 * {@link AMLServiceController} can be used as an helper to construct properly formatted intents to interact
 * with this service.
 */
public class AMLTunnelService extends VpnService {

    public static final String ACTION_START = "aml.tunnel.intent.action.SERVICE_START";
    public static final String ACTION_STOP = "aml.tunnel.intent.action.SERVICE_STOP";
    public static final String ACTION_BIND_MONITORING = "aml.tunnel.intent.action.BIND_MONITORING";
    public static final String EXTRA_TARGET_PACKAGES = "aml.tunnel.intent.extra.TARGET_NAMES";
    public static final String EXTRA_MODULE_PARAMETERS = "aml.tunnel.intent.extra.MODULES";

    private static final Logger LOG = LoggerFactory.getLogger(AMLTunnelService.class);
    private static final String SESSION_NAME = "firewall";

    private static boolean isActive = false;

    private final AMLTunnel amlTunnel = new AMLTunnel();

    /**
     * {@code true} if an instance of this service has been created.
     *
     * <p> This condition yields true even when  the tunnel itself is not started with an explicit intent, the
     * service is bound for monitoring with {@link android.content.Context#BIND_AUTO_CREATE Context.BIND_AUTO_CREATE}
     * or the engine failed in an unexpected way.
     */
    public static boolean isActive() {
        return isActive;
    }

    @Override
    public void onCreate() {
        LOG.debug("Service created");
        super.onCreate();
        isActive = true;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        // We could use onCreate/onDestroy to initialize/stop the VPN but then we wouldn't have access to intent params
        if (intent == null || intent.getAction() == null) {
            LOG.warn("Service started without an action parameter");
            return START_NOT_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_START:
                startVPN(intent.getStringArrayExtra(EXTRA_TARGET_PACKAGES),
                        intent.getBundleExtra(EXTRA_MODULE_PARAMETERS));
                break;

            case ACTION_STOP:
                // stopService()/stopSelf() doesn't work and onDestroy() does not get called, the only way to stop
                // the tunnel is to close the tunnel device, then we can call stopSelf() and terminate
                amlTunnel.stopSystem();
                stopSelf();
                break;

            default:
                LOG.warn("Service received an unknown action command");
                return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        LOG.debug("Binding intent received: {}", intent);
        if (intent != null && ACTION_BIND_MONITORING.equals(intent.getAction())) {
            LOG.info("Requested AMLContext binding for monitoring");
            return new ContextBinding();
        }
        return super.onBind(intent);
    }

    @Override
    public void onRevoke() {
        LOG.info("Service revoked by system");
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        amlTunnel.stopSystem();
        isActive = false;
        LOG.debug("Service destroyed");
    }

    private void startVPN(final String[] targetPackages, final Bundle moduleBundle) {
        // Create and configure a new VpnService.Builder
        final Builder builder = new Builder();
        builder.setSession(SESSION_NAME);
        amlTunnel.configureInterface(builder, targetPackages);

        // Create the VPN interface
        final ParcelFileDescriptor vpnInterface;
        try {
            vpnInterface = builder.establish();
        } catch (final IllegalStateException e) {
            LOG.error("Cannot establish tunnel, application not prepared");
            stopSelf();
            return;
        }

        // Initialize AML and load modules
        try {
            amlTunnel.initialize(this, vpnInterface);
            if (moduleBundle != null)
                new ReflectiveModuleLoader(amlTunnel.getModuleManager()).addModules(moduleBundle);
            amlTunnel.startSystem();
        } catch (final IOException e) {
            LOG.error("Error while starting AML", e);
            stopSelf();
        }
    }

    public class ContextBinding extends Binder {
        public AMLContext getTunnelContext() {
            return amlTunnel.getAmlContext();
        }
    }

}
