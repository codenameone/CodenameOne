/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.io;
/**
 * Signals that an end of file or end of stream has been reached unexpectedly during input.
 * This exception is mainly used by data input streams, which generally expect a binary file in a specific format, and for which an end of stream is an unusual condition. Most other input streams return a special value on end of stream.
 * Note that some input operations react to end-of-file by returning a distinguished value (such as -1) rather than by throwing an exception.
 * Since: JDK1.0, CLDC 1.0 See Also:DataInputStream, IOException
 */
public class EOFException extends java.io.IOException{
    /**
     * Constructs an EOFException with null as its error detail message.
     */
    public EOFException(){
         //TODO codavaj!!
    }

    /**
     * Constructs an EOFException with the specified detail message. The string s may later be retrieved by the
     * method of class java.lang.Throwable.
     * s - the detail message.
     */
    public EOFException(java.lang.String s){
         //TODO codavaj!!
    }

}
