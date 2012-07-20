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
package com.codename1.system;

/**
 * Invoked to indicate that an exception occurred, it is up to the developer to decide
 * whether to send the device log to the server by invoking Log.sendLog(). Notice
 * that sending a log only works for paid accounts. This interface should be registered
 * with the Display class.
 * <p>Notice that exceptions will only be reported for threads created by Codename One
 * using the API's within the Display class, this will not work for exceptions within
 * threads that are created by the new Thread() API.
 *
 * @author Shai Almog
 */
public interface CrashReport {
    /**
     * Callback for an exception that was not handled by the developer
     * @param t the exception
     */
    public void exception(Throwable t);
}
