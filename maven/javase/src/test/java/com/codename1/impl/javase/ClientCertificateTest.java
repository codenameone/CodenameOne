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
package com.codename1.impl.javase;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// The socket factory ConnectionRequest.setClientCertificate installs presents
/// the certificate to a server that demands one.
class ClientCertificateTest {

    @Test
    void serverSeesTheClientCertificate() throws Exception {
        File dir = File.createTempFile("cn1-mtls", "");
        assertTrue(dir.delete() && dir.mkdir());
        dir.deleteOnExit();
        File store = new File(dir, "device.p12");
        store.deleteOnExit();
        char[] password = "changeit".toCharArray();
        String keytool = System.getProperty("java.home") + File.separator + "bin" + File.separator + "keytool";
        Process p = new ProcessBuilder(keytool, "-genkeypair", "-alias", "device", "-keyalg", "EC",
                "-keysize", "256", "-dname", "CN=thermostat-42", "-validity", "2", "-storetype", "PKCS12",
                "-keystore", store.getPath(), "-storepass", "changeit", "-keypass", "changeit")
                .redirectErrorStream(true).start();
        assertEquals(0, p.waitFor());

        KeyStore keys = KeyStore.getInstance("PKCS12");
        InputStream in = new FileInputStream(store);
        try {
            keys.load(in, password);
        } finally {
            in.close();
        }
        PrivateKey key = (PrivateKey) keys.getKey("device", password);
        Certificate certificate = keys.getCertificate("device");
        String pem = "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder().encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----\n";

        // The server presents the same certificate and trusts only it.
        KeyManagerFactory serverKeys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        serverKeys.init(keys, password);
        TrustManagerFactory serverTrust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        serverTrust.init(keys);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(serverKeys.getKeyManagers(), serverTrust.getTrustManagers(), null);
        final SSLServerSocket server = (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0);
        server.setNeedClientAuth(true);
        final AtomicReference<Object> seen = new AtomicReference<Object>();
        Thread accept = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SSLSocket s = (SSLSocket) server.accept();
                    s.startHandshake();
                    seen.set(s.getSession().getPeerCertificates()[0]);
                    s.getOutputStream().write(1);
                    s.close();
                } catch (Exception e) {
                    seen.set(e);
                }
            }
        });
        accept.start();

        SSLSocket client = (SSLSocket) TrustModifier.clientCertificateFactory(pem, "EC", key.getEncoded(),
                serverTrust.getTrustManagers())
                .createSocket("127.0.0.1", server.getLocalPort());
        try {
            client.startHandshake();
            OutputStream out = client.getOutputStream();
            out.flush();
            assertEquals(1, client.getInputStream().read());
        } finally {
            client.close();
            accept.join(10000);
            server.close();
        }
        Object peer = seen.get();
        assertTrue(peer instanceof X509Certificate, String.valueOf(peer));
        assertEquals("CN=thermostat-42", ((X509Certificate) peer).getSubjectX500Principal().getName());
    }
}
