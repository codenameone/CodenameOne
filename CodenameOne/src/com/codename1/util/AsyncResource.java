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


import com.codename1.io.Util;
import com.codename1.ui.CN;
import static com.codename1.ui.CN.isEdt;
import java.util.Observable;
import java.util.Observer;

/**
 * A wrapper for an object that needs to be loaded asynchronously.  This can serve
 * as a handle for the object to be passed around irrespective of whether the 
 * object has finished loading.  Conceptually this is very similar to Futures and 
 * Promises.
 * @author shannah
 * @since 7.0
 */
public class AsyncResource<V> extends Observable  {
    private V value;
    private Throwable error;
    private SuccessCallback<V> successCallback;
    private SuccessCallback<Throwable> errorCallback;
    private boolean done;
    private boolean cancelled;
    

    /**
     * Cancels loading the resource.
     * @param mayInterruptIfRunning
     * @return True if the resource loading was cancelled.  False if the loading was already done.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (done) {
            return false;
        }
        if (!cancelled) {
            cancelled = true;
            done = true;
            error = new RuntimeException("Cancelled");
            setChanged();
        }
        return true;
    }

    /**
     * Wait for loading to complete.  If on EDT, this will use invokeAndBlock to safely
     * block until loading is complete.
     */
    public void waitFor() {
        try {
            get();
        } catch (Throwable t){}
    }
    

    /**
     * Gets the resource synchronously. This will wait until either the resource failed
     * with an exception, or the loading was canceled, or was done without error.
     * 
     * <p>If on edt, this uses invokeAndBlock to block safely.</p>
     * @return The wrapped resource.
     * @throws AsyncExecutionException if the resource failed with an error.  To get the actual error, use {@link Throwable#getCause() }.
     * 
     */
    public V get() {
        if (done && error == null) {
            return value;
        }
        if (done && error != null) {
            throw new AsyncExecutionException(error);
        }
        final boolean[] complete = new boolean[1];
        Observer observer = new Observer() { public void update(Observable obj,Object arg){
            if (isDone()) {
                complete[0] = true;
                synchronized(complete) {
                    complete.notify();
                }
            }
        }};
        addObserver(observer);
        while (!complete[0]) {
            if (isEdt()) {
                CN.invokeAndBlock(new Runnable(){public void run(){
                    synchronized(complete) {
                        Util.wait(complete);
                    }
                }});
            } else {
                synchronized(complete) {
                    Util.wait(complete);
                }
            }
        }
        deleteObserver(observer);
        if (error != null) {
            throw new AsyncExecutionException(error);
        }
        return value;
    }

    /**
     * Exception to wrap exceptions that are thrown during asynchronous execution.
     * This is thrown by {@link #get() } if the this resource failed with an exception.
     * 
     * <p>Call {@link AsyncExecutionException#getCause() } to get the original exception.</p>
     */
    public static class AsyncExecutionException extends RuntimeException {
        private Throwable cause;
        
        public AsyncExecutionException(Throwable cause) {
            super(cause.getMessage());
        }
        
        public Throwable getCause() {
            return cause;
        }
    }


    /**
     * Gets the resource if it is ready.  If it is not ready, then it will simply
     * return the provided defaultVal.
     * @param defaultVal
     * @return Either the resource value, or the provided default.
     */
    public V get(V defaultVal) {
        if (value != null) {
            return value;
        }
        return defaultVal;
    }


    /**
     * Checks if the resource loading was cancelled.
     * @return 
     */
    public boolean isCancelled() {
        return cancelled;
    }


    /**
     * Checks if the resource loading is done.  This will be true
     * even if the resource loading failed with an error.
     * @return 
     */
    public boolean isDone() {
        return done;
    }
    
    /**
     * Checks if the resource is ready.
     * @return 
     */
    public boolean isReady() {
        return done && error == null;
    }
    
    private class AsyncCallback<T> implements SuccessCallback<T> {
        private SuccessCallback<T> cb;
        private EasyThread t;
        private boolean edt;
        
        AsyncCallback(SuccessCallback<T> cb, EasyThread t) {
            this.cb = cb;
            this.t = t;
            this.edt = t == null && CN.isEdt();
        }

