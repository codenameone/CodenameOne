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
package com.codename1.wordprocessing;

import com.codename1.impl.xml.XmlOut;
import com.codename1.impl.xml.XmlScanner;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/// The `.cn1doc` XML of a list of blocks, for formats that hold document text inside their own
/// files. A slide's text box is a list of blocks, and `.cn1slides` stores it in exactly the
/// vocabulary `.cn1doc` stores a document body in, so both read and write it the same way.
///
/// Not an end-user API: it works on the internal XML reader and writer of the office formats.
public final class BlockXml {
    private BlockXml() {
    }

    /// Writes blocks as `.cn1doc` elements.
    ///
    /// #### Parameters
    ///
    /// - `x`: the writer, inside an element of the caller's
    ///
    /// - `blocks`: the blocks
    ///
    /// #### Throws
    ///
    /// - `IOException`: when writing fails
    public static void write(XmlOut x, List<Block> blocks) throws IOException {
        NativeFormat.blocks(x, blocks);
    }

    /// Reads the block whose start tag the scanner is on.
    ///
    /// #### Parameters
    ///
    /// - `x`: the scanner, on a start tag
    ///
    /// - `out`: receives the block
    ///
    /// - `ids`: the ids read so far, shared by everything in one file; a repeated id is an
    ///   error
    ///
    /// #### Returns
    ///
    /// false when the element is not a block, and the caller should skip it
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the block is damaged
    public static boolean read(XmlScanner x, List<Block> out, Set<String> ids) throws IOException {
        return NativeFormat.block(x, out, ids);
    }
}
