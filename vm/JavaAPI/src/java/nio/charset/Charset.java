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
package java.nio.charset;

/**
 * Added this for Kotlin
 * @author shannah
 */
public class Charset implements Comparable<Charset> {

    private String name;
    private static final Charset UTF8 = new SimpleCharset("UTF-8");
    private static final Charset USASCII = new SimpleCharset("US-ASCII");
    private static final Charset ISO88591 = new SimpleCharset("ISO-8859-1");
    protected Charset(String canonicalName, String[] aliases) {
        name = canonicalName;
    }
    
    public int compareTo(Charset another) {
        return name.compareTo(another.name);
    }
    
    public String displayName() {
        return name;
    }
    
    public static Charset forName(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        String normalized = name.toUpperCase().replace('_', '-');
        if ("UTF-8".equals(normalized)) {
            return UTF8;
        }
        if ("US-ASCII".equals(normalized) || "ASCII".equals(normalized)) {
            return USASCII;
        }
        if ("ISO-8859-1".equals(normalized) || "ISO8859-1".equals(normalized)) {
            return ISO88591;
        }
        throw new UnsupportedOperationException("Charset.forName not implemented on this platform: " + name);
    }

    private static final class SimpleCharset extends Charset {
        private SimpleCharset(String canonicalName) {
            super(canonicalName, new String[0]);
        }
    }
    
}
