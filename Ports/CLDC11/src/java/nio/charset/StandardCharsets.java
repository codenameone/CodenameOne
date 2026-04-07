/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package java.nio.charset;

/// Minimal charset constants supported by CLDC11 stubs.
public final class StandardCharsets {
    private StandardCharsets() {
    }

    public static final Charset UTF_8 = new Charset("UTF-8", new String[0]);
    public static final Charset US_ASCII = new Charset("US-ASCII", new String[0]);
    public static final Charset ISO_8859_1 = new Charset("ISO-8859-1", new String[0]);
}
