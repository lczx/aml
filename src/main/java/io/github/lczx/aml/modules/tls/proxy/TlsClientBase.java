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

package io.github.lczx.aml.modules.tls.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.tls.AlertDescription;
import org.spongycastle.crypto.tls.AlertLevel;
import org.spongycastle.crypto.tls.DefaultTlsClient;

abstract class TlsClientBase extends DefaultTlsClient {

    private static final Logger LOG = LoggerFactory.getLogger(TlsClientBase.class);

    @Override
    public void notifyAlertRaised(final short alertLevel, final short alertDescription,
                                  final String message, final Throwable cause) {
        LOG.warn(String.format("Local client alert: [%s] %s, %s",
                AlertLevel.getText(alertLevel), AlertDescription.getText(alertDescription), message), cause);
    }

    @Override
    public void notifyAlertReceived(final short alertLevel, final short alertDescription) {
        LOG.warn("Remote server alert: [{}] {}",
                AlertLevel.getText(alertLevel), AlertDescription.getText(alertDescription));
    }

}
