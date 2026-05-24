/*
 * Copyright (c) 2008, 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
 */
package com.codename1.impl.javase.connectivity;

import com.codename1.io.bonjour.BonjourPlatform;
import com.codename1.io.bonjour.BonjourServiceListener;
import com.codename1.ui.CN;

import java.util.Map;

/// Bonjour platform for the JavaSE simulator. Loads JmDNS reflectively so
/// the JavaSE port stays JmDNS-optional; developers who want real Bonjour
/// in the simulator add the dependency to their *application's* simulator
/// profile, not to the core / common pom (which would push it to devices).
public final class JavaSEBonjourPlatform extends BonjourPlatform {
    @Override
    public boolean isSupported() {
        return jmdnsAvailable();
    }

    private static boolean jmdnsAvailable() {
        try {
            Class.forName("javax.jmdns.JmDNS");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public Object startBrowse(String type, final BonjourServiceListener listener) {
        JavaSEConnectivityUsage.noteUsage("Bonjour");
        if (listener == null) return null;
        if (!jmdnsAvailable()) {
            System.out.println("[CN1 simulator] Bonjour browse: JmDNS not on classpath. "
                    + "Add org.jmdns:jmdns to the executable-jar/simulator profile of your "
                    + "application's pom to exercise real discovery.");
            CN.callSerially(new Runnable() {
                @Override public void run() {
                    listener.onBrowseError(new UnsupportedOperationException(
                            "JmDNS not on simulator classpath"));
                }
            });
            return null;
        }
        try {
            Object jmdns = Class.forName("javax.jmdns.JmDNS")
                    .getMethod("create").invoke(null);
            System.out.println("[CN1 simulator] Bonjour browse started via JmDNS for type=" + type);
            return jmdns;
        } catch (Throwable t) {
            final Throwable err = t;
            CN.callSerially(new Runnable() {
                @Override public void run() { listener.onBrowseError(err); }
            });
            return null;
        }
    }

    @Override
    public void stopBrowse(Object handle) {
        if (handle == null) return;
        try {
            handle.getClass().getMethod("close").invoke(handle);
        } catch (Throwable ignored) { }
    }

    @Override
    public Object startPublish(String name, String type, int port,
                               Map<String, String> txt) {
        JavaSEConnectivityUsage.noteUsage("Bonjour");
        System.out.println("[CN1 simulator] Bonjour publish " + name + "@" + type + ":" + port);
        return new Object();
    }

    @Override
    public void stopPublish(Object handle) {
    }
}
