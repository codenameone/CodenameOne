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
package com.codename1.vpn.profile;

import com.codename1.vpn.VpnProtocol;

/// A VPN configuration: which server, which protocol, and how to prove who
/// you are.
///
/// ```java
/// VpnProfile p = new VpnProfile("vpn.example.com")
///         .protocol(VpnProtocol.IKEV2)
///         .remoteIdentifier("vpn.example.com")
///         .localIdentifier("alice")
///         .usernamePassword("alice", secret)
///         .onDemand(true);
/// ```
///
/// #### Credentials are handed over, not held
///
/// The username and password given here are passed to the platform, which
/// stores them in its own keychain and does not hand them back. [#getPassword]
/// therefore answers null for a profile that was loaded rather than built --
/// a loaded profile describes the configuration, not the secret.
public final class VpnProfile {
    private final String serverAddress;
    private VpnProtocol protocol = VpnProtocol.IKEV2;
    private String remoteIdentifier;
    private String localIdentifier;
    private String username;
    private String password;
    private String sharedSecret;
    private boolean onDemand;
    private String displayName;
    private boolean passwordKnown;

    /// Whether this description came back from [Vpn#load()], which never
    /// carries the secrets.
    private boolean secretsWithheld;

    /// Creates a profile for a server.
    ///
    /// @param serverAddress the host name or address, never null or empty
    public VpnProfile(String serverAddress) {
        if (serverAddress == null || serverAddress.length() == 0) {
            throw new IllegalArgumentException("A VPN server address is required");
        }
        this.serverAddress = serverAddress;
    }

    /// The tunnelling protocol. Defaults to [VpnProtocol#IKEV2].
    public VpnProfile protocol(VpnProtocol value) {
        this.protocol = value == null ? VpnProtocol.IKEV2 : value;
        return this;
    }

    /// The server's identity, as it appears in its certificate. Defaults to
    /// the server address.
    public VpnProfile remoteIdentifier(String value) {
        this.remoteIdentifier = value;
        return this;
    }

    /// This client's identity.
    public VpnProfile localIdentifier(String value) {
        this.localIdentifier = value;
        return this;
    }

    /// Authenticates with a username and password.
    public VpnProfile usernamePassword(String user, String pass) {
        this.username = user;
        this.password = pass;
        this.passwordKnown = pass != null;
        // Supplying a credential is what makes a loaded description
        // installable again; see markSecretsWithheld.
        this.secretsWithheld = false;
        return this;
    }

    /// Authenticates with a pre-shared key.
    public VpnProfile sharedSecret(String value) {
        this.sharedSecret = value;
        this.secretsWithheld = false;
        return this;
    }

    // There is deliberately no certificate(byte[]) here, and deliberately no
    // alwaysOn(boolean).
    //
    // Certificate authentication cannot be carried by a PKCS#12 blob alone:
    // importing one needs its passphrase on both platforms -- iOS through
    // kSecImportExportPassphrase, Android through KeyStore.load -- so a
    // method taking only the bytes could never install the identity, and a
    // profile built with it would save cleanly and then fail to authenticate
    // with nothing to point at. Adding it means adding the passphrase to this
    // class and identity import to both ports; until that happens, not having
    // the method is the honest state.
    //
    // Always-on is not something an ordinary app can ask for. On iOS it needs
    // a supervised device and an MDM payload, and on Android it is a Settings
    // toggle or a device-owner API. The nearest thing either platform offers
    // an app is [#onDemand], which iOS honours and Android does not -- and
    // the Android bridge drops CAPABILITY_ON_DEMAND to say so.

    /// Whether the system should bring the tunnel up when traffic needs it.
    public VpnProfile onDemand(boolean value) {
        this.onDemand = value;
        return this;
    }

    /// The name the user sees for this configuration in system settings.
    public VpnProfile displayName(String value) {
        this.displayName = value;
        return this;
    }

    /// The host name or address.
    public String getServerAddress() {
        return serverAddress;
    }

    /// The tunnelling protocol.
    public VpnProtocol getProtocol() {
        return protocol;
    }

    /// The server's identity, or null for the server address.
    public String getRemoteIdentifier() {
        return remoteIdentifier;
    }

    /// This client's identity, or null.
    public String getLocalIdentifier() {
        return localIdentifier;
    }

    /// The username, or null.
    public String getUsername() {
        return username;
    }

    /// The password, or null -- always null for a loaded profile, because
    /// the platform keeps the secret and does not return it.
    public String getPassword() {
        return password;
    }

    /// The pre-shared key, or null.
    public String getSharedSecret() {
        return sharedSecret;
    }

    /// Whether the tunnel comes up on demand.
    public boolean isOnDemand() {
        return onDemand;
    }

    /// The name shown in system settings, or null.
    public String getDisplayName() {
        return displayName;
    }

    /// Whether a secret was supplied when this profile was built.
    ///
    /// Distinguishes "built without a password" from "loaded, and the
    /// platform kept the password", which [#getPassword] alone cannot.
    public boolean isPasswordKnown() {
        return passwordKnown;
    }

    /// Records that this description came from the platform and carries no
    /// secrets.
    ///
    /// The load path never hands back a password or a pre-shared key -- the
    /// platform holds them and does not give them out -- so a description
    /// that came from there is not installable as it stands. Marking it lets
    /// [Vpn#install] refuse the reinstall instead of quietly replacing a
    /// working configuration with an unauthenticated one; supplying either
    /// credential clears the mark.
    ///
    /// @hidden not part of the public API.
    void markSecretsWithheld() {
        this.secretsWithheld = true;
    }

    /// Whether this description came from the platform with its secrets
    /// removed.
    ///
    /// @hidden not part of the public API.
    boolean areSecretsWithheld() {
        return secretsWithheld;
    }
}
