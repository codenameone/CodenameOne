/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.developerguide.snippets.generated;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.outbox.Outbox;
import com.codename1.orm.session.Session;
import com.codename1.security.CertificateRequest;
import com.codename1.security.KeyGenerator;
import com.codename1.security.KeyPair;
import com.codename1.security.Signature;
import java.io.IOException;

class EmbeddedLinuxConnectedSnippet {
    private Session session;
    private String deviceId;
    private String fleetUrl;
    private String certificatePem;

    void recordReading(Object reading, byte[] body) {
        // tag::embedded-linux-outbox[]
        Outbox outbox = Outbox.open(session, "readings@" + deviceId);

        session.beginTransaction();
        session.persist(reading);
        outbox.enqueue("/v1/readings", body);
        session.commitTransaction();

        // From a timer, or when the network comes back:
        outbox.flush(message -> {
            int status = postToServer(message.getTopic(), message.getKey(), message.getPayload());
            return status < 300 ? Outbox.DELIVERED
                    : status < 500 ? Outbox.REJECTED : Outbox.RETRY;
        });
        // end::embedded-linux-outbox[]
    }

    KeyPair enroll() throws IOException {
        // tag::embedded-linux-csr[]
        KeyPair keys = KeyGenerator.ec(256);
        byte[] csr = new CertificateRequest(keys.getPublicKey().getEncoded(), "EC")
                .commonName(deviceId)
                .dnsName(deviceId + ".local")
                .sign(data -> Signature.sign(Signature.SHA256_WITH_ECDSA, keys.getPrivateKey(), data));
        String pem = CertificateRequest.toPem(csr);
        // Send pem to the enrollment endpoint, store the certificate it returns.
        // end::embedded-linux-csr[]
        return keys;
    }

    ConnectionRequest connect(KeyPair keys) {
        // tag::embedded-linux-mtls[]
        ConnectionRequest request = new ConnectionRequest(fleetUrl + "/v1/readings", true);
        request.setClientCertificate(certificatePem, keys.getPrivateKey());
        // end::embedded-linux-mtls[]
        return request;
    }

    private int postToServer(String topic, String key, byte[] payload) throws IOException {
        return 200;
    }
}
