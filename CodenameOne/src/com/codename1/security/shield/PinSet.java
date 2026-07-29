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
package com.codename1.security.shield;

import java.util.Hashtable;
import java.util.Vector;

/// An immutable set of certificate pins, keyed by host, as published by the attestation service.
///
/// Pins are over the **subject public key info**, not the whole certificate, so a host can renew
/// its certificate on the same key pair without invalidating the pin. A chain matches if *any*
/// certificate in it matches *any* pin for the host, which is what makes it safe to pin an issuing
/// CA as the backup.
///
/// #### The never-brick rules
///
/// Client-side pinning is the one part of the shield that can take an app offline for reasons the
/// developer cannot fix without an app store release, so the failure behaviour is deliberately
/// asymmetric:
///
/// - A host with **no pins** is never enforced. That covers first run, a cold start with no
///   network, and any host the service has not published pins for.
/// - A **failed pin fetch** never fails a request. The last known set is kept.
/// - Pins carry a soft expiry, after which a refresh is attempted, and a much later hard expiry.
///   Past the hard expiry the set is dropped and enforcement stops. A device that cannot reach the
///   service for weeks loses pinning; it does not lose the app.
/// - Only an actual mismatch -- a host that *has* pins presenting a chain that matches none of
///   them -- fails a request, and it fails before any request body is written.
public final class PinSet {

    /// A pin set with no hosts. Enforces nothing.
    public static final PinSet EMPTY = new PinSet(new Hashtable(), 0, 0, 0);

    private final Hashtable hostToPins;
    private final int version;
    private final long softExpiry;
    private final long hardExpiry;

    /// @param hostToPins host (lowercase) to a `Vector` of base64 SHA-256 SPKI digests
    /// @param version monotonic version from the service, used to detect a newer published set
    /// @param softExpiry local millis after which a refresh should be attempted, 0 for never
    /// @param hardExpiry local millis after which the set is discarded entirely, 0 for never
    public PinSet(Hashtable hostToPins, int version, long softExpiry, long hardExpiry) {
        // Deep copy. The set is reachable through the public AppShield.getPinSet(),
        // and a caller that cleared the backing vectors would leave every host
        // looking unpinned -- which silently disables enforcement rather than
        // failing visibly.
        this.hostToPins = copyOf(hostToPins);
        this.version = version;
        this.softExpiry = softExpiry;
        this.hardExpiry = hardExpiry;
    }

    public int getVersion() {
        return version;
    }

    /// True once the set should be refreshed. Does not mean it has stopped being enforced.
    public boolean isStale() {
        return softExpiry > 0 && System.currentTimeMillis() > softExpiry;
    }

    /// True once the set is too old to keep enforcing. At this point pinning disables itself
    /// rather than risk locking a long-offline device out of its own app.
    public boolean isExpired() {
        return hardExpiry > 0 && System.currentTimeMillis() > hardExpiry;
    }

    /// True when this set has at least one pin for the host and has not hard-expired, i.e. when a
    /// chain for this host is actually going to be checked.
    public boolean isEnforcedFor(String host) {
        if (host == null || isExpired()) {
            return false;
        }
        Vector pins = pinsFor(host);
        return pins != null && !pins.isEmpty();
    }

    /// Number of hosts with at least one pin. Used by tests and diagnostics.
    public int hostCount() {
        return hostToPins.size();
    }

    /// The pins registered for a host, honouring a leading `*.` wildcard, or null when the host is
    /// not pinned.
    public Vector pinsFor(String host) {
        if (host == null) {
            return null;
        }
        String h = ShieldHosts.normalize(host);
        Object exact = hostToPins.get(h);
        if (exact != null) {
            return copyOf((Vector) exact);
        }
        // Walk up the labels so a "*.example.com" entry covers "api.example.com".
        int dot = h.indexOf('.');
        while (dot >= 0 && dot < h.length() - 1) {
            Object wild = hostToPins.get("*." + h.substring(dot + 1));
            if (wild != null) {
                return copyOf((Vector) wild);
            }
            dot = h.indexOf('.', dot + 1);
        }
        return null;
    }

    private static Hashtable copyOf(Hashtable in) {
        Hashtable out = new Hashtable();
        if (in == null) {
            return out;
        }
        java.util.Enumeration keys = in.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = in.get(key);
            out.put(key, value instanceof Vector ? copyOf((Vector) value) : value);
        }
        return out;
    }

    private static Vector copyOf(Vector in) {
        Vector out = new Vector();
        if (in != null) {
            for (int i = 0; i < in.size(); i++) {
                out.addElement(in.elementAt(i));
            }
        }
        return out;
    }

    /// True when at least one of the supplied chain digests matches a pin for the host.
    ///
    /// Returns true when the host is not pinned at all -- "no opinion" must never be reported as a
    /// mismatch, or an unpinned host would start failing.
    public boolean matches(String host, String[] chainSpkiDigests) {
        if (!isEnforcedFor(host)) {
            return true;
        }
        if (chainSpkiDigests == null || chainSpkiDigests.length == 0) {
            return false;
        }
        Vector pins = pinsFor(host);
        for (String digest : chainSpkiDigests) {
            if (digest != null && pins.contains(digest)) {
                return true;
            }
        }
        return false;
    }

    /// True when no host is pinned.
    public boolean isEmpty() {
        return hostToPins.isEmpty();
    }

    @Override
    public String toString() {
        return "PinSet[version=" + version + ", hosts=" + hostToPins.size()
                + ", stale=" + isStale() + ", expired=" + isExpired() + "]";
    }
}
