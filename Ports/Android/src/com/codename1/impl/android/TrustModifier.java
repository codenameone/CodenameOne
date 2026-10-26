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
package com.codename1.impl.android;

import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class TrustModifier {

    private static final TrustingHostnameVerifier TRUSTING_HOSTNAME_VERIFIER = new TrustingHostnameVerifier();
    private static SSLSocketFactory factory;

    public static void relaxHostChecking(HttpURLConnection conn) throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
        if (conn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) conn;
            SSLSocketFactory factory = prepFactory(httpsConnection);

            httpsConnection.setSSLSocketFactory(factory);
            httpsConnection.setHostnameVerifier(TRUSTING_HOSTNAME_VERIFIER);
        }
    }

    static synchronized SSLSocketFactory prepFactory(HttpsURLConnection httpsConnection) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        if (factory == null) {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{new AlwaysTrustManager()}, null);
            factory = ctx.getSocketFactory();
        }

        return factory;
    }

    private static final class TrustingHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    /// A socket factory that presents `chainPem` with its private key, and
    /// checks the server as the default context does unless `insecure` asked
    /// for the same trust-everything manager as relaxHostChecking.
    static SSLSocketFactory clientCertificateFactory(String chainPem, String keyAlgorithm, byte[] pkcs8,
            boolean insecure) throws java.io.IOException, java.security.GeneralSecurityException {
        java.util.Collection<? extends java.security.cert.Certificate> certificates =
                java.security.cert.CertificateFactory.getInstance("X.509").generateCertificates(
                        new java.io.ByteArrayInputStream(chainPem.getBytes("US-ASCII")));
        java.security.PrivateKey key = java.security.KeyFactory.getInstance(keyAlgorithm)
                .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(pkcs8));
        // An in-memory store holding the one entry; the password protects nothing.
        char[] password = new char[0];
        java.security.KeyStore store = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
        store.load(null, null);
        store.setKeyEntry("client", key, password,
                certificates.toArray(new java.security.cert.Certificate[certificates.size()]));
        javax.net.ssl.KeyManagerFactory keys = javax.net.ssl.KeyManagerFactory.getInstance(
                javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        keys.init(store, password);
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(keys.getKeyManagers(), insecure ? new TrustManager[]{new AlwaysTrustManager()} : null, null);
        return ctx.getSocketFactory();
    }

    private static class AlwaysTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}
