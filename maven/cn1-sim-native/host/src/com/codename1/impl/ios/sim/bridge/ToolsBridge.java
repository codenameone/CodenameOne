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
package com.codename1.impl.ios.sim.bridge;

/**
 * Feeds simulator tool data from the isolated app universe to the shell
 * universe, whose tools are themselves Codename One UI. Implemented by the
 * shell; called by the child implementation's instrumentation. Like every
 * bridge interface, signatures use only classloader-neutral types.
 */
public interface ToolsBridge {
    /**
     * A network connection was opened by the app.
     *
     * @param id monotonically increasing per-connection id
     * @param url the connection url
     */
    void networkConnect(int id, String url);

    /**
     * The request method was determined.
     */
    void networkMethod(int id, String method);

    /**
     * The response arrived.
     *
     * @param code the HTTP response code
     * @param contentLength the reported content length (-1 when unknown)
     */
    void networkResponse(int id, int code, int contentLength);

    /**
     * The serialized component tree of the app's current form, sent in response
     * to an {@code "inspect"} control command. One node per line as
     * {@code id\tdepth\tlabel} where label is {@code ClassName #UIID [x,y wxh] "text"}.
     */
    void inspectorTree(String tree);

    /**
     * The property readout for the node the inspector selected (in response to
     * an {@code "inspectSelect:<id>"} control command). Multi-line text.
     */
    void inspectorDetail(String detail);

    /**
     * Periodic performance sample while the Performance Monitor is enabled.
     *
     * @param fps frames flushed in the last second
     * @param usedKb heap in use (KB)
     * @param totalKb heap allocated (KB)
     */
    void perfStats(int fps, int usedKb, int totalKb);
}
