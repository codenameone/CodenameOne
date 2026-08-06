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

package java.lang;

import java.io.PrintWriter;

/**
 * The Throwable class is the superclass of all errors and exceptions in the Java language. Only objects that are instances of this class (or of one of its subclasses) are thrown by the Java Virtual Machine or can be thrown by the Java throw statement. Similarly, only this class or one of its subclasses can be the argument type in a catch clause.
 * Instances of two subclasses, Error and Exception, are conventionally used to indicate that exceptional situations have occurred. Typically, these instances are freshly created in the context of the exceptional situation so as to include relevant information (such as stack trace data).
 * By convention, class Throwable and its subclasses have two constructors, one that takes no arguments and one that takes a String argument that can be used to produce an error message.
 * A Throwable class contains a snapshot of the execution stack of its thread at the time it was created. It can also contain a message string that gives more information about the error.
 * Here is one example of catching an exception:
 * Since: JDK1.0, CLDC 1.0
 */
public class Throwable{
    private String message;
    private Throwable cause;
    private String stack;
    private java.util.List<Throwable> suppressed;
    private StackTraceElement[] parsedStack;
    private boolean stackParsed;
    
    
    /**
     * Constructs a new Throwable with null as its error message string.
     */
    public Throwable(){
    }

    public Throwable(Throwable cause) {
        this.cause = cause;
        this.message = cause == null ? null : cause.toString();
    }
    
    /**
     * @deprecated DO NOT USE THIS METHOD, its here just to get the compiler working and isn't intended for use
     */
    public Throwable initCause(Throwable cause) {
        this.cause = cause;
        return this;
    }
    
    public Throwable getCause() {
        return cause;
    }
    
    /**
     * Constructs a new Throwable with the specified error message.
     * message - the error message. The error message is saved for later retrieval by the
     * method.
     */
    public Throwable(java.lang.String message){
        this.message = message;
    }
    
    public Throwable(java.lang.String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }
    

    /**
     * Returns the error message string of this Throwable object.
     */
    public java.lang.String getMessage(){
        return message;
    }

    /**
     * Invoked from native!
     */
    private native void fillInStack();
    private native String getStack();
    
    /**
     * Prints this Throwable and its backtrace to the standard error stream. This method prints a stack trace for this Throwable object on the error output stream that is the value of the field System.err. The first line of output contains the result of the
     * method for this object.
     * The format of the backtrace information depends on the implementation.
     */
    public void printStackTrace(){
        System.out.println(stack);
        if (cause != null) {
            System.out.println("Caused by ");
            cause.printStackTrace();
        }
    }
    
    public void printStackTrace(java.io.PrintStream s) {
        s.println(stack);
        if (cause != null) {
            s.println("Caused by ");
            cause.printStackTrace(s);
        }
    }

    public void printStackTrace(PrintWriter s) {
        s.println(stack);
        if (cause != null) {
            s.println("Caused by ");
            cause.printStackTrace(s);
        }
    }
    
    
    public StackTraceElement[] getStackTrace() {
        if(!stackParsed) {
            parsedStack = parseStackString(stack);
            stackParsed = true;
        }
        if(parsedStack == null || parsedStack.length == 0) {
            return new StackTraceElement[0];
        }
        StackTraceElement[] copy = new StackTraceElement[parsedStack.length];
        System.arraycopy(parsedStack, 0, copy, 0, parsedStack.length);
        return copy;
    }

    public void setStackTrace(StackTraceElement[] el) {
        if(el == null) {
            throw new NullPointerException();
        }
        StackTraceElement[] copy = new StackTraceElement[el.length];
        for(int i = 0 ; i < el.length ; i++) {
            if(el[i] == null) {
                throw new NullPointerException();
            }
            copy[i] = el[i];
        }
        parsedStack = copy;
        stackParsed = true;
    }

