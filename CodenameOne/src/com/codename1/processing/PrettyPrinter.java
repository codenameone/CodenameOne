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
package com.codename1.processing;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Private class, do not use.
 * 
 * An internal utility method used by toString() methods to produce a JSON document
 * from a given hashtable or vector.
 * 
 * @author Eric Coolman (2012-03 - derivative work from original Sun source).
 */
class PrettyPrinter {
    Hashtable myHashMap;
    
    private PrettyPrinter(Hashtable h) {
    	this.myHashMap = h;
    }
    
    public static String print(Hashtable h) {
    	return print(h, 2, 0);
    }

    public static String print(Vector v) {
    	return print(v, 2, 0);
    }

    static String print(Hashtable h, int indentFactor, int indent) {
    	PrettyPrinter printer = new PrettyPrinter(h);
    	return printer.toString(indentFactor, indent);
    }
    
    static String print(Vector v, int indentFactor, int indent) {
        int len = v.size();
        if (len == 0) {
            return "[]";
        }
        int i;
        StringBuffer sb = new StringBuffer("[");
        if (len == 1) {
            sb.append(valueToString(v.elementAt(0),
                    indentFactor, indent));
        } else {
            int newindent = indent + indentFactor;
            sb.append('\n');
            for (i = 0; i < len; i += 1) {
                if (i > 0) {
                    sb.append(",\n");
                }
                for (int j = 0; j < newindent; j += 1) {
                    sb.append(' ');
                }
                sb.append(valueToString(v.elementAt(i),
                        indentFactor, newindent));
            }
            sb.append('\n');
            for (i = 0; i < indent; i += 1) {
                sb.append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    	
    }

    /**
     * Get the number of keys stored in the Hashtable.
     *
     * @return The number of keys in the Hashtable.
     */
    public int length() {
        return this.myHashMap.size();
    }

    /**
     * Get an enumeration of the keys of the Hashtable.
     *
     * @return An iterator of the keys.
     */
    public Enumeration keys() {
        return this.myHashMap.keys();
    }

    /**
     * Make a prettyprinted JSON text of this Hashtable.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    public String toString(int indentFactor) {
        return toString(indentFactor, 0);
    }


    /**
     * Make a prettyprinted JSON text of this Hashtable.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    String toString(int indentFactor, int indent) {
        int          i;
        int          n = length();
        if (n == 0) {
            return "{}";
        }
        Enumeration keys = keys();
        StringBuffer sb = new StringBuffer("{");
        int          newindent = indent + indentFactor;
        Object       o;
        if (n == 1) {
            o = keys.nextElement();
            sb.append(quote(o.toString()));
            sb.append(": ");
            sb.append(valueToString(this.myHashMap.get(o), indentFactor,
                    indent));
        } else {
            while (keys.hasMoreElements()) {
                o = keys.nextElement();
                if (sb.length() > 1) {
                    sb.append(",\n");
                } else {
                    sb.append('\n');
                }
                for (i = 0; i < newindent; i += 1) {
                    sb.append(' ');
                }
                sb.append(quote(o.toString()));
                sb.append(": ");
                sb.append(valueToString(this.myHashMap.get(o), indentFactor,
                        newindent));
            }
            if (sb.length() > 1) {
                sb.append('\n');
                for (i = 0; i < indent; i += 1) {
                    sb.append(' ');
                }
            }
        }
        sb.append('}');
        return sb.toString();
    }



    /**
     * Make a prettyprinted JSON text of an object value.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
     static String valueToString(Object value, int indentFactor, int indent) {
        if (value == null || value.equals(null)) {
            return "null";
        }
        try {
	        if (value instanceof String) {
	        	return (String)value;
	        }
        } catch (Exception e) {
        	/* forget about it */
        }
         if (value instanceof Float || value instanceof Double ||
            value instanceof Byte || value instanceof Short || 
            value instanceof Integer || value instanceof Long) {
            return numberToString(value);
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Hashtable) {
            return print((Hashtable)value, indentFactor, indent);
        }
        if (value instanceof Vector) {
            return print((Vector)value, indentFactor, indent);
        }
        return quote(value.toString());
    }

     /**
      * Produce a string in double quotes with backslash sequences in all the
      * right places. A backslash will be inserted within </, allowing JSON
      * text to be delivered in HTML. In JSON text, a string cannot contain a
      * control character or an unescaped quote or backslash.
      * @param string A String
      * @return  A String correctly formatted for insertion in a JSON text.
      */
     public static String quote(String string) {
         if (string == null || string.length() == 0) {
             return "\"\"";
         }

         char         b;
         char         c = 0;
         int          i;
         int          len = string.length();
         StringBuffer sb = new StringBuffer(len + 4);
         String       t;

         sb.append('"');
         for (i = 0; i < len; i += 1) {
             b = c;
             c = string.charAt(i);
             switch (c) {
             case '\\':
             case '"':
                 sb.append('\\');
                 sb.append(c);
                 break;
             case '/':
                 if (b == '<') {
                     sb.append('\\');
                 }
                 sb.append(c);
                 break;
             case '\b':
                 sb.append("\\b");
                 break;
             case '\t':
                 sb.append("\\t");
                 break;
             case '\n':
                 sb.append("\\n");
                 break;
             case '\f':
                 sb.append("\\f");
                 break;
             case '\r':
                 sb.append("\\r");
                 break;
             default:
                 if (c < ' ') {
                     t = "000" + Integer.toHexString(c);
                     sb.append("\\u" + t.substring(t.length() - 4));
                 } else {
                     sb.append(c);
                 }
             }
         }
         sb.append('"');
         return sb.toString();
     }

     /**
      * Make a JSON text of an Object value. If the object has an
      * value.toJSONString() method, then that method will be used to produce
      * the JSON text. The method is required to produce a strictly
      * conforming text. If the object does not contain a toJSONString
      * method (which is the most common case), then a text will be
      * produced by the rules.
      * <p>
      * Warning: This method assumes that the data structure is acyclical.
      * @param value The value to be serialized.
      * @return a printable, displayable, transmittable
      *  representation of the object, beginning
      *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
      *  with <code>}</code>&nbsp;<small>(right brace)</small>.
      */
     static String valueToString(Object value) {
         if (value == null || value.equals(null)) {
             return "null";
         }
         if (value instanceof String) {
 	        	return (String)value;
         }
         if (value instanceof Float || value instanceof Double ||
             value instanceof Byte || value instanceof Short || 
             value instanceof Integer || value instanceof Long) {
             return numberToString(value);
         }
         if (value instanceof Boolean || value instanceof Hashtable ||
                 value instanceof Vector) {
             return value.toString();
         }
         return quote(value.toString());
     }
     
     static public String trimNumber(String s) {
         if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
             while (s.endsWith("0")) {
                 s = s.substring(0, s.length() - 1);
             }
             if (s.endsWith(".")) {
                 s = s.substring(0, s.length() - 1);
             }
         }
         return s;
     }

     /**
      * Produce a string from a Number.
      * @param  n A Number
      * @return A String.
      * @throws JSONException If n is a non-finite number.
      */
     static public String numberToString(Object n) {
         if (n == null) {
             return null;
         }
         return trimNumber(n.toString());
     }
}
