/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
/// Signals that an unexpected exception has occurred in a static initializer.
///
/// Present so an interpreted class initializer can fail the way Java says it
/// fails: the first touch of a class whose `<clinit>` threw reports this, and
/// every touch after it reports NoClassDefFoundError. Without the type, a
/// pushed program's `catch (ExceptionInInitializerError e)` names a class the
/// device does not have.
public class ExceptionInInitializerError extends java.lang.LinkageError {
    private java.lang.Throwable exception;

    /// Constructs an ExceptionInInitializerError with no detail message.
    public ExceptionInInitializerError(){
    }

    /// Constructs an ExceptionInInitializerError with the specified detail message.
    /// s - the detail message.
    public ExceptionInInitializerError(java.lang.String s){
        super(s);
    }

    /// Constructs an ExceptionInInitializerError for the given throwable.
    /// thrown - the exception the initializer threw.
    public ExceptionInInitializerError(java.lang.Throwable thrown){
        this.exception = thrown;
    }

    /// The exception the class initializer threw, or null.
    public java.lang.Throwable getException(){
        return exception;
    }

    /// Same as getException(), for code written against the Throwable API.
    public java.lang.Throwable getCause(){
        return exception;
    }
}