    /**
     * Parses the pre-rendered stack string produced by the native getStack() into
     * structured frames. The format emitted on the C targets (see
     * nativeMethods.m java_lang_Throwable_getStack) is a class-name header line
     * followed by one "    at &lt;fqcn&gt;.&lt;method&gt;:&lt;line&gt;" line per frame.
     *
     * On the ParparVM JavaScript port the same field instead holds a JavaScript
     * engine's Error().stack, whose frames carry '(', '/' or '@' -- characters a
     * Java class or method name never contains. We reject the whole parse in that
     * case (returning no frames, the historical behaviour) rather than fabricate
     * bogus frames from a foreign format. Parsing is indexOf-based on purpose: it
     * runs while another failure is being reported, so it avoids regex and never
     * throws.
     */
    private static StackTraceElement[] parseStackString(String s) {
        if(s == null || s.length() == 0) {
            return new StackTraceElement[0];
        }
        java.util.ArrayList<StackTraceElement> frames = new java.util.ArrayList<StackTraceElement>();
        int pos = 0;
        int len = s.length();
        while(pos < len) {
            String line;
            int nl = s.indexOf('\n', pos);
            if(nl < 0) {
                line = s.substring(pos);
                pos = len;
            } else {
                line = s.substring(pos, nl);
                pos = nl + 1;
            }
            // Only "    at ..." lines are frames; the class-name header and any
            // blank line are skipped.
            if(line.indexOf("    at ") != 0) {
                continue;
            }
            String body = line.substring(7);
            // Any of these characters means this is not the ParparVM text format
            // (it is a JavaScript Error().stack, whose frames use URLs, parens or
            // '@'). Bail on the whole trace rather than emit a made-up frame.
            if(body.indexOf('(') >= 0 || body.indexOf('/') >= 0
                    || body.indexOf('@') >= 0 || body.indexOf(' ') >= 0) {
                return new StackTraceElement[0];
            }
            int colon = body.lastIndexOf(':');
            if(colon < 0) {
                continue;
            }
            int dot = body.lastIndexOf('.', colon - 1);
            if(dot < 0) {
                continue;
            }
            String cls = body.substring(0, dot);
            String method = body.substring(dot + 1, colon);
            if(cls.length() == 0 || method.length() == 0) {
                continue;
            }
            int lineNumber = parseLineNumber(body, colon + 1);
            // Synthesize a source file name from the simple class name so
            // isNativeMethod() (fileName == null) stays false -- ParparVM does not
            // carry the original source file, so this is best-effort, not authoritative.
            String fileName = simpleClassName(cls) + ".java";
            frames.add(new StackTraceElement(cls, method, fileName, lineNumber));
        }
        return frames.toArray(new StackTraceElement[frames.size()]);
    }

    private static int parseLineNumber(String s, int from) {
        int len = s.length();
        int i = from;
        boolean negative = false;
        if(i < len && s.charAt(i) == '-') {
            negative = true;
            i++;
        }
        int value = 0;
        boolean any = false;
        for(; i < len ; i++) {
            char c = s.charAt(i);
            if(c < '0' || c > '9') {
                break;
            }
            value = value * 10 + (c - '0');
            any = true;
        }
        if(!any) {
            return -1;
        }
        return negative ? -value : value;
    }

    private static String simpleClassName(String fqcn) {
        int d = fqcn.lastIndexOf('.');
        String simple = d < 0 ? fqcn : fqcn.substring(d + 1);
        int dollar = simple.indexOf('$');
        if(dollar > 0) {
            simple = simple.substring(0, dollar);
        }
        return simple;
    }

    /**
     * Returns a short description of this Throwable object. If this Throwable object was
     * with an error message string, then the result is the concatenation of three strings: The name of the actual class of this object ": " (a colon and a space) The result of the
     * method for this object If this Throwable object was
     * with no error message string, then the name of the actual class of this object is returned.
     */
    public java.lang.String toString(){
        return getClass().getName() + (message != null ? (": " + message):"");
    }
    
    public final void addSuppressed(Throwable exception){
        if (exception == this) {
            throw new IllegalArgumentException("Throwable cannot suppress itself");
        }
        if (exception == null) {
            throw new NullPointerException("null exception cannot be added suppressed");
        }
        if (suppressed == null) {
            suppressed = new java.util.ArrayList<Throwable>();
        }
        suppressed.add(exception);
    }
    public final Throwable[] getSuppressed() {
        if (suppressed == null) {
            return new Throwable[0];
        }
        return suppressed.toArray(new Throwable[suppressed.size()]);
    }
    
    public String getLocalizedMessage() {
        return message;
    }
    
    

}
