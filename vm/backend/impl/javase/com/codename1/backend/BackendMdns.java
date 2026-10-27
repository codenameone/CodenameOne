/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import com.codename1.impl.MdnsEngine;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/// The Java SE twin of the backend's multicast DNS socket, over
/// `MulticastSocket`. The protocol above it is the same [MdnsEngine] the
/// translated binary runs, so a server developed on a laptop is found by the
/// phone exactly as the board will be.
final class BackendMdns extends MdnsEngine {
    private static final int PORT = 5353;
    // The mDNS multicast group is fixed by RFC 6762, not a deployment choice.
    private static final String GROUP = "224.0.0.251"; // NOPMD AvoidUsingHardCodedIP
    private final AtomicReference<MulticastSocket> socket = new AtomicReference<MulticastSocket>();

    @Override
    protected int rawOpen() {
        MulticastSocket s = null; //NOPMD CloseResource - kept in the field until rawClose, closed here on failure
        try {
            s = new MulticastSocket(null);
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(PORT));
            s.setTimeToLive(255);
            InetAddress group = InetAddress.getByName(GROUP);
            int joined = 0;
            for (NetworkInterface ni : usable()) {
                try {
                    s.joinGroup(new InetSocketAddress(group, PORT), ni);
                    joined++;
                } catch (IOException ignored) {
                    // An interface that refuses the group is skipped, as in C.
                }
            }
            if (joined == 0) {
                s.joinGroup(group);
            }
            socket.set(s);
            return 1;
        } catch (IOException e) {
            if (s != null) {
                s.close();
            }
            return -5;
        }
    }

    @Override
    protected int rawSend(int handle, byte[] packet, int length) {
        MulticastSocket s = socket.get(); //NOPMD CloseResource - owned by the field; rawClose closes it
        if (s == null) {
            return -9;
        }
        try {
            s.send(new DatagramPacket(packet, length, InetAddress.getByName(GROUP), PORT));
            return length;
        } catch (IOException e) {
            // A network that is down is not an error the engine can act on.
            return 0;
        }
    }

    @Override
    protected int rawReceive(int handle, byte[] buffer, int timeoutMs) {
        MulticastSocket s = socket.get(); //NOPMD CloseResource - owned by the field; rawClose closes it
        if (s == null) {
            return -9;
        }
        try {
            s.setSoTimeout(Math.max(1, timeoutMs));
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            s.receive(p);
            return p.getLength();
        } catch (SocketTimeoutException e) {
            return 0;
        } catch (IOException e) {
            return -5;
        }
    }

    @Override
    protected String rawHostInfo() {
        StringBuilder out = new StringBuilder();
        try {
            out.append(InetAddress.getLocalHost().getHostName());
        } catch (IOException e) {
            out.append("localhost");
        }
        out.append('\n');
        for (NetworkInterface ni : usable()) {
            Enumeration<InetAddress> addresses = ni.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress a = addresses.nextElement();
                if (a instanceof Inet4Address) {
                    out.append(a.getHostAddress()).append('\n');
                }
            }
        }
        return out.toString();
    }

    @Override
    protected void rawClose(int handle) {
        MulticastSocket s = socket.getAndSet(null); //NOPMD CloseResource - closed just below
        if (s != null) {
            s.close();
        }
    }

    private static List<NetworkInterface> usable() {
        List<NetworkInterface> out = new ArrayList<NetworkInterface>();
        try {
            Enumeration<NetworkInterface> all = NetworkInterface.getNetworkInterfaces();
            while (all != null && all.hasMoreElements()) {
                NetworkInterface ni = all.nextElement();
                if (ni.isUp() && ni.supportsMulticast() && !ni.isLoopback()) {
                    out.add(ni);
                }
            }
        } catch (IOException e) {
            // No interfaces: the engine sends on the default route.
        }
        return out;
    }
}
