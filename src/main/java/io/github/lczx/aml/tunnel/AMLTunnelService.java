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

package io.github.lczx.aml.tunnel;

import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import io.github.lczx.aml.AMLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AMLTunnelService extends VpnService implements SocketProtector {

    public static final String ACTION_START = "aml.tunnel.intent.action.SERVICE_START";
    public static final String ACTION_STOP = "aml.tunnel.intent.action.SERVICE_STOP";
    public static final String ACTION_BIND_MONITORING = "aml.tunnel.intent.action.BIND_MONITORING";
    public static final String EXTRA_TARGET_PACKAGES = "aml.tunnel.intent.extra.TARGET_NAMES";

    private static final Logger LOG = LoggerFactory.getLogger(AMLTunnelService.class);

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
                startVPN(intent.getStringArrayExtra(EXTRA_TARGET_PACKAGES));
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

    private void startVPN(final String[] targetPackages) {
        final Builder builder = new Builder();
        builder.setSession("firewall");
        amlTunnel.configureInterface(builder, targetPackages);

        final ParcelFileDescriptor vpnInterface;
        try {
            vpnInterface = builder.establish();
        } catch (final IllegalStateException e) {
            LOG.error("Cannot establish tunnel, application not prepared");
            stopSelf();
            return;
        }

        try {
            amlTunnel.startSystem(this, vpnInterface);
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
