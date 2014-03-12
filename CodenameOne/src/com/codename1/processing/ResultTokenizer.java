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

Derivative Revision History:

2012-03 - derivative work from original Sun source, added
	support for nested expressions to support new features added
	to Result class.
*/

package com.codename1.processing;

import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import java.util.Vector;

/**
 * Internal class, do not use.
 * 
 * Used internally by Result class apply() methods, this converts a path expression into a List of string tokens.
 *
 * @author Eric Coolman (2012-03 - derivative work from original Sun source).
 * 
 */
class ResultTokenizer {
    
    private final String expression;
    private final int length;
    
    private int pos;
    boolean predicate;
    
    ResultTokenizer(final String expr) {
        if (expr == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
        
        expression = expr;
        length = expression.length();
        pos = 0;
        predicate = false;
    }
    
    List tokenize(Map namespaceAliases) {
        final List tokens = new Vector();
        String tok;
        int i;
        for (pos = 0, tok = next(); !"".equals(tok); tok = next()) {
            if (namespaceAliases != null && ((i = tok.indexOf(':')) != -1)) {
                String mapto = (String)namespaceAliases.get(tok.substring(0, i));
                if (mapto != null) {
                    tok = mapto + tok.substring(i);
                }
            }
            tokens.add(tok);
        }
        return tokens;
    }
    
    /**
     * Handle the predicate expression within outermost brackets.  
     * This allows us to nest predicate expressions.  For example:
     * 
     * <code>
     * /result/address_component[/type[position() < 5]='locality']/long_name
     * </code>
     * The nested predicate in the above statement is 
     * 
     * <code>
     * /type[position() < 5]='locality'  
     * </code>
     * 
     * which is applied against the address_component node.
     * 
     * @param sbuf
     * @return full predicate expression
     */
    private String getPredicate(StringBuffer sbuf) {
    	int stack = 1;

    	for (int i = pos; i < length; i++) {
    		final char ch = expression.charAt(i);
    		if (isSubscript(ch)) {
            	if (ch == Result.ARRAY_START) {
            		stack++;
            	} else {
            		stack--;
            	}
            }
        	if (stack == 0) {
        		pos = i;
        		predicate = false;
        		break;
        	} else {
        		sbuf.append(ch);
        	}
        }
    	return sbuf.toString();
    }

    private String next() {
        final StringBuffer sbuf = new StringBuffer();
        
        if (pos >= length) {
            return sbuf.toString();
        }
        if (predicate) {
        	return getPredicate(sbuf);
        }
        final char del = expression.charAt(pos);
        if (isDelimiter(del)) {
        	// next token will be handled by
        	// getPredicate() instead of next().
        	if (del == Result.ARRAY_START) {
        		predicate = true;
        	} else if (isSeparator(del)) {
                while (isSeparator(expression.charAt(pos+1))) {
                	// handle 'decendants' token
                	sbuf.append(del);
                	pos++;
                }
        	}
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

    static boolean isSubscript(final char ch) {
        switch (ch) {
            case Result.ARRAY_START:
            case Result.ARRAY_END:
                return true;
        }
        return false;
    }
    
    static boolean isSeparator(final char ch) {
    	return (ch == Result.SEPARATOR);
    }
    
    static boolean isDelimiter(final char ch) {
    	return isSubscript(ch) || isSeparator(ch);
    }
}
