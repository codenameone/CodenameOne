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
package com.codenameone.examples.brickyard.rbx.place;

import java.util.ArrayList;
import java.util.List;

/// A whole place (`.rbxl`, `.rbxlx`) or model (`.rbxm`, `.rbxmx`) file: its
/// top-level instances. In a place those are the services; in a model, the
/// objects the model holds.
public final class PlaceFile {
    public final List<PlaceNode> roots = new ArrayList<PlaceNode>();
    /// Everything a reader could not decode, by what it is.
    public final List<String> readerNotes = new ArrayList<String>();

    /// Every node of the file, depth first.
    public List<PlaceNode> all() {
        List<PlaceNode> out = new ArrayList<PlaceNode>();
        for (PlaceNode r : roots) {
            r.walk(out);
        }
        return out;
    }

    /// Reads either format, telling them apart by the binary signature.
    public static PlaceFile read(byte[] data) {
        if (RbxlReader.isBinary(data)) {
            return RbxlReader.read(data);
        }
        return RbxlxReader.read(data);
    }
}
