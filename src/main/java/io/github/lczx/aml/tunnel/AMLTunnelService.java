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
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import io.github.lczx.aml.modules.tls.TlsProxy;
import eu.faircode.netguard.IPUtil;
import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.AMLContextImpl;
import io.github.lczx.aml.hook.monitoring.BaseMeasureKeys;
import io.github.lczx.aml.hook.monitoring.MeasureHolder;
import io.github.lczx.aml.hook.monitoring.StatusProbe;
import io.github.lczx.aml.tunnel.protocol.IpProtocolDispatcher;
import io.github.lczx.aml.tunnel.protocol.ProtocolNetworkInterface;
import io.github.lczx.aml.tunnel.protocol.tcp.TcpNetworkInterface;
import io.github.lczx.aml.tunnel.protocol.udp.UdpNetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AMLTunnelService extends VpnService implements SocketProtector {

    public static final String ACTION_START = "aml.tunnel.intent.action.SERVICE_START";
    public static final String ACTION_STOP = "aml.tunnel.intent.action.SERVICE_STOP";
    public static final String ACTION_BIND_MONITORING = "aml.tunnel.intent.action.BIND_MONITORING";
    public static final String EXTRA_TARGET_PACKAGES = "aml.tunnel.intent.extra.TARGET_NAMES";

    private static final Logger LOG = LoggerFactory.getLogger(AMLTunnelService.class);
    private static final String VPN_ADDRESS = "10.0.8.2"; // IPv4 only
    private static final String VPN_ROUTE = "0.0.0.0"; // Catch-all

    private static boolean isActive = false;

    private String[] targetPackages;
    private ParcelFileDescriptor vpnInterface;

    private AMLContext amlContext;

    private ConcurrentPacketConnector tcpTxPipe, udpTxPipe, rxPipe;
    private ProtocolNetworkInterface tcpNetworkInterface, udpNetworkInterface;
    private Thread vpnThread;

    private TlsProxy proxy;

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
                targetPackages = intent.getStringArrayExtra(EXTRA_TARGET_PACKAGES);
                startVPN();
                break;
            case ACTION_STOP:
                // stopService()/stopSelf() doesn't work and onDestroy() does not get called, the only way to stop
                // the tunnel is to close the tunnel device, then we can call stopSelf() and terminate
                stopVPN();
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
        stopVPN();
        isActive = false;
        LOG.debug("Service destroyed");
    }

    private void startVPN() {
        vpnInterface = initializeInterface();
        if (vpnInterface == null) {
            LOG.error("Cannot establish tunnel, application not prepared");
            stopSelf();
            return;
        }

        amlContext = new AMLContextImpl(this);
        amlContext.getStatusMonitor().attachProbe(new ServiceProbe());


        tcpTxPipe = new ConcurrentPacketConnector();
        udpTxPipe = new ConcurrentPacketConnector();
        rxPipe = new ConcurrentPacketConnector();

        tcpNetworkInterface = new TcpNetworkInterface(amlContext, tcpTxPipe, rxPipe);
        udpNetworkInterface = new UdpNetworkInterface(amlContext, udpTxPipe, rxPipe);

        proxy = new TlsProxy(this, getAssets());
        proxy.start();
        tcpNetworkInterface.__setHook(proxy.createTcpHook());

        final IpProtocolDispatcher dispatcher = new IpProtocolDispatcher(tcpTxPipe, udpTxPipe, null);
        vpnThread = new Thread(new TaskRunner("VPN I/O",
                new TunUplinkReader(vpnInterface.getFileDescriptor(), dispatcher),
                new TunDownlinkWriter(vpnInterface.getFileDescriptor(), rxPipe)));

        try {
            tcpNetworkInterface.start();
            udpNetworkInterface.start();
            vpnThread.start();
        } catch (final IOException e) {
            LOG.error("Selector initialization failed", e);
            cleanup();
            stopSelf();
            //return;
        }
    }

    private void stopVPN() {
        if (amlContext != null) {
            proxy.stop();
            vpnThread.interrupt();
            tcpNetworkInterface.shutdown();
            udpNetworkInterface.shutdown();
        }
        cleanup();
    }

    private void cleanup() {
        proxy = null;
        tcpTxPipe = null;
        udpTxPipe = null;
        rxPipe = null;
        tcpNetworkInterface = null;
        udpNetworkInterface = null;
        // TODO: Clear buffer pool when implemented
        IOUtils.safeClose(vpnInterface);
        vpnInterface = null;
        amlContext = null;
    }

    private ParcelFileDescriptor initializeInterface() {
        final Builder builder = new Builder();
        builder.setSession("firewall");
        builder.addAddress(VPN_ADDRESS, 32);
        //builder.addRoute(VPN_ROUTE, 0);
        configureRoutes(builder);
        if (targetPackages != null) {
            for (final String packageName : targetPackages) {
                try {
                    builder.addAllowedApplication(packageName);
                    LOG.debug("Adding package to allowed applications: {}", packageName);
                } catch (final PackageManager.NameNotFoundException e) {
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

    private void configureRoutes(final Builder builder) {
        // Exclusion ranges
        final List<IPUtil.CIDR> blacklist = new ArrayList<>();
        blacklist.add(new IPUtil.CIDR("127.0.0.0", 8)); // Loopback
        blacklist.add(new IPUtil.CIDR("192.168.42.0", 23)); // Tethering (USB: *42.x, Wi-Fi: *43.x )
        blacklist.add(new IPUtil.CIDR("192.168.49.0", 24)); // Wi-Fi Direct
        blacklist.add(new IPUtil.CIDR("224.0.0.0", 3)); // Broadcast (pt. 1)
        // blacklist.add(new IPUtil.CIDR("239.255.255.250", 32))
        Collections.sort(blacklist);

        try {
            InetAddress start = InetAddress.getByName("0.0.0.0");
            for (final IPUtil.CIDR exclude : blacklist) {
                LOG.trace("Excluding IP range from route: {} ... {}",
                        exclude.getStart().getHostAddress(), exclude.getEnd().getHostAddress());
                for (final IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart())))
                    builder.addRoute(include.address, include.prefix);
                start = IPUtil.plus1(exclude.getEnd());
            }
            for (final IPUtil.CIDR include : IPUtil.toCIDR("224.0.1.130", "239.255.255.249"))
                builder.addRoute(include.address, include.prefix);
            for (final IPUtil.CIDR include : IPUtil.toCIDR("239.255.255.251", "255.255.255.255"))
                builder.addRoute(include.address, include.prefix);
        } catch (final UnknownHostException e) {
            LOG.error("Error while configuring routes", e);
        }
    }

    public class ContextBinding extends Binder {
        public AMLContext getTunnelContext() {
            return amlContext;
        }
    }

    private class ServiceProbe implements StatusProbe {
        @Override
        public void onMeasure(final MeasureHolder m) {
            m.putInt(BaseMeasureKeys.QUEUE_SIZE_RX, rxPipe.waitingPacketsCount());
            m.putInt(BaseMeasureKeys.QUEUE_SIZE_TX_TCP, tcpTxPipe.waitingPacketsCount());
            m.putInt(BaseMeasureKeys.QUEUE_SIZE_TX_UDP, udpTxPipe.waitingPacketsCount());
            m.putInt(BaseMeasureKeys.THREAD_STATE_VPN, vpnThread.getState().ordinal());
        }
    }

}
