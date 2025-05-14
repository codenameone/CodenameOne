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
package com.codename1.plugin.event;

/**
 * Plugin event fired when {@link Display#isGalleryTypeSupported(int)} method is called to give
 * plugins an opportunity to answer this question.
 *
 * @since 8.0
 * @author Steve Hannah
 */
public class IsGalleryTypeSupportedEvent extends PluginEvent<Boolean> {
    private int type;

    /**
     * Creates a new event with the given type.
     * @param type The type of the gallery.  This is one of the constants defined in {@link Display}.
     *
     */
    public IsGalleryTypeSupportedEvent(int type) {
        super(null, Type.IsGalleryTypeSupported);
        this.type = type;
    }

    /**
     * Gets the type of gallery to open.
     * @return The type of gallery to open.  This is one of the constants defined in {@link Display}.
     * @since 8.0
     * @see Display#openGallery(com.codename1.ui.events.ActionListener, int)
     */
    public int getType() {
        return type;
    }
}
