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

package java.io;
/**
 * Signals that an I/O operation has been interrupted. An InterruptedIOException is thrown to indicate that an input or output transfer has been terminated because the thread performing it was terminated. The field bytesTransferred indicates how many bytes were successfully transferred before the interruption occurred.
 * Since: JDK1.0, CLDC 1.0 See Also:InputStream, OutputStream
 */
public class InterruptedIOException extends java.io.IOException{
    /**
     * Reports how many bytes had been transferred as part of the I/O operation before it was interrupted.
     */
    public int bytesTransferred;

    /**
     * Constructs an InterruptedIOException with null as its error detail message.
     */
    public InterruptedIOException(){
         //TODO codavaj!!
    }

    /**
     * Constructs an InterruptedIOException with the specified detail message. The string s can be retrieved later by the
     * method of class java.lang.Throwable.
     * s - the detail message.
     */
    public InterruptedIOException(java.lang.String s){
         super(s);
    }

}
