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
package com.codename1.util;

import java.util.Vector;

/**
 * This is a String Utility class
 * @author Chen
 */
public class StringUtil {


    /**
     * This method replaces all occurrences of the pattern with the 
     * replacement String
     * 
     * @param source the String source
     * @param pattern String to replace in the source
     * @param replace replacement String
     * @return 
     */
    public static String replaceAll(String source, String pattern, String replace) {
        StringBuffer sb = new StringBuffer();
        int idx = 0;
        String workingSource = source;
        idx = workingSource.indexOf(pattern);
        if(idx == -1){
            return source;
        }
        
        while (idx != -1) {
            sb.append(workingSource.substring(0, idx));
            sb.append(replace);
            workingSource = workingSource.substring(idx + pattern.length());
            idx = workingSource.indexOf(pattern);            
        }
        sb.append(workingSource);

        return sb.toString();
    }
    
    /**
     * Breaks a String to multiple strings.
     * 
     * @param source the String to break
     * @param separator the pattern to search and break.
     * @return a Vector of Strings
     */
    public static Vector tokenizeString(String source, char separator) {
        Vector tokenized = new Vector();
        int len = source.length();
        boolean lastSeparator = false;
        StringBuffer buf = new StringBuffer();
        for(int iter = 0 ; iter < len ; iter++) {
            char current = source.charAt(iter);
            if(current == separator) {
                if(lastSeparator) {
                    buf.append(separator);
                    lastSeparator = false;
                    continue;
                }
                lastSeparator = true;
                if(buf.length() > 0) {
                    tokenized.addElement(buf.toString());
                    buf = new StringBuffer();
                }
            } else {
                lastSeparator = false;
                buf.append(current);
            }
        }
        if(buf.length() > 0) {
            tokenized.addElement(buf.toString());
        }
        return tokenized;
    }
    

    /**
     * Breaks a String to multiple strings (similar to string tokenizer)
     * 
     * @param source the String to break
     * @param separator the characters that can be used to search and break.
     * @return a Vector of Strings
     */
    public static Vector tokenizeString(String source, String separator) {
        if(separator.length() == 1) {
            // slightly faster
            return tokenizeString(source, separator.charAt(0));
        }
        Vector tokenized = new Vector();
        int len = source.length();
        StringBuffer buf = new StringBuffer();
        for(int iter = 0 ; iter < len ; iter++) {
            char current = source.charAt(iter);
            if(separator.indexOf(current) > -1) {
                if(buf.length() > 0) {
                    tokenized.addElement(buf.toString());
                    buf = new StringBuffer();
                }
            } else {
                buf.append(current);
            }
        }
        if(buf.length() > 0) {
            tokenized.addElement(buf.toString());
        }
        return tokenized;
    }
}
