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
package com.codename1.backend;

/// The Java SE twin of the backend's board access. A JVM cannot reach the
/// kernel's GPIO, I2C or SPI interfaces without native code, so on this arm
/// nothing is installed and `Hardware.getInstance()` keeps answering the
/// unsupported default: a server developed on a laptop runs, and reports no
/// buses, exactly as the client simulator does when it is not a simulator.
public final class BoardHardware {
    private BoardHardware() {
    }

    /// Does nothing on the JVM; the translated binary installs the kernel
    /// implementation here.
    public static void install() {
    }
}
