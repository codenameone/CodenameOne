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
package com.codename1.androidcompat.jdk;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/// `java.security.KeyStore` for the Codename One runtime. Only the
/// `AndroidKeyStore` type exists, and it holds secret keys generated through
/// `KeyGenerator.getInstance(alg, "AndroidKeyStore")`; where they are kept,
/// and why, is described on [AndroidKeyStoreEntries].
///
/// JDK types in signatures (`java.security.Key`, the checked exceptions) are
/// written under their JDK names: the remap step rewrites them to this
/// package's shims, as it does the application's own references.
public class KeyStore {
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private boolean loaded;

    protected KeyStore() {
    }

    public static KeyStore getInstance(String type) throws java.security.KeyStoreException {
        if (!ANDROID_KEY_STORE.equals(type)) {
            throw new java.security.KeyStoreException(type + " not found");
        }
        return new KeyStore();
    }

    public static KeyStore getInstance(String type, String provider)
            throws java.security.KeyStoreException, java.security.NoSuchProviderException {
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        if (!ANDROID_KEY_STORE.equals(provider)) {
            throw new java.security.NoSuchProviderException("no such provider: " + provider);
        }
        return getInstance(type);
    }

    public static String getDefaultType() {
        return ANDROID_KEY_STORE;
    }

    public final String getType() {
        return ANDROID_KEY_STORE;
    }

    public final void load(LoadStoreParameter param)
            throws IOException, java.security.NoSuchAlgorithmException,
            java.security.cert.CertificateException {
        if (param != null) {
            throw new IllegalArgumentException("AndroidKeyStore takes no load parameter");
        }
        loaded = true;
    }

    public final void load(InputStream stream, char[] password)
            throws IOException, java.security.NoSuchAlgorithmException,
            java.security.cert.CertificateException {
        if (stream != null) {
            throw new IllegalArgumentException("AndroidKeyStore cannot be loaded from a stream");
        }
        loaded = true;
    }

    private void checkLoaded() throws java.security.KeyStoreException {
        if (!loaded) {
            throw new java.security.KeyStoreException("Uninitialized keystore");
        }
    }

    public final boolean containsAlias(String alias) throws java.security.KeyStoreException {
        checkLoaded();
        return alias != null && AndroidKeyStoreEntries.contains(alias);
    }

    public final Enumeration<String> aliases() throws java.security.KeyStoreException {
        checkLoaded();
        List<String> all = AndroidKeyStoreEntries.aliases();
        Vector<String> v = new Vector<String>(all.size());
        for (int i = 0; i < all.size(); i++) {
            v.addElement(all.get(i));
        }
        return v.elements();
    }

    public final int size() throws java.security.KeyStoreException {
        checkLoaded();
        return AndroidKeyStoreEntries.aliases().size();
    }

    public final boolean isKeyEntry(String alias) throws java.security.KeyStoreException {
        return containsAlias(alias);
    }

    public final boolean isCertificateEntry(String alias) throws java.security.KeyStoreException {
        checkLoaded();
        return false;
    }

    public final void deleteEntry(String alias) throws java.security.KeyStoreException {
        checkLoaded();
        if (alias != null) {
            AndroidKeyStoreEntries.delete(alias);
        }
    }

    /// The key stored under `alias`, or null when there is none. The answer
    /// is always a `javax.crypto.SecretKey`, so the application's cast to one
    /// holds. The password is ignored, as on Android.
    public final java.security.Key getKey(String alias, char[] password)
            throws java.security.KeyStoreException, java.security.NoSuchAlgorithmException,
            java.security.UnrecoverableKeyException {
        checkLoaded();
        if (alias == null) {
            return null;
        }
        try {
            return AndroidKeyStoreEntries.read(alias);
        } catch (IOException e) {
            java.security.UnrecoverableKeyException u =
                    new java.security.UnrecoverableKeyException("Failed to read key " + alias);
            u.initCause(e);
            throw u;
        }
    }

    public final Entry getEntry(String alias, ProtectionParameter protParam)
            throws java.security.NoSuchAlgorithmException, java.security.UnrecoverableEntryException,
            java.security.KeyStoreException {
        java.security.Key k = getKey(alias, null);
        if (k instanceof javax.crypto.SecretKey) {
            return new SecretKeyEntry((javax.crypto.SecretKey) k);
        }
        return null;
    }

    public final boolean entryInstanceOf(String alias, Class entryClass)
            throws java.security.KeyStoreException {
        return containsAlias(alias) && entryClass == SecretKeyEntry.class;
    }

    public final Date getCreationDate(String alias) throws java.security.KeyStoreException {
        checkLoaded();
        try {
            AndroidKeyStoreSecretKey k = alias == null ? null : AndroidKeyStoreEntries.read(alias);
            return k == null ? null : new Date(k.created());
        } catch (IOException e) {
            return null;
        }
    }

    /// `KeyStore.LoadStoreParameter`.
    public interface LoadStoreParameter {
        ProtectionParameter getProtectionParameter();
    }

    /// `KeyStore.ProtectionParameter`.
    public interface ProtectionParameter {
    }

    /// `KeyStore.Entry`.
    public interface Entry {
    }

    /// `KeyStore.SecretKeyEntry`.
    public static final class SecretKeyEntry implements Entry {
        private final javax.crypto.SecretKey key;

        public SecretKeyEntry(javax.crypto.SecretKey secretKey) {
            if (secretKey == null) {
                throw new NullPointerException("invalid null input");
            }
            this.key = secretKey;
        }

        public javax.crypto.SecretKey getSecretKey() {
            return key;
        }
    }
}
