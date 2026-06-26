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
 * Dispatches native menu selections back into the Codename One universe that
 * pushed the menu (the simulator shell normally; the app itself in shell-less
 * pure mode). Registered alongside each setNativeMenu push.
 */
public interface MenuDispatcher {
    /**
     * Fires the Codename One command at the given menu index on that
     * universe's EDT.
     */
    void fireMenuCommand(int index);

    /**
     * Resolves a command label to its menu index - used by test drivers to
     * fire commands without hardcoding ordering.
     *
     * @return the index or -1
     */
    int indexOfLabel(String label);
}
