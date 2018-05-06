package io.github.lczx.aml.modules.tls;

import io.github.lczx.aml.hook.AMLEvent;

public class ProxyTlsConnectedEvent extends AMLEvent {

    private final ProxyConnection connection;

    /* package*/ ProxyTlsConnectedEvent(final ProxyConnection connection) {
        this.connection = connection;
    }

    public ProxyConnection getConnection() {
        return connection;
    }

}
