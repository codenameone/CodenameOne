/*
Copyright (c) 2007, Sun Microsystems, Inc.

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in
      the documentation and/or other materials provided with the
      distribution.
    * Neither the name of Sun Microsystems, Inc. nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.codename1.io.util;

import java.util.Vector;

/**
 * Used internally by Result class apply() methods, this converts a path expression into a Vector of string tokens.
 *
 */
class ResultTokenizer {
    
    private final String expression;
    private final int length;
    
    private int pos;
    
    ResultTokenizer(final String expr) {
        if (expr == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        
        expression = expr;
        length = expression.length();
        pos = 0;
    }
    
    Vector tokenize() {
        final Vector tokens = new Vector();
        String tok;
        for (pos = 0, tok = next(); !"".equals(tok); tok = next()) {
            tokens.addElement(tok);
        }
        return tokens;
    }
    
    private String next() {
        final StringBuffer sbuf = new StringBuffer();
        
        if (pos >= length) {
            return sbuf.toString();
        }
        
        final char del = expression.charAt(pos);
        if (isDelimiter(del)) {
            pos++;
            sbuf.append(del);
            return sbuf.toString();
        }
        
        for (int i = pos; i < length; i++) {
            final char ch = expression.charAt(i);
            if (isDelimiter(ch)) {
                pos = i;
                return sbuf.toString();
            } else {
                sbuf.append(ch);
            }
        }

        pos = length;
        return sbuf.toString();
    }
    
    static boolean isDelimiter(final char ch) {
        switch (ch) {
            case Result.SEPARATOR:
            case Result.ARRAY_START:
            case Result.ARRAY_END:
                return true;
        }
        return false;
    }
}
