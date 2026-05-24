/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.ios;

import com.codename1.io.bonjour.BonjourPlatform;
import com.codename1.io.bonjour.BonjourServiceListener;
import com.codename1.ui.CN;

import java.util.Map;

/// iOS Bonjour implementation. Each entry point goes through IOSNative,
/// which gates the NSNetServiceBrowser / NSNetService native code behind
/// CN1_INCLUDE_BONJOUR so apps that never reference
/// `com.codename1.io.bonjour` ship without the delegate / NSBonjourServices
/// Info.plist entries.
public final class IOSBonjourPlatform extends BonjourPlatform {
    @Override
    public boolean isSupported() { return true; }

    @Override
    public Object startBrowse(String type, BonjourServiceListener listener) {
        if (listener == null) return null;
        long handle = IOSImplementation.nativeInstance.bonjourBrowseStart(type);
        if (handle == 0) {
            final BonjourServiceListener lf = listener;
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    lf.onBrowseError(new RuntimeException("Bonjour unavailable"));
                }
            });
            return null;
        }
        IOSConnectivity.registerBonjour(handle, listener);
        return Long.valueOf(handle);
    }

    @Override
    public void stopBrowse(Object handle) {
        if (handle == null) return;
        long h = ((Long) handle).longValue();
        IOSImplementation.nativeInstance.bonjourBrowseStop(h);
        IOSConnectivity.unregisterBonjour(h);
    }

    @Override
    public Object startPublish(String name, String type, int port,
                               Map<String, String> txt) {
        String[] keys = new String[txt == null ? 0 : txt.size()];
        String[] vals = new String[keys.length];
        if (txt != null) {
            int i = 0;
            for (Map.Entry<String, String> e : txt.entrySet()) {
                keys[i] = e.getKey();
                vals[i] = e.getValue() == null ? "" : e.getValue();
                i++;
            }
        }
        long h = IOSImplementation.nativeInstance.bonjourPublishStart(name, type, port, keys, vals);
        return h == 0 ? null : Long.valueOf(h);
    }

    @Override
    public void stopPublish(Object handle) {
        if (handle == null) return;
        IOSImplementation.nativeInstance.bonjourPublishStop(((Long) handle).longValue());
    }
}
