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
package com.codename1.util;

import com.codename1.ui.Display;

/**
 * A utility class for calling {@link Callback}s on the EDT.
 * @author shannah
 */
public class CallbackDispatcher<T> implements Runnable {
    private SuccessCallback<T> success;
    private FailureCallback<T> failure;
    private T arg;
    private Throwable error;
    
    private CallbackDispatcher(SuccessCallback<T> success, T arg) {
        this.success = success;
        this.arg = arg;
    }
    
    private CallbackDispatcher(FailureCallback failure, Throwable error) {
        this.failure = failure;
        this.error = error;
    }
    
    private CallbackDispatcher() {
        
    }
    
    public void run() {
        if (success != null) {
            success.onSucess(arg);
        } else if (failure != null) {
            failure.onError(failure, error, 0, error.getMessage());
        }
    }
    
    /**
     * Calls the given callback's {@link Callback#onSucess(java.lang.Object) } method, passing the supplied arg as
     * a parameter.  This method guarantees that onSuccess() will be called on the EDT.  If it is already running
     * on the EDT, it will just call it directly.  Otherwise it will wrap it in {@link Display#callSerially(java.lang.Runnable) }.
     * 
     * @param <T> The type of the callback.
     * @param success The success callback to be called.
     * @param arg The argument to pass to the success callback.
     */
    public static <T> void dispatchSuccess(SuccessCallback<T> success, T arg) {
        if (Display.getInstance().isEdt()) {
            success.onSucess(arg);
        } else {
            CallbackDispatcher<T> dispatcher = new CallbackDispatcher<T>(success, arg);
            Display.getInstance().callSerially(dispatcher);
        }
    }
    
    /**
     * Calls the given callback's {@link Callback#onError(java.lang.Object, java.lang.Throwable, int, java.lang.String) } method, passing the supplied error as
     * a parameter.  This method guarantees that onError() will be called on the EDT.  If it is already running
     * on the EDT, it will just call it directly.  Otherwise it will wrap it in {@link Display#callSerially(java.lang.Runnable) }.
     * 
     * @param failure The failure callback to be called.
     * @param error The error to pass to the callback
     */
    public static void dispatchError(FailureCallback failure, Throwable error) {
        if (Display.getInstance().isEdt()) {
            failure.onError(failure, error, 0, error.getMessage());
        } else {
            CallbackDispatcher dispatcher = new CallbackDispatcher(failure, error);
            Display.getInstance().callSerially(dispatcher);
        }
    }
}
