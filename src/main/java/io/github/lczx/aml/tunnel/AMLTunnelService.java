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
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import io.github.lczx.aml.tunnel.protocol.IpProtocolDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMLTunnelService extends VpnService {

    public static final String INTENT_ACTION_KEY = "action";
    public static final int INTENT_ACTION_VALUE_START = 1;
    public static final int INTENT_ACTION_VALUE_STOP = 2;
    public static final String INTENT_TARGET_PACKAGES_KEY = "targetPackages";

    private static final Logger LOG = LoggerFactory.getLogger(AMLTunnelService.class);
    private static final String VPN_ADDRESS = "10.0.8.2"; // IPv4 only
    private static final String VPN_ROUTE = "0.0.0.0"; // Catch-all

    private static boolean isRunning = false;

    private String[] targetPackages;
    private ParcelFileDescriptor vpnInterface;

    private Thread vpnThread;
    private ConcurrentPacketConnector tcpTxPipe, udpTxPipe, rxPipe;

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        switch (intent.getIntExtra(INTENT_ACTION_KEY, -1)) {
            case INTENT_ACTION_VALUE_START:
                targetPackages = intent.getStringArrayExtra(INTENT_TARGET_PACKAGES_KEY);
                startVPN();
                break;
            case INTENT_ACTION_VALUE_STOP:
                stopVPN();
                stopSelf();
                //stopService(new Intent(this, AMLTunnelService.class));
                break;
            default:
                LOG.warn("Service started with unknown action number");
                break;
        }
        return START_STICKY;
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        stopVPN();
    }

    private void startVPN() {
        vpnInterface = initializeInterface();
        if (vpnInterface == null) {
            LOG.error("Cannot establish tunnel, application not prepared");
            stopSelf();
        }

        final IpProtocolDispatcher dispatcher = new IpProtocolDispatcher(tcpTxPipe, udpTxPipe, null);

        vpnThread = new Thread(new TaskRunner("VPN I/O",
                new TunUplinkReader(vpnInterface.getFileDescriptor(), dispatcher),
                new TunDownlinkWriter(vpnInterface.getFileDescriptor(), rxPipe)));

        // TODO: Do something with these pipes

        vpnThread.start();
        isRunning = true;
    }

    private void stopVPN() {
        isRunning = false;
        vpnThread.interrupt();

        cleanup();
    }

    private void cleanup() {
        tcpTxPipe = null;
        udpTxPipe = null;
        rxPipe = null;
        // TODO: Clear buffer pool when implemented
        IOUtils.closeResources(vpnInterface);
    }

    private ParcelFileDescriptor initializeInterface() {
        final Builder builder = new Builder();
        builder.setSession("firewall");
        builder.addAddress(VPN_ADDRESS, 32);
        builder.addRoute(VPN_ROUTE, 0);
        if (targetPackages != null) {
            for (final String packageName : targetPackages) {
                try {
                    builder.addAllowedApplication(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    LOG.warn("Target application with package \"{}\" not found, ignoring", packageName);
                }
            }
        }
        try {
            return builder.establish();
        } catch (final IllegalStateException e) {
            LOG.error("Cannot establish tunnel", e);
            return null;
        }
    }

}
