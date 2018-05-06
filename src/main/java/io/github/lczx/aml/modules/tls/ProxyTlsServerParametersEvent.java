package io.github.lczx.aml.modules.tls;

import io.github.lczx.aml.hook.AMLEvent;
import io.github.lczx.aml.modules.tls.proxy.ServerParameters;

public class ProxyTlsServerParametersEvent extends AMLEvent {

    private final ProxyConnection connection;
    private final ServerParameters serverParameters;

    /* package*/ ProxyTlsServerParametersEvent(
            final ProxyConnection connection, final ServerParameters serverParameters) {
        this.connection = connection;
        this.serverParameters = serverParameters;
    }

    public ProxyConnection getConnection() {
        return connection;
    }

    public ServerParameters getServerParameters() {
        return serverParameters;
    }

}
