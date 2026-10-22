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
package java.util.concurrent;

/// Thrown by [Future#get] when the work failed; the cause is what it threw.
public class ExecutionException extends Exception {
    /// An exception with no message or cause.
    public ExecutionException() { }

    /// An exception with `message`.
    ///
    /// @param message the detail message
    public ExecutionException(String message) { super(message); }

    /// An exception with `message` and the failure that caused it.
    ///
    /// @param message the detail message
    /// @param cause what the work threw
    public ExecutionException(String message, Throwable cause) { super(message, cause); }

    /// An exception wrapping the failure that caused it.
    ///
    /// @param cause what the work threw
    public ExecutionException(Throwable cause) { super(cause); }
}
