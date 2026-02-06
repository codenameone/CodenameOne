/*
 * Copyright (c) 2012, Eric Coolman, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.processing;

import com.codename1.io.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/// Internal class, do not use.
///
/// A DOM accessor implementation for working with JSON content.
///
/// @author Eric Coolman
class JSONContent extends MapContent {
    /// Construct from a parsed JSON dom
    ///
    /// #### Parameters
    ///
    /// - `content`: a parsed JSON dom
    public JSONContent(Map content) {
        super(content);
    }

    /// Construct from a JSON string
    ///
    /// #### Parameters
    ///
    /// - `content`: a JSON string
    ///
    /// #### Throws
    ///
    /// - `IOException`: on error reading/parsing string
    public JSONContent(String content) throws IOException {
        this(com.codename1.io.Util.getReader(new ByteArrayInputStream(com.codename1.util.StringUtil.getBytes(content))));
    }

    /// Construct from a JSON input stream
    ///
    /// #### Parameters
    ///
    /// - `content`: a JSON input stream
    ///
    /// #### Throws
    ///
    /// - `IOException`: on error reading/parsing the stream
    public JSONContent(InputStream content) throws IOException {
        this(new JSONParser().parse(com.codename1.io.Util.getReader(content)));
    }

    /// Construct from a JSON input stream
    ///
    /// #### Parameters
    ///
    /// - `content`: a JSON reader
    ///
    /// #### Throws
    ///
    /// - `IOException`: on error reading/parsing the stream
    public JSONContent(Reader content) throws IOException {
        this(new JSONParser().parse(content));
    }
}
