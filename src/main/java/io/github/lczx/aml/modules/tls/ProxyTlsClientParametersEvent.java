package io.github.lczx.aml.modules.tls;

import io.github.lczx.aml.hook.AMLEvent;
import io.github.lczx.aml.modules.tls.proxy.ClientParameters;

public class ProxyTlsClientParametersEvent extends AMLEvent {

    private final ProxyConnection connection;
    private final ClientParameters clientParameters;

    /* package*/ ProxyTlsClientParametersEvent(
            final ProxyConnection connection, final ClientParameters clientParameters) {
        this.connection = connection;
        this.clientParameters = clientParameters;
    }

    public ProxyConnection getConnection() {
        return connection;
    }

    public ClientParameters getClientParameters() {
        return clientParameters;
    }

}
