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

import com.codename1.impl.charset.CharsetSupport;

/**
 * Added this for Kotlin
 */
public class Charset implements Comparable<Charset> {

    private String name;
    // Indexed by CharsetSupport's constants, which list every charset this
    // class library converts.
    private static final Charset[] CHARSETS = new Charset[CharsetSupport.COUNT];
    protected Charset(String canonicalName, String[] aliases) {
        name = canonicalName;
    }
    
    public int compareTo(Charset another) {
        return name.compareTo(another.name);
    }
    
    public final String name() {
        return name;
    }

    public String displayName() {
        return name;
    }
    
    /**
     * The charset {@code name} names, by its canonical name or any of the JDK's
     * aliases for it, compared without case. The names are CharsetSupport's,
     * the one list that String, the reader and writer and the URL codec read
     * too, so a charset found here is one all of them convert.
     */
    public static Charset forName(String name) {
        if (name == null) {
            throw new NullPointerException();
        }
        int charset;
        try {
            charset = CharsetSupport.lookup(name);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Charset.forName not implemented on this platform: " + name);
        }
        Charset c = CHARSETS[charset];
        if (c == null) {
            c = new SimpleCharset(CharsetSupport.name(charset));
            CHARSETS[charset] = c;
        }
        return c;
    }

    private static final class SimpleCharset extends Charset {
        private SimpleCharset(String canonicalName) {
            super(canonicalName, new String[0]);
        }
    }
    
}
