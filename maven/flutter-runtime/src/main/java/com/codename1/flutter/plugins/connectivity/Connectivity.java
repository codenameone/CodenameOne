/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.connectivity;

import com.codename1.io.NetworkManager;
import com.codename1.io.NetworkTypeListener;
import com.codename1.ui.Display;

import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;
import dart.core.DartList;

/**
 * connectivity_plus's {@code Connectivity}, over NetworkManager's network type and its
 * change listener. Without a display (a headless run) there is no platform to ask, and
 * the answer is {@code none}.
 */
public class Connectivity {
    private static StreamController<DartList<ConnectivityResult>> changes;

    public Connectivity() {
    }

    static DartList<ConnectivityResult> results(int type, boolean vpn) {
        DartList<ConnectivityResult> out = new DartList<ConnectivityResult>();
        switch (type) {
            case NetworkManager.NETWORK_TYPE_WIFI:
                out.add(ConnectivityResult.wifi);
                break;
            case NetworkManager.NETWORK_TYPE_CELLULAR:
                out.add(ConnectivityResult.mobile);
                break;
            case NetworkManager.NETWORK_TYPE_ETHERNET:
                out.add(ConnectivityResult.ethernet);
                break;
            case NetworkManager.NETWORK_TYPE_BLUETOOTH:
                out.add(ConnectivityResult.bluetooth);
                break;
            case NetworkManager.NETWORK_TYPE_NONE:
                out.add(ConnectivityResult.none);
                break;
            default:
                out.add(ConnectivityResult.other);
                break;
        }
        if (vpn && type != NetworkManager.NETWORK_TYPE_NONE) {
            out.add(ConnectivityResult.vpn);
        }
        return out;
    }

    public Future<DartList<ConnectivityResult>> checkConnectivity() {
        if (!Display.isInitialized()) {
            return Future.value(results(NetworkManager.NETWORK_TYPE_NONE, false));
        }
        NetworkManager nm = NetworkManager.getInstance();
        return Future.value(results(nm.getCurrentNetworkType(), nm.isVPNActive()));
    }

    public Stream<DartList<ConnectivityResult>> onConnectivityChanged() {
        synchronized (Connectivity.class) {
            if (changes == null) {
                changes = StreamController.broadcast();
                if (Display.isInitialized()) {
                    NetworkManager.getInstance().addNetworkTypeListener(new NetworkTypeListener() {
                        @Override
                        public void onNetworkTypeChanged(int oldType, int newType, boolean vpnActive) {
                            changes.add(results(newType, vpnActive));
                        }
                    });
                }
            }
            return changes.stream();
        }
    }
}
