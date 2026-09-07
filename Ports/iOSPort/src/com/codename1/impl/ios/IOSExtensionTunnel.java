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
package com.codename1.impl.ios;

import com.codename1.impl.vpn.ExtensionTunnelHost;

/// The packet writer inside a generated Network Extension.
///
/// #### Why this is in the port rather than in core
///
/// The `writeNative` below is implemented by the generated extension and by
/// nothing else. A native declared in core has to be implemented by every
/// port -- `scripts/check-native-signatures.sh` says so, and a device build
/// would say it as a link error -- so it lives here, in the jar only an iOS
/// build carries.
///
/// #### Why it is called at all
///
/// The extension is a separate process with a translated VM and no
/// framework. The generated provider calls [#install(int)] before starting the
/// tunnel, which is what connects the Java packet loop to
/// `NEPacketTunnelFlow`.
///
/// @hidden not part of the public API; called by generated extension code.
public final class IOSExtensionTunnel implements ExtensionTunnelHost.Writer {

    /// Which start this writer belongs to; see [#install(int)].
    private final int generation;

    private IOSExtensionTunnel(int generation) {
        this.generation = generation;
    }

    /// Installs this writer for one start of the tunnel.
    ///
    /// The generation is the extension's own start counter, and carrying it
    /// is what stops a packet crossing tunnels. A stopped tunnel's
    /// `onPacket` can still be running -- the callback cannot be retracted --
    /// and `ExtensionTunnelHost.end` clears the host and the transport but
    /// not the writer, so a late `forward` used to reach whatever provider
    /// was current. If a new tunnel had started by then, one session's
    /// packet went out on another's link.
    ///
    /// @hidden not part of the public API.
    public static void install(int generation) {
        ExtensionTunnelHost.setWriter(generation,
                new IOSExtensionTunnel(generation));
    }

    @Override
    public void write(byte[] packet, int offset, int length) {
        writeNative(generation, packet, offset, length);
    }

    /// Hands one packet to `NEPacketTunnelFlow`.
    ///
    /// Implemented by the generated extension; see
    /// `IOSVpnTunnelExtensionBuilder.writerSource`. It is not in the port's
    /// nativeSources because the symbol only exists inside the extension
    /// target -- the app target has no packet flow to write to -- which is
    /// why the generated project lists it in
    /// `cn1-native-verify-ignore.txt`.
    private static native void writeNative(int generation, byte[] packet,
            int offset, int length);
}
