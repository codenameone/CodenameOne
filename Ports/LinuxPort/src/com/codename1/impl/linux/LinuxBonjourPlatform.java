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
package com.codename1.impl.linux;

import com.codename1.impl.MdnsEngine;
import com.codename1.io.bonjour.BonjourPlatform;
import com.codename1.io.bonjour.BonjourService;
import com.codename1.io.bonjour.BonjourServiceListener;
import com.codename1.ui.Display;
import java.util.Map;

/// Bonjour on the Linux desktop and embedded targets, over [LinuxMdns].
/// Listeners run on the event dispatch thread, as on the other ports.
final class LinuxBonjourPlatform extends BonjourPlatform {
    private final LinuxMdns engine = new LinuxMdns();

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public Object startBrowse(String type, final BonjourServiceListener listener) {
        return engine.browse(type, new MdnsEngine.Listener() {
            @Override
            public void resolved(Object browse, String name, String type, String host, int port,
                    Map<String, String> txt) {
                final BonjourService s = new BonjourService(name, type, host, port, txt);
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        listener.onServiceResolved(s);
                    }
                });
            }

            @Override
            public void lost(Object browse, String name, String type) {
                final BonjourService s = new BonjourService(name, type, null, 0, null);
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        listener.onServiceLost(s);
                    }
                });
            }

            @Override
            public void failed(Object browse, final String message) {
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        listener.onBrowseError(new java.io.IOException(message));
                    }
                });
            }
        });
    }

    @Override
    public void stopBrowse(Object handle) {
        engine.stopBrowse(handle);
    }

    @Override
    public Object startPublish(String name, String type, int port, Map<String, String> txt) {
        return engine.publish(name, type, port, txt);
    }

    @Override
    public void stopPublish(Object handle) {
        engine.unpublish(handle);
    }
}