        @Override
        public void onSucess(final T value) {

            if (edt && !CN.isEdt()) {
                CN.callSerially(new Runnable() {
                    public void run() {
                        onSucess(value);
                    }
                });
                return;
            }
            if (t != null && !t.isThisIt()) {
                t.run(new Runnable() {

                    @Override
                    public void run() {
                        onSucess(value);
                    }
                });
                return;
                
            }
            
            cb.onSucess(value);
                
        }
        
        
    }
    
    /**
     * Runs the provided callback when the resource is ready.  
     * 
     * <p>If an {@link EasyThread} is provided, then the callback will be run on that
     * thread.  If an EasyThread is not provided, and this call is made on the EDT, then 
     * the callback will be run on the EDT.  Otherwise, the callback will occur on
     * whatever thread the {@link #complete(java.lang.Object) } call is called on.</p>
     * 
     * @param callback Callback to run when the resource is ready.
     * @param t Optional EasyThread on which the callback should be run.
     * @return Self for chaining
     */
    public AsyncResource<V> ready(final SuccessCallback<V> callback, EasyThread t) {
        if (done && error == null) {
            new AsyncCallback(callback, t).onSucess(value);
        } else {
            if (successCallback == null) {
                successCallback = new AsyncCallback<V>(callback, t);
            } else {
                final SuccessCallback<V> oldCallback = successCallback;
                successCallback = new AsyncCallback<V>(new SuccessCallback<V>(){ public void onSucess(V res){
                    oldCallback.onSucess(res);
                    callback.onSucess(res);
                }}, t);
            }
        }
        return this;
    }
    
    /**
     * Runs the provided callback when the resource is ready.
     * 
     * <p>If this call is made on the EDT, then the callback will be run on the EDT.
     * Otherwise, it will be run on whatever thread the complete() methdo is invoked on.</p>
     * 
     * @param callback The callback to be run when the resource is ready.
     * @return Self for chaining.
     */
    public AsyncResource<V> ready(SuccessCallback<V> callback) {
        return ready(callback, null);
    }
    
    /**
     * Sets callback to run if an error occurs.
     * <p>If an {@link EasyThread} is provided, then the callback will be run on that
     * thread.  If an EasyThread is not provided, and this call is made on the EDT, then 
     * the callback will be run on the EDT.  Otherwise, the callback will occur on
     * whatever thread the {@link #complete(java.lang.Object) } call is called on.</p>
     * @param callback Callback to run on error.
     * @param t Optional EasyThread to run callback on.
     * @return Self for chaining.
     */
    public AsyncResource<V> except(final SuccessCallback<Throwable> callback, EasyThread t) {
        if (done && error != null) {
            callback.onSucess(error);
        } else {
            if (errorCallback == null) {
                errorCallback = new AsyncCallback<Throwable>(callback, t);
            } else {
                final SuccessCallback<Throwable> oldErrorCallback = errorCallback;
                errorCallback = new AsyncCallback<Throwable>(new SuccessCallback<Throwable>(){ public void onSucess(Throwable res){
                    oldErrorCallback.onSucess(res);
                    callback.onSucess(res);
                }}, t);
            }
            
        }
        return this;
    }
    
    /**
     * Sets callback to run if an error occurs.  If this call is made on the EDT,
     * then the callback will be run on the EDT.  Otherwise it will be run on whatever
     * thread the error() method is invoked on.
     * @param callback The callback to run in case of error.
     * @return 
     */
    public AsyncResource<V> except(SuccessCallback<Throwable> callback) {
        return except(callback, null);
    }
    
    /**
     * Sets the resource value.  This will trigger the ready callbacks to be run.
     * @param value The value to set for the resource.
     */
    public void complete(final V value) {
        this.value = value;
        done = true;
        setChanged();
        notifyObservers();
        if (successCallback != null) {
            successCallback.onSucess(value);
        }
    }
    
    /**
     * Sets the error for this resource in the case that it could not be loaded.  This will trigger
     * the error callbacks.
     * @param t 
     */
    public void error(Throwable t) {
        this.error = t;
        done = true;
        setChanged();
        notifyObservers();
        if (errorCallback != null) {
            errorCallback.onSucess(error);
        }
    }
    
}