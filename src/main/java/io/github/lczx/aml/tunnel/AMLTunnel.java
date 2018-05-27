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

import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import eu.faircode.netguard.IPUtil;
import io.github.lczx.aml.AMLContext;
import io.github.lczx.aml.AMLContextImpl;
import io.github.lczx.aml.hook.ModuleManager;
import io.github.lczx.aml.hook.monitoring.BaseMeasureKeys;
import io.github.lczx.aml.hook.monitoring.MeasureHolder;
import io.github.lczx.aml.hook.monitoring.StatusProbe;
import io.github.lczx.aml.tunnel.network.IpProtocolDispatcher;
import io.github.lczx.aml.tunnel.network.ProtocolNetworkInterface;
import io.github.lczx.aml.tunnel.network.tcp.TcpNetworkInterface;
import io.github.lczx.aml.tunnel.network.udp.UdpNetworkInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reference lifecycle of AML components: this class orchestrates and binds AML tunnel components together.
 */
public class AMLTunnel {

    private static final Logger LOG = LoggerFactory.getLogger(AMLTunnel.class);
    private static final String VPN_ADDRESS = "10.0.8.2"; // IPv4 only

    private ParcelFileDescriptor vpnInterface;

    private AMLContext amlContext;
    private ModuleManager moduleManager;

    private ConcurrentPacketConnector tcpTxPipe, udpTxPipe, rxPipe;
    private ProtocolNetworkInterface tcpNetworkInterface, udpNetworkInterface;
    private Thread vpnThread;

    public void configureInterface(final VpnService.Builder builder, final String[] targetPackages) {
        builder.addAddress(VPN_ADDRESS, 32);
        //builder.addRoute("0.0.0.0", 0);
        addRoutes(builder);
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
    }

    public void initialize(final SocketProtector socketProtector, final ParcelFileDescriptor vpnInterface) {
        this.vpnInterface = vpnInterface;

        amlContext = new AMLContextImpl(socketProtector);
        amlContext.getStatusMonitor().attachProbe(new ServiceProbe());

        tcpTxPipe = new ConcurrentPacketConnector();
        udpTxPipe = new ConcurrentPacketConnector();
        rxPipe = new ConcurrentPacketConnector();

        tcpNetworkInterface = new TcpNetworkInterface(amlContext, tcpTxPipe, rxPipe);
        udpNetworkInterface = new UdpNetworkInterface(amlContext, udpTxPipe, rxPipe);

        final IpProtocolDispatcher dispatcher = new IpProtocolDispatcher(tcpTxPipe, udpTxPipe, null);
        vpnThread = new Thread(new TaskRunner("VPN I/O",
                new TunUplinkReader(vpnInterface.getFileDescriptor(), dispatcher),
                new TunDownlinkWriter(vpnInterface.getFileDescriptor(), rxPipe)), "tun_vpn");

        moduleManager = new ModuleManager(amlContext);
    }

    public void startSystem() throws IOException {
        if (amlContext == null)
            throw new IllegalStateException("Tunnel not initialized");

        moduleManager.startModules();

        try {
            tcpNetworkInterface.start();
            udpNetworkInterface.start();
            vpnThread.start();
        } catch (final IOException e) {
            cleanup();
            throw new IOException("Selector initialization failed", e);
        }
    }

    public void stopSystem() {
        if (amlContext != null) {
            moduleManager.stopModules();
            vpnThread.interrupt();
            tcpNetworkInterface.shutdown();
            udpNetworkInterface.shutdown();
        }
        cleanup();
    }

    public AMLContext getAmlContext() {
        return amlContext;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    private void addRoutes(final VpnService.Builder builder) {
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

    private void cleanup() {
        moduleManager = null;
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
