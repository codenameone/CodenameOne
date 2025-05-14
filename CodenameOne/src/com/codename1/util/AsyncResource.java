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


import com.codename1.annotations.Async;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.util.promise.Promise;

import static com.codename1.ui.CN.invokeAndBlock;
import static com.codename1.ui.CN.isEdt;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

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
    private final Object lock = new Object();
    

    @Async.Schedule
    public AsyncResource() {
        
    }
    
    /**
     * Cancels loading the resource.
     * @param mayInterruptIfRunning
     * @return True if the resource loading was cancelled.  False if the loading was already done.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean changed = false;
        synchronized(lock) {
            if (done) {
                return false;
            }
            if (!cancelled) {
                cancelled = true;
                done = true;
                error = new CancellationException();
                changed = true;
            }
        }
        if (changed) {
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
     * 
     
     * @return The wrapped resource.
     * @throws AsyncExecutionException if the resource failed with an error.  To get the actual error, use {@link Throwable#getCause() }.
     * 
     */
    public V get() {
        try {
            return get(-1);
        } catch (InterruptedException ex) {
            // This should never happen
            throw new RuntimeException("Interrupted exception occurred, but this should never happen.  Likely programming error.");
        }
    }
    
    /**
     * Gets the resource synchronously. This will wait until either the resource failed
     * with an exception, or the loading was canceled, or was done without error.
     * 
     * <p>If on edt, this uses invokeAndBlock to block safely.</p>
     * 
     * @param timeout Timeout
     * @return The wrapped resource.
     * @throws AsyncExecutionException if the resource failed with an error.  To get the actual error, use {@link Throwable#getCause() }.
     * @throws InterruptedException if timeout occurs.
     * 
     */
    public V get(final int timeout) throws InterruptedException {
        final long startTime = (timeout > 0) ? System.currentTimeMillis() : 0;

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
            if (timeout > 0 && System.currentTimeMillis() > startTime + timeout) {
                throw new InterruptedException("Timeout occurred in get()");
            }
            if (isEdt()) {
                CN.invokeAndBlock(new Runnable(){public void run(){
                    synchronized(complete) {
                        if (timeout > 0) {
                            Util.wait(complete, (int)Math.max(1, timeout - (System.currentTimeMillis() - startTime)));
                        } else {
                            Util.wait(complete);
                        }
                    }
                }});
            } else {
                synchronized(complete) {
                    if (timeout > 0) {
                        Util.wait(complete, (int)Math.max(1, timeout - (System.currentTimeMillis() - startTime)));
                    } else {
                        Util.wait(complete);
                    }
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
            this.cause = cause;
        }
        
        public Throwable getCause() {
            return cause;
        }
        
        /**
         * Returns true if this exception wraps a {@link CancellationException}, or another
         * AsyncExecutionException that has {@link #isCancelled() } true.
         * @return True if this exception was caused by cancelling an AsyncResource.
         * @since 7.0
         */
        public boolean isCancelled() {
            if (cause != null && cause.getClass() == CancellationException.class) {
                return true;
            }
            if (cause != null && cause instanceof AsyncExecutionException) {
                return ((AsyncExecutionException)cause).isCancelled();
            }
            return false;
        }
        
    }

    /**
     * Returns true if the provided throwable was caused by a cancellation of an AsyncResource.
     * @param t The exception to check for a cancellation.
     * @return True if the exception was caused by cancelling an AsyncResource.
     * @since 7.0
     */
    public static boolean isCancelled(Throwable t) {
        if (t == null) {
            return false;
        }
        if (t instanceof AsyncExecutionException) {
            return ((AsyncExecutionException)t).isCancelled();
        } else if (t.getClass() == CancellationException.class) {
            return true;
        }
        return false;
    }
    
    /**
     * Exception thrown when the AsyncResource is cancelled.  Use {@link AsyncResource#isCancelled(java.lang.Throwable) 
     * to test a particular exception to see if it resulted from cancelling an AsyncResource as this will
     * return turn true if the exception itself is a CancellationException, or if the exception was caused by
     * a CancellationException.
     * 
     * @since 7.0
     * @see #isCancelled(java.lang.Throwable) 
     * 
     */
    public static class CancellationException extends RuntimeException {
        public CancellationException() {
            super("Cancelled");
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
        AsyncCallback runImmediately = null;
        synchronized(lock) {
            if (done && error == null) {
                runImmediately = new AsyncCallback(callback, t);
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
            
        }
        if (runImmediately != null) {
            runImmediately.onSucess(value);
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
        AsyncCallback runImmediately = null;
        synchronized(lock) {
            if (done && error != null) {
                runImmediately = new AsyncCallback<Throwable>(callback, t);
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
        }
        if (runImmediately != null) {
            runImmediately.onSucess(error);
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
    @Async.Execute
    public void complete(final V value) {
        SuccessCallback cb = null;
        synchronized(lock) {
            this.value = value;
            done = true;
            if (successCallback != null) {
                cb = successCallback;
            }
        }
        
        setChanged();
        notifyObservers();
        if (cb != null) {
            cb.onSucess(value);
        }
    }
    
    /**
     * Sets the error for this resource in the case that it could not be loaded.  This will trigger
     * the error callbacks.
     * @param t 
     */
    @Async.Execute
    public void error(Throwable t) {
        SuccessCallback cb = null;
        synchronized(lock) {
            this.error = t;
            done = true;
            if (errorCallback != null) {
                cb = errorCallback;
            }
        }
        setChanged();
        notifyObservers();
        if (cb != null) {
            cb.onSucess(error);
        }
    }
    
    /**
     * Creates a single AsyncResource that will fire its ready() only when all of the provided resources
     * are ready.  And will fire an exception if any of the provided resources fires an exception.
     * @param resources One ore more resources to wrap.
     * @return A combined AsyncResource.
     * @since 7.0
     */
    public static AsyncResource<Boolean> all(AsyncResource<?>... resources) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        final Set<AsyncResource> pending = new HashSet<AsyncResource>(Arrays.asList(resources));
        final boolean[] complete = new boolean[1];
        for (final AsyncResource<?> res : resources) {
            res.ready(new SuccessCallback() {
                public void onSucess(Object arg) {
                    synchronized (complete) {
                        if (complete[0]) {
                            return;
                        }
                        pending.remove(res);
                        if (pending.isEmpty()) {
                            complete[0] = true;
                            //out.complete(true);
                        } else {
                            return;
                        }
                    }
                    out.complete(true);
                }
            });
            res.except(new SuccessCallback<Throwable>() {
                public void onSucess(Throwable ex) {
                    synchronized (complete) {
                        if (complete[0]) {
                            return;
                        }
                        pending.remove(res);
                        complete[0] = true;
                    }

                    out.error(ex);

                }
            });
        }
        return out;
    }

    /**
     * Creates a single AsyncResource that will fire its ready() only when all of the provided resources
     * are ready.  And will fire an exception if any of the provided resources fires an exception.
     * @param resources One ore more resources to wrap.
     * @return A combined AsyncResource.
     * @since 7.0
     */
    public static AsyncResource<Boolean> all(java.util.Collection<AsyncResource<?>> resources) {
        return all(resources.toArray(new AsyncResource[resources.size()]));
    }

    /**
     * Waits for a set of AsyncResources to be complete.  If any of them fires an exception,
     * then this method will throw a RuntimeException with that exception as the cause.
     * @param resources The resources to wait for.
     * @since 7.0
     */
    public static void await(java.util.Collection<AsyncResource<?>> resources) throws AsyncExecutionException {
         await(resources.toArray(new AsyncResource[resources.size()]));
    }
    
    /**
     * Waits and blocks until this AsyncResource is done.
     * @throws com.codename1.util.AsyncResource.AsyncExecutionException 
     */
    public void await() throws AsyncExecutionException {
        await(this);
    }

    /**
     * Waits for a set of AsyncResources to be complete.  If any of them fires an exception,
     * then this method will throw a RuntimeException with that exception as the cause.
     * @param resources The resources to wait for.
     * @since 7.0
     */
    public static void await(AsyncResource<?>... resources) throws AsyncExecutionException {
        final boolean[] complete = new boolean[1];
        final Throwable[] t = new Throwable[1];
        all(resources)
                .ready(new SuccessCallback() {
                    @Override
                    public void onSucess(Object arg) {
                        synchronized (complete) {
                            complete[0] = true;
                            complete.notify();
                        }
                    }
                }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable ex) {
                synchronized (complete) {
                    t[0] = ex;
                    complete[0] = true;
                    complete.notify();
                }
            }
        });
        while (!complete[0]) {
            if (isEdt()) {
                invokeAndBlock(new Runnable() {
                    public void run() {
                        synchronized (complete) {
                            if (!complete[0]) {
                                Util.wait(complete);
                            }
                        }
                    }
                });
            } else {
                synchronized (complete) {
                    if (!complete[0]) {
                        Util.wait(complete);
                    }
                }
            }
        }

        if (t[0] != null) {
            throw new AsyncExecutionException(t[0]);
        }
    }
    
    /**
     * Adds another AsyncResource as a listener to this async resource.
     * @param resource 
     * @since 7.0
     */
    public void addListener(final AsyncResource<V> resource) {
        ready(new SuccessCallback<V>() {
            @Override
            public void onSucess(V value) {
                if (!resource.isDone()) {
                    resource.complete(value);
                }
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable value) {
                if (!resource.isDone()) {
                    resource.error(value);
                }
            }
        });
    }
    
    /**
     * Combines ready() and except() into a single callback with 2 parameters.  
     * @param onResult A callback that handles both the ready() case and the except() case.  Use {@link #isCancelled(java.lang.Throwable) }
     * to test the error parameter of {@link AsyncResult#onReady(java.lang.Object, java.lang.Throwable) } to see if 
     * if was caused by a cancellation.
     * @since 7.0
     */
    public void onResult(final AsyncResult<V> onResult) {
        ready(new SuccessCallback<V>() {
            @Override
            public void onSucess(V value) {
                onResult.onReady(value, null);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable value) {
                onResult.onReady(null, value);
            }
        });
    }

    /**
     * Wraps this AsyncResource object as a {@link Promise}
     * @return A Promise wrapping this AsyncResource.
     * @since 8.0
     */
    public Promise<V> asPromise() {
        return Promise.promisify(this);
    }
    
    
}