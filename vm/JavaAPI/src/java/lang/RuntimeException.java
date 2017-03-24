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
/**
 * RuntimeException is the superclass of those exceptions that can be thrown during the normal operation of the Java Virtual Machine.
 * A method is not required to declare in its throws clause any subclasses of RuntimeException that might be thrown during the execution of the method but not caught.
 * Since: JDK1.0, CLDC 1.0
 */
public class RuntimeException extends java.lang.Exception{
    /**
     * Constructs a RuntimeException with no detail message.
     */
    public RuntimeException(){
    }

    /**
     * Constructs a RuntimeException with the specified detail message.
     * s - the detail message.
     */
    public RuntimeException(java.lang.String s){
         super(s);
    }
    
    public RuntimeException(Throwable cause) {
        super(cause);
    }
    
    public RuntimeException(java.lang.String s, Throwable cause) {
        super(s, cause);
    }

}
