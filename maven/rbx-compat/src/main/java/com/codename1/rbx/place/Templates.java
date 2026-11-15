/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx.place;

import com.codename1.rbx.Engine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// The starter places the studio offers. Each is an original `.rbxlx`
/// shipped in this jar, written by `tools/gen-templates.py`.
public final class Templates {
    private static final String[] IDS = {"obby", "tycoon", "arena", "simulator", "racing"};

    private Templates() {
    }

    /// Template ids, in the order the studio lists them.
    public static String[] ids() {
        String[] out = new String[IDS.length];
        System.arraycopy(IDS, 0, out, 0, IDS.length);
        return out;
    }

    /// The place file of template `id`.
    public static byte[] bytes(String id) throws IOException {
        InputStream in = Templates.class.getResourceAsStream("/com/codename1/rbx/templates/" + id + ".rbxlx"); // NOPMD CloseResource - closed in the finally below
        if (in == null) {
            throw new IOException("No template " + id);
        }
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream(); // NOPMD CloseResource - a memory stream holds nothing to close
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                b.write(buf, 0, n);
            }
            return b.toByteArray();
        } finally {
            in.close();
        }
    }

    /// Loads template `id` into an empty engine.
    public static CompatReport load(Engine e, String id) throws IOException {
        return PlaceLoader.loadPlace(e, PlaceFile.read(bytes(id)));
    }
}
