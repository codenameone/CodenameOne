/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.tools.resourcebuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Loader for {@code .properties} files that supports both UTF-8 and the legacy
 * ISO-8859-1 / {@code native2ascii} encoding.
 *
 * <p>This mirrors the behavior of {@code java.util.PropertyResourceBundle} as
 * updated in JDK&nbsp;9 (JEP&nbsp;226): the file is read first as UTF-8, and if
 * the bytes are not valid UTF-8 the loader falls back to ISO-8859-1 so existing
 * files produced by {@code native2ascii} (or otherwise stored in Latin-1) keep
 * working. Standard {@code \\uXXXX} escapes recognized by
 * {@link Properties#load(Reader)} are honored in either mode.</p>
 */
public final class PropertiesUtil {

    private PropertiesUtil() {}

    /**
     * Loads {@code file} into {@code props}, preferring UTF-8 and falling back
     * to ISO-8859-1 if the file is not valid UTF-8.
     */
    public static void loadUtf8WithFallback(File file, Properties props) throws IOException {
        byte[] data = Files.readAllBytes(file.toPath());
        loadUtf8WithFallback(data, props);
    }

    /**
     * Loads the contents of {@code in} into {@code props}, preferring UTF-8 and
     * falling back to ISO-8859-1 if the bytes are not valid UTF-8. The stream
     * is fully read but not closed.
     */
    public static void loadUtf8WithFallback(InputStream in, Properties props) throws IOException {
        loadUtf8WithFallback(readAll(in), props);
    }

    private static void loadUtf8WithFallback(byte[] data, Properties props) throws IOException {
        Properties decoded = new Properties();
        try (Reader r = new InputStreamReader(new ByteArrayInputStream(data),
                StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT))) {
            decoded.load(r);
        } catch (MalformedInputException | UnmappableCharacterException ex) {
            decoded = new Properties();
            try (Reader r = new InputStreamReader(new ByteArrayInputStream(data),
                    StandardCharsets.ISO_8859_1)) {
                decoded.load(r);
            }
        }
        for (String name : decoded.stringPropertyNames()) {
            props.setProperty(name, decoded.getProperty(name));
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int n;
        while ((n = in.read(tmp)) > 0) {
            buf.write(tmp, 0, n);
        }
        return buf.toByteArray();
    }
}
