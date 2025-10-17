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

package com.codename1.plugin;

import com.codename1.plugin.event.PluginEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Support class for implementing plugins in codename one.
 *
 * @author Steve Hannah
 * @since 8.0
 */
public class PluginSupport {
    private final List<Plugin> plugins = new ArrayList<Plugin>();

    /**
     * Registers a plugin with the Codename One runtime.
     *
     * @param plugin The plugin to register.
     * @since 8.0
     */
    public void registerPlugin(Plugin plugin) {
        synchronized (this) {
            plugins.add(plugin);
        }

    }

    /**
     * Deregisters a plugin from the Codename One runtime.
     *
     * @param plugin The plugin to deregister
     * @since 8.0
     */
    public void deregisterPlugin(Plugin plugin) {
        synchronized (this) {
            plugins.remove(plugin);
        }
    }

    /**
     * Fires a plugin event to give plugins an opportunity to handle it before default behaviour
     * is resumed.
     *
     * @param event The event to fire.
     * @param <T>   The event class
     * @return The event
     */
    public <T extends PluginEvent> T firePluginEvent(T event) {
        List<Plugin> localListeners;

        synchronized (this) {
            localListeners = new ArrayList<Plugin>(plugins);
        }
        for (Plugin plugin : localListeners) {
            if (event.isConsumed()) {
                return event;
            }
            plugin.actionPerformed(event);
        }

        return event;
    }
}
