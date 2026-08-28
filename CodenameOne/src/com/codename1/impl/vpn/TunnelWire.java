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
package com.codename1.impl.vpn;

import com.codename1.impl.call.CallWire;
import com.codename1.vpn.tunnel.TunnelSetup;

/// The encoding a `TunnelSetup` crosses the SPI in.
///
/// Tab-delimited like every other record here, and for the same reason: the
/// iOS side parses it in C, where building a Java object is expensive and
/// error-prone. Lists are carried in one field, comma-separated, because a
/// variable field count cannot be indexed by position and every reader of a
/// positional record has to agree on where the fixed fields end.
///
/// @hidden not part of the public API.
public final class TunnelWire {

    /// The separator inside a list field. A comma cannot appear in a CIDR
    /// block, an IP address or a DNS name, so nothing legal needs escaping
    /// past what the field escape already does.
    private static final char LIST = ',';

    private TunnelWire() {
    }

    /// @param s the setup to encode
    /// @return the record, never null
    public static String encodeSetup(TunnelSetup s) {
        if (s == null) {
            return "";
        }
        return CallWire.join(new String[]{
            VpnWire.escape(s.getAddress()),
            VpnWire.escape(s.getServer()),
            VpnWire.escape(list(s.getRoutes())),
            VpnWire.escape(list(s.getDnsServers())),
            VpnWire.escape(list(s.getSearchDomains())),
            String.valueOf(s.getMtu()),
            VpnWire.escape(s.getSessionName()),
            VpnWire.escape(s.getData()),
        });
    }

    /// The device's address in CIDR form.
    public static String address(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 0));
    }

    /// The far end.
    public static String server(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 1));
    }

    /// The routes directed into the tunnel.
    public static String[] routes(String[] fields) {
        return items(VpnWire.unescape(CallWire.field(fields, 2)));
    }

    /// The DNS servers the tunnel provides.
    public static String[] dnsServers(String[] fields) {
        return items(VpnWire.unescape(CallWire.field(fields, 3)));
    }

    /// The DNS search domains.
    public static String[] searchDomains(String[] fields) {
        return items(VpnWire.unescape(CallWire.field(fields, 4)));
    }

    /// The link MTU, or the default when the field is unusable.
    public static int mtu(String[] fields) {
        String raw = CallWire.field(fields, 5).trim();
        if (raw.length() == 0) {
            return TunnelSetup.DEFAULT_MTU;
        }
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c < '0' || c > '9') {
                // Tested rather than caught. ParparVM does not check
                // CHECKCAST and a NumberFormatException here would be a
                // parse this side cannot see; a malformed MTU is a
                // configuration mistake, and the default is the recoverable
                // answer -- a tunnel that will not start teaches nobody
                // anything.
                return TunnelSetup.DEFAULT_MTU;
            }
        }
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : TunnelSetup.DEFAULT_MTU;
        } catch (NumberFormatException tooLarge) {
            return TunnelSetup.DEFAULT_MTU;
        }
    }

    /// The session name the system shows.
    public static String sessionName(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 6));
    }

    /// The application data handed to the tunnel.
    public static String data(String[] fields) {
        return VpnWire.unescape(CallWire.field(fields, 7));
    }

    /// Splits a record into its fields.
    public static String[] split(String record) {
        return CallWire.split(record);
    }

    private static String list(String[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(LIST);
            }
            sb.append(values[i]);
        }
        return sb.toString();
    }

    private static String[] items(String value) {
        if (value == null || value.length() == 0) {
            return new String[0];
        }
        int count = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == LIST) {
                count++;
            }
        }
        String[] out = new String[count];
        int at = 0;
        int start = 0;
        for (int i = 0; i <= value.length(); i++) {
            if (i == value.length() || value.charAt(i) == LIST) {
                out[at++] = value.substring(start, i);
                start = i + 1;
            }
        }
        return out;
    }
}
