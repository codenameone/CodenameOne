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

package com.codename1.ant;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

/**
 *
 * @author shai
 */
public class SortedProperties extends Properties {
    /**
     * Stores properties to the specified {@code OutputStream}, using ISO-8859-1.
     * See "<a href="#character_encoding">Character Encoding</a>".
     *
     * @param out the {@code OutputStream}
     * @param comment an optional comment to be written, or null
     * @throws IOException
     * @throws ClassCastException if a key or value is not a string
     */
    @Override
    public synchronized void store(OutputStream out, String comment) throws IOException {
        store(new OutputStreamWriter(out, "UTF-8"), comment);
    }

    /**
     * Stores the mappings in this {@code Properties} object to {@code out},
     * putting the specified comment at the beginning.
     *
     * @param writer the {@code Writer}
     * @param comment an optional comment to be written, or null
     * @throws IOException
     * @throws ClassCastException if a key or value is not a string
     * @since 1.6
     */
    @Override
    public synchronized void store(Writer writer, String comment) throws IOException {
        if (comment != null && comment.length() > 0) {
            writer.write("#");
            writer.write(comment);
            writer.write("\n");
            writer.write("#");
            writer.write(new Date().toString());
            writer.write("\n");
        }

        StringBuilder sb = new StringBuilder(200);
        ArrayList<String> k = new ArrayList<String>();
        for(Object kk : keySet()) {
            k.add((String)kk);
        }
        k.sort(String.CASE_INSENSITIVE_ORDER);
        for (String key : k) {
            dumpString(sb, key, true);
            sb.append('=');
            dumpString(sb, (String) get(key), false);
            sb.append("\n");
            writer.write(sb.toString());
            sb.setLength(0);
        }
        writer.flush();
    }

    private void dumpString(StringBuilder buffer, String string, boolean key) {
        int i = 0;
        if (!key && i < string.length() && string.charAt(i) == ' ') {
            buffer.append("\\ ");
            i++;
        }
        int slen = string.length();
        for (; i < slen; i++) {
            char ch = string.charAt(i);
            switch (ch) {
            case '\t':
                buffer.append("\\t");
                break;
            case '\n':
                buffer.append("\\n");
                break;
            case '\f':
                buffer.append("\\f");
                break;
            case '\r':
                buffer.append("\\r");
                break;
            default:
                if ("\\#!=:".indexOf(ch) >= 0 || (key && ch == ' ')) {
                    buffer.append('\\');
                }
                if (ch >= ' ' && ch <= '~') {
                    buffer.append(ch);
                } else {
                    String hex = Integer.toHexString(ch);
                    buffer.append("\\u");
                    int hlen = hex.length();
                    for (int j = 0; j < 4 - hlen; j++) {
                        buffer.append("0");
                    }
                    buffer.append(hex);
                }
            }
        }
    }
}
