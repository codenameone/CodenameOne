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

package com.codename1.io;

import com.codename1.annotations.Async;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/// Main entry point for managing the connection requests, this is essentially a
/// threaded queue that makes sure to route all connections via the network thread
/// while sending the callbacks through the Codename One EDT.
///
/// The sample
/// code below fetches a page of data from the nestoria housing listing API.
///
/// You can see instructions on how to display the data in the `com.codename1.components.InfiniteScrollAdapter`
/// class. You can read more about networking in Codename One `here`
///
/// ```java
/// int pageNumber = 1;
/// java.util.List<Map<String, Object>> fetchPropertyData(String text) {
///     try {
///         ConnectionRequest r = new ConnectionRequest();
///         r.setPost(false);
///         r.setUrl("http://api.nestoria.co.uk/api");
///         r.addArgument("pretty", "0");
///         r.addArgument("action", "search_listings");
///         r.addArgument("encoding", "json");
///         r.addArgument("listing_type", "buy");
///         r.addArgument("page", "" + pageNumber);
///         pageNumber++;
///         r.addArgument("country", "uk");
///         r.addArgument("place_name", text);
///         NetworkManager.getInstance().addToQueueAndWait(r);
///         Map result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
///         Map response = (Map)result.get("response");
///         return (java.util.List<Map<String, Object>>)response.get("listings");
///     } catch(Exception err) {
///         Log.e(err);
///         return null;
///     }
/// }
/// ```
///
/// @author Shai Almog
public final class NetworkManager {
    /// Indicates an unknown access point type
    public static final int ACCESS_POINT_TYPE_UNKNOWN = 1;

    /// Indicates a wlan (802.11b/c/g/n) access point type
    public static final int ACCESS_POINT_TYPE_WLAN = 2;

    /// Indicates an access point based on a cable
    public static final int ACCESS_POINT_TYPE_CABLE = 3;

    /// Indicates a 3g network access point type
    public static final int ACCESS_POINT_TYPE_NETWORK3G = 4;

    /// Indicates a 2g network access point type
    public static final int ACCESS_POINT_TYPE_NETWORK2G = 5;


    /// Indicates a corporate routing server access point type (e.g. BIS etc.)
    public static final int ACCESS_POINT_TYPE_CORPORATE = 6;

    /// Active network type reported by `getCurrentNetworkType()` /
    /// `NetworkTypeListener`: no usable connectivity.
    public static final int NETWORK_TYPE_NONE = 0;
    /// Active network type: WiFi / 802.11 / WLAN.
    public static final int NETWORK_TYPE_WIFI = 1;
    /// Active network type: cellular data (2G/3G/4G/5G).
    public static final int NETWORK_TYPE_CELLULAR = 2;
    /// Active network type: wired Ethernet.
    public static final int NETWORK_TYPE_ETHERNET = 3;
    /// Active network type: short-range Bluetooth PAN.
    public static final int NETWORK_TYPE_BLUETOOTH = 4;
    /// Active network type reported by the platform but not classified into
    /// one of the named buckets above.
    public static final int NETWORK_TYPE_OTHER = 5;

    private static final Object LOCK = new Object();
    private static final NetworkManager INSTANCE = new NetworkManager();
    private EventDispatcher networkTypeListeners;
    private int lastNetworkType = -1;
    private boolean lastVpnActive;
    private static String autoDetectURL = "https://www.google.com/";
    private final Vector pending = new Vector();
    private final Hashtable threadAssignements = new Hashtable();
    private boolean running;
    private int threadCount = 1;
    private NetworkThread[] networkThreads;
    private EventDispatcher errorListeners;
    private EventDispatcher progressListeners;
    private int timeout = 300000;
    private Hashtable userHeaders;
    private boolean autoDetected;
    private int nextConnectionId = 1;

    private NetworkManager() {
    }

    /// This URL is used to check whether an Internet connection is available
    ///
    /// #### Returns
    ///
    /// the autoDetectURL
    public static String getAutoDetectURL() {
        return autoDetectURL;
    }

    /// This URL is used to check whether an Internet connection is available
    ///
    /// #### Parameters
    ///
    /// - `aAutoDetectURL`: the autoDetectURL to set
    public static void setAutoDetectURL(String aAutoDetectURL) {
        autoDetectURL = aAutoDetectURL;
    }

    /// Callback for native layer to check the certificates of a connection request.
    ///
    /// #### Parameters
    ///
    /// - `connectionId`: THe connection ID of the connection request to check.
    ///
    /// #### Returns
    ///
    /// @return True if the certificates check out, or if the ConnectionRequest is not set
    /// to check certificates.
    ///
    /// Currently this is only used by iOS.
    /// To use this method in other ports, you need to implement the `CodenameOneImplementation#checkSSLCertificatesRequiresCallbackFromNative()` to return true.
    ///
    /// #### Deprecated
    ///
    /// For internal use only
    ///
    /// #### See also
    ///
    /// - CodenameOneImplementation#checkSSLCertificatesRequiresCallbackFromNative()
    static boolean checkCertificatesNativeCallback(int connectionId) {
        ArrayList<NetworkThread> threads = new ArrayList<NetworkThread>();
        synchronized (LOCK) {
            if (INSTANCE == null || INSTANCE.networkThreads == null) {
                return true;
            }

            for (NetworkThread nt : INSTANCE.networkThreads) {
                if (nt != null) {
                    threads.add(nt);
                }
            }
        }
        for (NetworkThread nt : threads) {
            if (nt.currentRequest == null) {
                continue;
            }
            if (nt.currentRequest.getId() == connectionId) {

                return nt.currentRequest.checkCertificatesNativeCallback();
            }
        }
        return true;
    }

    /// Returns the singleton instance of this class
    ///
    /// #### Returns
    ///
    /// instance of this class
    public static NetworkManager getInstance() {
        return INSTANCE;
    }

    /// Read through [#getNetworkGuard()], which synchronizes on the same monitor the two
    /// writers below take. Not `volatile`: the core is built for Java 5 semantics and the
    /// repo's PMD gate forbids the modifier outright, so the lock is what publishes the
    /// write to the network thread.
    private static NetworkGuard networkGuard;
    private static boolean networkGuardSealed;

    /// Installs the app-wide [NetworkGuard].
    ///
    /// The first call wins and the slot then seals. A guard can veto requests and enforce
    /// certificate pins, so allowing it to be replaced at runtime would let any code that runs
    /// later -- including code an attacker injected -- swap in a permissive one.
    ///
    /// @throws IllegalStateException if a guard is already installed
    public static void setNetworkGuard(NetworkGuard guard) {
        if (guard == null) {
            throw new IllegalArgumentException("guard is null");
        }
        synchronized (NetworkManager.class) {
            if (networkGuardSealed) {
                throw new IllegalStateException("A network guard is already installed");
            }
            networkGuard = guard;
            networkGuardSealed = true;
        }
    }

    /// The installed guard, or null when none was installed.
    public static synchronized NetworkGuard getNetworkGuard() {
        return networkGuard;
    }

    /// Read through [#getNetworkTracer()], for the same publication reason as the guard.
    private static NetworkTracer networkTracer;

    /// Installs the app-wide [NetworkTracer], replacing any earlier one; null removes it.
    ///
    /// Unlike the guard this slot does not seal: a tracer only observes, so replacing
    /// one cannot weaken anything, and telemetry that is switched off at run time has
    /// to be able to take itself out.
    public static synchronized void setNetworkTracer(NetworkTracer tracer) {
        networkTracer = tracer;
    }

    /// The installed tracer, or null.
    public static synchronized NetworkTracer getNetworkTracer() {
        return networkTracer;
    }

    /// Test hook: drops the installed guard and unseals the slot.
    static void resetNetworkGuardForTesting() {
        synchronized (NetworkManager.class) {
            networkGuard = null;
            networkGuardSealed = false;
        }
    }

    void resetAPN() {
        autoDetected = false;
    }

    boolean handleErrorCode(ConnectionRequest r, int code, String message) {
        if (errorListeners != null) {
            ActionEvent ev = new NetworkEvent(r, code, message);
            errorListeners.fireActionEvent(ev);
            return ev.isConsumed();
        }
        return false;
    }

    private boolean handleException(ConnectionRequest r, Exception o) {
        if (errorListeners != null) {
            ActionEvent ev = new NetworkEvent(r, o);
            errorListeners.fireActionEvent(ev);
            return ev.isConsumed();
        }
        return false;
    }

    /// The number of threads
    ///
    /// #### Returns
    ///
    /// the threadCount
    public int getThreadCount() {
        return threadCount;
    }

    /// Thread count should never be changed when the network is running since it will have no effect.
    /// Increasing the thread count can bring many race conditions and problems to the surface,
    /// furthermore some platforms don't support more than one network thread hence increasing
    /// the thread count might fail.
    ///
    /// #### Parameters
    ///
    /// - `threadCount`: the threadCount to set
    ///
    /// #### Deprecated
    ///
    /// @deprecated since the network is always running in Codename One this method is quite confusing
    /// unfortunately fixing it will probably break working code. You should migrate the code to use
    /// `#updateThreadCount(int)`
    public void setThreadCount(int threadCount) {
        // in auto detect mode multiple threads can break the detections
        if (!Util.getImplementation().shouldAutoDetectAccessPoint()) {
            this.threadCount = threadCount;
        }
    }

    /// Sets the number of network threads and restarts the network threads
    ///
    /// #### Parameters
    ///
    /// - `threadCount`: the new number of threads
    public void updateThreadCount(int threadCount) {
        this.threadCount = threadCount;
        shutdown();
        start();
    }

    boolean hasProgressListeners() {
        return progressListeners != null;
    }

    void fireProgressEvent(ConnectionRequest c, int type, int length, int sentReceived) {
        // progressListeners might be made null by a separate thread
        EventDispatcher d = progressListeners;
        if (d != null) {
            NetworkEvent n = new NetworkEvent(c, type);
            n.setLength(length);
            n.setSentReceived(sentReceived);
            d.fireActionEvent(n);
        }
    }

    private NetworkThread createNetworkThread() {
        return new NetworkThread();
    }

    /// There is no need to invoke this method since the network manager is started
    /// implicitly. It is useful only if you explicitly stop the network manager.
    /// Invoking this method otherwise will just do nothing.
    public void start() {
        if (networkThreads != null) {
            //throw new IllegalStateException("Network manager already initialized");
            return;
        }
        running = true;
        networkThreads = new NetworkThread[getThreadCount()];
        for (int iter = 0; iter < getThreadCount(); iter++) {
            networkThreads[iter] = createNetworkThread();
            networkThreads[iter].start();
        }
        // we need to implement a timeout thread of our own for this case...
        if (!Util.getImplementation().isTimeoutSupported()) {
            Util.getImplementation().startThread("Timeout Thread", new Runnable() {
                @Override
                public void run() {
                    // detect timeout violations by polling
                    while (running) {
                        try {
                            Thread.sleep(timeout / 10);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        NetworkThread[] networkThreads = NetworkManager.this.networkThreads;
                        if (networkThreads == null) {
                            return;
                        }
                        // check for timeout violations on the currently executing threads
                        int ntlen = networkThreads.length;
                        for (int iter = 0; iter < ntlen; iter++) {
                            ConnectionRequest c = networkThreads[iter].getCurrentRequest();
                            if (c != null) {
                                int cTimeout = Math.min(timeout, c.getTimeout());
                                if (c.getTimeout() < 0) {
                                    cTimeout = timeout;
                                }
                                if (c.getTimeSinceLastActivity() > cTimeout) {
                                    // we have a timeout problem on our hands! We need to try and kill!
                                    c.kill();
                                    networkThreads[iter].interrupt();
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException ex) {
                                        ex.printStackTrace();
                                    }

                                    // did the attempt work?
                                    if (networkThreads[iter].getCurrentRequest() == c) {
                                        if (c.getTimeSinceLastActivity() > cTimeout) {
                                            // we need to create a whole new network thread and abandon this one!
                                            if (running) {
                                                networkThreads[iter] = createNetworkThread();
                                                networkThreads[iter].start();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    /// Shuts down the network thread, this will trigger failures if you have network requests
    ///
    /// #### Deprecated
    ///
    /// This method is for internal use only
    public void shutdown() {
        running = false;
        if (networkThreads != null) {
            for (NetworkThread n : networkThreads) {
                if (n != null) {
                    n.stopped = true;
                }
            }
        }
        synchronized (LOCK) {
            networkThreads = null;
            LOCK.notifyAll();
        }

    }

    /// Shuts down the network thread and waits for shutdown to complete
    public void shutdownSync() {
        NetworkThread[] n = this.networkThreads;
        if (n != null) {
            NetworkThread t = n[0];
            if (t != null) {
                shutdown();
                t.join();
            }
        }
    }

    private void addSortedToQueue(ConnectionRequest request, int priority) {
        for (int iter = 0; iter < pending.size(); iter++) {
            ConnectionRequest r = (ConnectionRequest) pending.elementAt(iter);
            if (r.getPriority() < priority) {
                pending.insertElementAt(request, iter);
                return;
            }
        }
        pending.addElement(request);
    }

    /// Adds a header to the global default headers, this header will be implicitly added
    /// to all requests going out from this point onwards. The main use case for this is
    /// for authentication information communication via the header.
    ///
    /// #### Parameters
    ///
    /// - `key`: the key of the header
    ///
    /// - `value`: the value of the header
    public void addDefaultHeader(String key, String value) {
        if (userHeaders == null) {
            userHeaders = new Hashtable();
        }
        userHeaders.put(key, value);
    }

    /// Identical to add to queue but returns an AsyncResource object that will resolve to
    /// the ConnectionRequest.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request object to add.
    ///
    /// #### Returns
    ///
    /// AsyncResource resolving to the connection request on complete.
    ///
    public AsyncResource<ConnectionRequest> addToQueueAsync(final ConnectionRequest request) {
        final AsyncResource<ConnectionRequest> out = new AsyncResource<ConnectionRequest>();
        class WaitingClass implements ActionListener<NetworkEvent> {


            @Override
            public void actionPerformed(NetworkEvent e) {
                if (e.getError() != null) {

                    removeProgressListener(this);
                    removeErrorListener(this);
                    if (!out.isDone()) {
                        out.error(e.getError());
                    }
                    return;
                }
                if (e.getConnectionRequest() == request) { //NOPMD CompareObjectsWithEquals
                    if (e.getProgressType() == NetworkEvent.PROGRESS_TYPE_COMPLETED) {
                        if (request.retrying) {
                            request.retrying = false;
                            return;
                        }

                        removeProgressListener(this);
                        removeErrorListener(this);
                        if (!out.isDone()) {
                            out.complete(request);
                        }
                    }
                }
            }
        }
        WaitingClass w = new WaitingClass();
        addProgressListener(w);
        addErrorListener(w);
        addToQueue(request);
        return out;
    }

    /// Identical to add to queue but waits until the request is processed in the queue,
    /// this is useful for completely synchronous operations.
    ///
    /// #### Parameters
    ///
    /// - `request`: the request object to add
    public void addToQueueAndWait(final ConnectionRequest request) {
        class WaitingClass implements Runnable, ActionListener<NetworkEvent> {
            private final boolean edt = CN.isEdt();
            private boolean finishedWaiting;

            @Override
            public void run() {
                if (edt) {
                    while (!finishedWaiting) {
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    while (!request.complete) {
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void actionPerformed(NetworkEvent e) {
                if (e.getError() != null) {
                    finishedWaiting = true;
                    removeProgressListener(this);
                    removeErrorListener(this);
                    return;
                }
                if (e.getConnectionRequest() == request) { //NOPMD CompareObjectsWithEquals
                    if (e.getProgressType() == NetworkEvent.PROGRESS_TYPE_COMPLETED) {
                        if (request.retrying) {
                            request.retrying = false;
                            return;
                        }
                        finishedWaiting = true;
                        removeProgressListener(this);
                        removeErrorListener(this);
                    }
                }
            }
        }
        WaitingClass w = new WaitingClass();
        if (Display.getInstance().isEdt()) {
            addProgressListener(w);
            addErrorListener(w);
            addToQueue(request);
            Display.getInstance().invokeAndBlock(w);
        } else {
            addToQueue(request);
            w.run();
        }
    }

    /// Adds the given network connection to the queue of execution
    ///
    /// #### Parameters
    ///
    /// - `request`: network request for execution
    public void addToQueue(ConnectionRequest request) {
        addToQueue(request, false);
    }

    /// Kills the given request and waits until the request is killed if it is
    /// being processed by one of the threads. This method must not be invoked from
    /// a network thread!
    ///
    /// #### Parameters
    ///
    /// - `request`
    public void killAndWait(final ConnectionRequest request) {
        request.kill();
        class KillWaitingClass implements Runnable {
            @Override
            public void run() {
                for (int iter = 0; iter < threadCount; iter++) {
                    if (networkThreads[iter].currentRequest == request) {
                        synchronized (LOCK) {
                            while (networkThreads[iter].currentRequest == request) {
                                try {
                                    LOCK.wait(20);
                                } catch (InterruptedException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }
        Display.getInstance().invokeAndBlock(new KillWaitingClass());
    }

    void kill9(final ConnectionRequest request) {
        if (request.isKilled()) {
            for (int iter = 0; iter < threadCount; iter++) {
                if (networkThreads[iter].currentRequest == request) {
                    synchronized (LOCK) {
                        if (networkThreads[iter].currentRequest == request) {
                            networkThreads[iter].interrupt();
                            networkThreads[iter].stopped = true;
                            networkThreads[iter] = createNetworkThread();
                            networkThreads[iter].start();
                        }
                    }
                }
            }
        }
    }

    /// Ends the tracer attempt in flight on `req`, if any, with the tracer that
    /// started it. Cleared first, so an attempt is ended exactly once however the
    /// tracer behaves.
    ///
    /// Only the thread that started the attempt ends it. Once a retry has
    /// re-queued the request, another worker may already have begun the NEXT
    /// attempt by the time this one reaches its finally, and that attempt is
    /// not this thread's to end.
    static void endTracerAttempt(ConnectionRequest req, Throwable failure) {
        Object attempt = req.tracerAttempt;
        NetworkTracer owner = req.tracerOwner;
        if (attempt == null || owner == null
                || req.tracerThread != Thread.currentThread()) { //NOPMD CompareObjectsWithEquals
            return;
        }
        req.tracerAttempt = null;
        req.tracerOwner = null;
        req.tracerThread = null;
        // Kept for a retry of a request queued with no parent: see addToQueue.
        req.tracerLastAttempt = attempt;
        req.tracerLastOwner = owner;
        try {
            // Only a status THIS attempt received: a reused request still holds
            // the last one's.
            owner.afterRequest(req, attempt,
                    req.tracerResponded ? req.getResponseCode() : -1, failure);
        } catch (Throwable t) {
            // Observation must never change a request's outcome.
            Log.e(t);
        }
    }

    /// Clears `req`'s tracer state on the EDT, after whatever this attempt already
    /// queued there, unless the request was queued again in the meantime.
    private static void scheduleTracerClear(final ConnectionRequest req, final int requeues) {
        if (!Display.isInitialized()) {
            clearTracerState(req);
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (req.tracerRequeues == requeues) {
                    clearTracerState(req);
                }
            }
        });
    }

    /// Forgets every tracer object a finished request holds.
    static void clearTracerState(ConnectionRequest req) {
        req.tracerParent = null;
        req.tracerParentOwner = null;
        req.tracerParentChained = false;
        req.tracerLastAttempt = null;
        req.tracerLastOwner = null;
    }

    /// Adds the given network connection to the queue of execution
    ///
    /// #### Parameters
    ///
    /// - `request`: network request for execution
    void addToQueue(@Async.Schedule ConnectionRequest request, boolean retry) {
        if (retry) {
            // A redirect or retry re-queues THIS request object while its current
            // attempt is still open on the worker that ran it. With more than one
            // network thread another worker can pick it up before that worker
            // reaches its finally and overwrite the attempt's state -- losing the
            // span and leaving its traceparent on the request. So the attempt ends
            // here, on the thread that ran it, before the request is visible to
            // anyone else.
            endTracerAttempt(request, null);
            request.tracerRequeues++;
            // A request queued with no context -- the usual case for a generated
            // client used outside Telemetry.run -- would start a NEW trace on every
            // attempt: a 302 and the 200 it led to, or a failure and the retry that
            // succeeded, came out as unrelated traces with separate sampling
            // decisions, and the logical request could not be followed. So the
            // attempt that just ended becomes the next one's parent: one trace,
            // one decision, each attempt still its own span. A request that WAS
            // queued inside an action keeps that action as every attempt's parent.
            // A parent some OTHER tracer captured is as good as none: the tracer
            // that ran this attempt will not use it (it only takes its own), and
            // keeping it blocked the chain, so every retry started a new root.
            // The attempt to continue from is the last one that ENDED -- or, when a
            // listener retries from the EDT before the network thread has finished
            // the attempt it is reacting to, that attempt, still in flight. Waiting
            // for "ended" alone lost the race on a fast EDT, and the retry started
            // an unrelated trace.
            Object previous = request.tracerAttempt != null
                    ? request.tracerAttempt : request.tracerLastAttempt;
            NetworkTracer previousOwner = request.tracerAttempt != null
                    ? request.tracerOwner : request.tracerLastOwner;
            if ((request.tracerParent == null || request.tracerParentChained
                    || request.tracerParentOwner != previousOwner) //NOPMD CompareObjectsWithEquals
                    && previous != null) {
                request.tracerParent = previous;
                request.tracerParentOwner = previousOwner;
                request.tracerParentChained = true;
            }
        }
        // Captured HERE, on the thread that asked for the request, so the span it
        // becomes is a child of what the app was doing at the time. A retry keeps
        // the context of the request it retries. Held in locals and stored only once
        // the enqueue is accepted below: re-adding a request that is already pending
        // is rejected as a duplicate, and storing first would re-parent the queued
        // one under whatever the rejected call was doing.
        NetworkTracer queuedBy = null;
        Object queuedParent = null;
        if (!retry) {
            NetworkTracer tracer = getNetworkTracer();
            if (tracer != null) {
                try {
                    queuedParent = tracer.requestQueued(request);
                    queuedBy = tracer;
                } catch (Throwable t) {
                    Log.e(t);
                }
            }
        }
        Util.getImplementation().addConnectionToQueue(request);
        if (!running) {
            start();
        }
        if (!autoDetected) {
            autoDetected = true;
            if (Util.getImplementation().shouldAutoDetectAccessPoint()) {
                AutoDetectAPN r = new AutoDetectAPN();
                r.setPost(false);
                r.setUrl(autoDetectURL);
                r.setPriority(ConnectionRequest.PRIORITY_CRITICAL);
                addToQueue(r, false);
            }
        }
        request.validateImpl();
        synchronized (LOCK) {
            int i = request.getPriority();
            if (!retry) {
                if (!request.isDuplicateSupported()) {
                    if (pending.contains(request)) {
                        Log.p("Duplicate entry in the queue: " + request.getClass().getName() + ": " + request);
                        return;
                    }
                    ConnectionRequest currentRequest = networkThreads[0].getCurrentRequest();
                    if (currentRequest != null && !currentRequest.retrying && currentRequest.equals(request)) {
                        System.out.println("Duplicate entry detected");
                        return;
                    }
                }
                request.tracerParent = queuedParent;
                request.tracerParentOwner = queuedBy;
                // A fresh enqueue is a new logical request, not a retry of the last.
                // It advances the generation too: a cleanup the previous run queued
                // on the EDT would otherwise still match, and clear the parent this
                // enqueue just captured -- a listener can reuse a finished request
                // with addToQueue before that cleanup runs.
                request.tracerRequeues++;
                request.tracerParentChained = false;
                request.tracerLastAttempt = null;
                request.tracerLastOwner = null;
            } else {
                i = ConnectionRequest.PRIORITY_HIGH;
            }
            switch (i) {
                case ConnectionRequest.PRIORITY_CRITICAL:
                    pending.insertElementAt(request, 0);
                    ConnectionRequest currentRequest = networkThreads[0].getCurrentRequest();
                    if (currentRequest != null && currentRequest.getPriority() < ConnectionRequest.PRIORITY_CRITICAL) {
                        if (currentRequest.isPausable()) {
                            currentRequest.pause();
                            pending.insertElementAt(currentRequest, 1);
                        } else {
                            currentRequest.kill();
                        }
                    }
                    break;
                case ConnectionRequest.PRIORITY_HIGH:
                case ConnectionRequest.PRIORITY_NORMAL:
                case ConnectionRequest.PRIORITY_LOW:
                case ConnectionRequest.PRIORITY_REDUNDANT:
                    addSortedToQueue(request, i);
                    break;
                default:
                    addSortedToQueue(request, i);
                    break;
            }
            LOCK.notifyAll();
        }
    }

    /// Returns the timeout duration
    ///
    /// #### Returns
    ///
    /// timeout in milliseconds
    public int getTimeout() {
        return timeout;
    }

    /// Sets the timeout in milliseconds for network connections, a timeout may be "faked"
    /// for platforms that don't support the notion of a timeout
    ///
    /// #### Parameters
    ///
    /// - `t`: the timeout duration
    public void setTimeout(int t) {
        if (Util.getImplementation().isTimeoutSupported()) {
            Util.getImplementation().setTimeout(t);
        } else {
            timeout = t;
        }
    }

    /// Adds a generic listener to a network error that is invoked before the exception is propagated.
    /// Note that this handles also server error codes by default! You can change this default behavior setting to false
    /// ConnectionRequest.setHandleErrorCodesInGlobalErrorHandler(boolean).
    /// Consume the event in order to prevent it from propagating further.
    ///
    /// #### Parameters
    ///
    /// - `e`: callback will be invoked with the Exception as the source object
    public void addErrorListener(ActionListener<NetworkEvent> e) {
        if (errorListeners == null) {
            errorListeners = new EventDispatcher();
            errorListeners.setBlocking(true);
        }
        errorListeners.addListener(e);
    }

    /// Removes the given error listener
    ///
    /// #### Parameters
    ///
    /// - `e`: callback to remove
    public void removeErrorListener(ActionListener<NetworkEvent> e) {
        if (errorListeners == null) {
            return;
        }

        errorListeners.removeListener(e);
    }

    /// Adds a listener to be notified when progress updates
    ///
    /// #### Parameters
    ///
    /// - `al`: action listener
    public void addProgressListener(ActionListener<NetworkEvent> al) {
        if (progressListeners == null) {
            progressListeners = new EventDispatcher();
            progressListeners.setBlocking(false);
        }
        progressListeners.addListener(al);
    }

    /// Adds a listener to be notified when progress updates
    ///
    /// #### Parameters
    ///
    /// - `al`: action listener
    public void removeProgressListener(ActionListener<NetworkEvent> al) {
        if (progressListeners == null) {
            return;
        }
        progressListeners.removeListener(al);
        Collection v = progressListeners.getListenerCollection();
        if (v == null || v.isEmpty()) {
            progressListeners = null;
        }
    }

    /// Makes sure the given class (subclass of ConnectionRequest) is always assigned
    /// to the given thread number. This is useful for a case of an application that wants
    /// all background downloads to occur on one thread so it doesn't tie up the main
    /// network thread (but doesn't stop like a low priority request would).
    ///
    /// #### Parameters
    ///
    /// - `requestType`: the class of the specific connection request
    ///
    /// - `offset`: the offset of the thread starting from 0 and smaller than thread count
    public void assignToThread(Class requestType, int offset) {
        threadAssignements.put(requestType.getName(), Integer.valueOf(offset));
    }

    /// This method returns all pending ConnectioRequest connections.
    ///
    /// #### Returns
    ///
    /// the queue elements
    public Enumeration enumurateQueue() {
        Vector elements = new Vector();
        synchronized (LOCK) {
            Enumeration e = pending.elements();
            while (e.hasMoreElements()) {
                elements.addElement(e.nextElement());
            }
        }
        return elements.elements();
    }

    /// Indicates that the network queue is idle
    ///
    /// #### Returns
    ///
    /// true if no network activity is in progress or pending
    public boolean isQueueIdle() {
        return pending == null ||
                networkThreads == null ||
                networkThreads[0] == null ||
                (pending.isEmpty() && networkThreads[0].getCurrentRequest() == null);
    }

    /// Indicates whether looking up an access point is supported by this device
    ///
    /// #### Returns
    ///
    /// true if access point lookup is supported
    public boolean isAPSupported() {
        return Util.getImplementation().isAPSupported();
    }

    /// Returns the ids of the access points available if supported
    ///
    /// #### Returns
    ///
    /// ids of access points
    public String[] getAPIds() {
        return Util.getImplementation().getAPIds();
    }

    /// Returns the type of the access point
    ///
    /// #### Parameters
    ///
    /// - `id`: access point id
    ///
    /// #### Returns
    ///
    /// one of the supported access point types from network manager
    public int getAPType(String id) {
        return Util.getImplementation().getAPType(id);
    }

    /// Returns the user displayable name for the given access point
    ///
    /// #### Parameters
    ///
    /// - `id`: the id of the access point
    ///
    /// #### Returns
    ///
    /// the name of the access point
    public String getAPName(String id) {
        return Util.getImplementation().getAPName(id);
    }

    /// Returns the id of the current access point
    ///
    /// #### Returns
    ///
    /// id of the current access point
    public String getCurrentAccessPoint() {
        return Util.getImplementation().getCurrentAccessPoint();
    }

    /// Returns the id of the current access point
    ///
    /// #### Parameters
    ///
    /// - `id`: id of the current access point
    public void setCurrentAccessPoint(String id) {
        Util.getImplementation().setCurrentAccessPoint(id);
    }

    /// Indicates whether the current platform supports best-effort VPN detection.
    ///
    /// #### Returns
    ///
    /// `true` if `#isVPNActive()` is implemented on this platform.
    public boolean isVPNDetectionSupported() {
        return Util.getImplementation().isVPNDetectionSupported();
    }

    /// Best-effort check for whether a VPN appears to be active.
    ///
    /// This value should be treated as advisory only. Platform APIs and
    /// interface-name heuristics can miss some VPN configurations and may also
    /// report non-VPN tunnels as VPNs.
    ///
    /// #### Returns
    ///
    /// `true` if a VPN appears to be active on the current connection.
    public boolean isVPNActive() {
        return Util.getImplementation().isVPNActive();
    }

    /// Returns the device's currently active network type, one of the
    /// `NETWORK_TYPE_*` constants. Returns `NETWORK_TYPE_NONE` when there is
    /// no connectivity. Distinct from `getAPType` which describes a configured
    /// access point rather than the active data path.
    public int getCurrentNetworkType() {
        return Display.getInstance()
                .getNetworkTypePlatform().getCurrentNetworkType();
    }

    /// Fast best-effort connectivity check. Returns `false` only when the
    /// platform reports `NETWORK_TYPE_NONE`; all other states (WiFi,
    /// cellular, ethernet, VPN-only, or "other") return `true`. Avoids the
    /// HTTP probe that `getInstance().assignToThread(...)` performs against
    /// `autoDetectURL` so the check is suitable to call from the EDT.
    ///
    /// Apps that need a stronger "can I reach my server" guarantee should
    /// still fire a real `ConnectionRequest`; this method only reports
    /// whether the device has *any* usable network interface up.
    public boolean isConnected() {
        return getCurrentNetworkType() != NETWORK_TYPE_NONE;
    }

    /// Issues a blocking HTTP HEAD request to `url` and returns whether the
    /// server responded within `timeoutMillis`. Any response - including 4xx
    /// or 5xx - is treated as a successful ping (the network round-trip
    /// completed). Only socket errors, DNS failures, and timeouts return
    /// false.
    ///
    /// Use this for reachability probes against a known endpoint when
    /// [#isConnected] (a fast device-side network-type check) isn't strong
    /// enough. Must not be invoked from the EDT.
    ///
    /// #### Parameters
    ///
    /// - `url`: the URL to probe; typically a small static endpoint
    ///
    /// - `timeoutMillis`: maximum time to wait for the request to complete; pass
    /// 0 to use the connection's default timeout
    ///
    /// #### Returns
    ///
    /// true if the server responded, false on timeout, DNS, or socket error
    public boolean ping(String url, int timeoutMillis) {
        if (url == null) {
            throw new IllegalArgumentException("url is null");
        }
        ConnectionRequest cr = new ConnectionRequest();
        cr.setUrl(url);
        cr.setPost(false);
        cr.setHttpMethod("HEAD");
        cr.setFailSilently(true);
        cr.setReadResponseForErrors(false);
        if (timeoutMillis > 0) {
            cr.setTimeout(timeoutMillis);
            cr.setReadTimeout(timeoutMillis);
        }
        addToQueueAndWait(cr);
        return cr.getResponseCode() > 0;
    }

    /// Registers `l` to be notified when the device's active network type
    /// changes (WiFi <-> Cellular <-> None <-> ...). The listener is invoked
    /// on the EDT. Safe to call multiple times with the same listener; only
    /// the first registration takes effect.
    ///
    /// On platforms where network change events are unavailable the listener
    /// is still installed but never fires; `getCurrentNetworkType()` should
    /// be polled instead.
    public void addNetworkTypeListener(NetworkTypeListener l) {
        synchronized (LOCK) {
            if (networkTypeListeners == null) {
                networkTypeListeners = new EventDispatcher();
                networkTypeListeners.setBlocking(false);
                Display.getInstance()
                        .getNetworkTypePlatform().install(this);
                lastNetworkType = getCurrentNetworkType();
                lastVpnActive = isVPNActive();
            }
            if (!containsListener(networkTypeListeners, l)) {
                networkTypeListeners.addListener(l);
            }
        }
    }

    /// Removes a listener previously registered with
    /// `addNetworkTypeListener(NetworkTypeListener)`. If the last listener is
    /// removed the platform watcher is torn down too.
    public void removeNetworkTypeListener(NetworkTypeListener l) {
        synchronized (LOCK) {
            if (networkTypeListeners == null) {
                return;
            }
            networkTypeListeners.removeListener(l);
            Collection v = networkTypeListeners.getListenerCollection();
            if (v == null || v.isEmpty()) {
                Display.getInstance()
                        .getNetworkTypePlatform().uninstall(this);
                networkTypeListeners = null;
            }
        }
    }

    private boolean containsListener(EventDispatcher d, Object l) {
        Collection v = d.getListenerCollection();
        return v != null && v.contains(l);
    }

    /// Internal: invoked by platform implementations to deliver a network
    /// type change event. Public so platform code in other packages can call
    /// it; not intended for application use.
    public void fireNetworkTypeChange(int newType, boolean vpnActive) {
        EventDispatcher d;
        int oldType;
        boolean fire;
        synchronized (LOCK) {
            d = networkTypeListeners;
            oldType = lastNetworkType;
            fire = d != null && (oldType != newType || lastVpnActive != vpnActive);
            lastNetworkType = newType;
            lastVpnActive = vpnActive;
        }
        if (fire) {
            Collection listeners = d.getListenerCollection();
            if (listeners == null) {
                return;
            }
            Object[] arr = listeners.toArray();
            for (Object o : arr) {
                if (o instanceof NetworkTypeListener) {
                    ((NetworkTypeListener) o)
                            .onNetworkTypeChanged(oldType, newType, vpnActive);
                }
            }
        }
    }

    class NetworkThread implements Runnable {
        boolean stopped = false;
        private ConnectionRequest currentRequest;
        private Thread threadInstance;

        public NetworkThread() {
        }

        public ConnectionRequest getCurrentRequest() {
            return currentRequest;
        }

        public void join() {
            try {
                Thread t = threadInstance;
                if (t != null) {
                    t.join();
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        public void start() {
            Util.getImplementation().startThread("Network Thread", this);
        }

        public void interrupt() {
            if (threadInstance != null) {
                threadInstance.interrupt();
            }
        }

        public Thread getThreadInstance() {
            return threadInstance;
        }

        private boolean runCurrentRequest(@Async.Execute ConnectionRequest req) {
            if (!threadAssignements.isEmpty()) {
                String n = req.getClass().getName();
                Integer threadOffset = (Integer) threadAssignements.get(n);
                NetworkThread[] networkThreads = NetworkManager.this.networkThreads;
                if (networkThreads == null) {
                    return false;
                }
                if (threadOffset != null && networkThreads[threadOffset.intValue()] != this) { //NOPMD CompareObjectsWithEquals
                    synchronized (LOCK) {
                        if (!pending.isEmpty()) {
                            pending.insertElementAt(req, 1);
                            return false;
                        }
                        pending.addElement(req);
                        LOCK.notifyAll();
                        long end = System.currentTimeMillis() + 30;
                        while (true) {
                            long remaining = end - System.currentTimeMillis();
                            if (remaining <= 0) {
                                break;
                            }
                            try {
                                LOCK.wait(remaining);
                                break;
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }

            int frameRate = -1;
            boolean requestWasCompleted = true;
            // What failed the attempt, for the tracer. Both catches below handle the
            // failure and do not rethrow, so the finally is the one place that sees
            // every ending.
            Throwable failure = null;
            // How many times the request had been re-queued when this attempt
            // began; compared in the finally to learn whether it was the last.
            int requeuesBefore = req.tracerRequeues;
            // Default this to true because if, for some reason an exception is thrown
            // before calling performOperationComplete(), then the request
            // won't be retried.
            try {
                // for higher priority tasks increase the thread priority, for lower
                // prioirty tasks decrease it. In critical priority reduce the Codename One
                // rendering thread speed for even faster download
                switch (req.getPriority()) {
                    case ConnectionRequest.PRIORITY_CRITICAL:
                        frameRate = Display.getInstance().getFrameRate();
                        Display.getInstance().setFramerate(4);
                        Thread.currentThread().setPriority(Thread.MAX_PRIORITY - 1);
                        break;
                    case ConnectionRequest.PRIORITY_HIGH:
                        Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
                        break;
                    case ConnectionRequest.PRIORITY_NORMAL:
                        break;
                    case ConnectionRequest.PRIORITY_LOW:
                        Thread.currentThread().setPriority(Thread.MIN_PRIORITY + 2);
                        break;
                    case ConnectionRequest.PRIORITY_REDUNDANT:
                        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                        break;
                    default:
                        break;
                }

                // progressListeners might be made null by a separate thread
                EventDispatcher initListeners = progressListeners;
                if (initListeners != null) {
                    initListeners.fireActionEvent(new NetworkEvent(req, NetworkEvent.PROGRESS_TYPE_INITIALIZING));
                }
                if (req.getShowOnInit() != null) {
                    req.getShowOnInit().showModeless();
                }

                requestWasCompleted = req.performOperationComplete();
            } catch (IOException e) {
                failure = e;
                // Ended HERE, with the failure, before any handler runs: a handler
                // that retries re-queues the request, and ending the attempt at
                // that point has no failure to report, so the span came out with
                // neither a response nor an error.
                endTracerAttempt(req, e);
                if (!req.isFailSilently()) {
                    if (!handleException(req, e)) {
                        req.handleIOException(e);
                    }
                } else {
                    // for the record
                    Log.e(e);
                }
            } catch (RuntimeException er) {
                failure = er;
                endTracerAttempt(req, er);
                if (!req.isFailSilently()) {
                    if (!handleException(req, er)) {
                        req.handleRuntimeException(er);
                    }
                } else {
                    // for the record
                    Log.e(er);
                }
            } finally {
                Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
                if (frameRate > -1) {
                    Display.getInstance().setFramerate(frameRate);
                }
                if (requestWasCompleted) {
                    req.complete = true;
                }
                endTracerAttempt(req, failure);
                // Any tracer field, the owners included: a request queued outside an
                // action holds only tracerParentOwner, and that alone pins the whole
                // telemetry installation.
                if (req.tracerRequeues == requeuesBefore
                        && (req.tracerParent != null || req.tracerLastAttempt != null
                        || req.tracerParentOwner != null || req.tracerLastOwner != null)) {
                    // Nothing queued this request again YET. Its tracer state has
                    // to go once it is done -- the parent and the last attempt are
                    // the tracer's own objects, a span and through it the whole
                    // installation, and a request an app keeps for reuse held them
                    // for as long as it lived. But not from here: an exception or
                    // response-code listener runs LATER, on the EDT, and may still
                    // call retry(), which needs the last attempt to continue its
                    // trace. So the clear is queued on the EDT behind those
                    // listener callbacks, and skipped if one of them retried.
                    scheduleTracerClear(req, req.tracerRequeues);
                }
                NetworkGuard guard = getNetworkGuard();
                if (guard != null && req.hasGuardResponse()) {
                    try {
                        guard.afterResponse(req, req.getResponseCode(), req.getGuardHeaders());
                    } catch (Throwable t) {
                        // A guard's bookkeeping must never turn a completed
                        // request into a failed one.
                        Log.e(t);
                    }
                }
                // Read once into a local. A listener removed from the EDT --
                // which is where postResponse() runs, queued while this thread
                // was still finishing the request -- can null the field between
                // the check and the dispatch, and the NullPointerException that
                // followed would escape NetworkThread.run() and kill the only
                // network worker. Same reasoning as fireProgressEvent.
                EventDispatcher completionListeners = progressListeners;
                if (completionListeners != null) {
                    completionListeners.fireActionEvent(new NetworkEvent(req, NetworkEvent.PROGRESS_TYPE_COMPLETED));
                }
                if (req.getDisposeOnCompletion() != null && !req.isRedirecting()) {
                    // there may be a race condition where the dialog hasn't yet appeared but the
                    // network request completed
                    final ConnectionRequest finalReq = req;
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            Dialog dlg = finalReq.getDisposeOnCompletion();
                            if (dlg != null) {
                                dlg.dispose();
                            }
                        }
                    });
                }
            }
            return true;
        }

        @Override
        public void run() {
            threadInstance = Thread.currentThread();
            while (running && !stopped) {
                if (!pending.isEmpty()) {
                    // the synchronization here isn't essential, only for good measure
                    synchronized (LOCK) {
                        //double lock to prevent a potential exception
                        if (pending.isEmpty()) {
                            continue;
                        }
                        currentRequest = (ConnectionRequest) pending.elementAt(0);
                        pending.removeElementAt(0);
                        currentRequest.prepare();
                        if (currentRequest.isKilled()) {
                            // Killed while it waited: runCurrentRequest, whose
                            // finally forgets the tracer state addToQueue
                            // captured, never runs -- so forget it here, or a
                            // request the app keeps holds the parent span and
                            // through it the whole telemetry installation. And
                            // let go of the request itself: the worker would
                            // otherwise hold it as currentRequest until the
                            // next one arrives.
                            scheduleTracerClear(currentRequest, currentRequest.tracerRequeues);
                            currentRequest = null;
                            LOCK.notifyAll();
                            continue;
                        }
                        currentRequest.setId(nextConnectionId++);
                        if (nextConnectionId > 2000000000) {
                            nextConnectionId = 1;
                        }
                    }
                    if (userHeaders != null && currentRequest.shouldApplyDefaultHeaders()) {
                        Enumeration e = userHeaders.keys();
                        while (e.hasMoreElements()) {
                            String key = (String) e.nextElement();
                            String value = (String) userHeaders.get(key);
                            currentRequest.addRequestHeaderDontRepleace(key, value);
                        }
                    }
                    if (!runCurrentRequest(currentRequest)) {
                        continue;
                    }

                    // wakeup threads waiting for the completion of this network operation
                    synchronized (LOCK) {
                        currentRequest = null;
                        LOCK.notifyAll();
                    }
                } else {
                    synchronized (LOCK) {
                        try {
                            // prevent waiting when there is still a pending request
                            // this can occur with a race condition since the synchronize
                            // scope is limited to prevent blocking on add...
                            while (pending.isEmpty() && running && !stopped) {
                                LOCK.wait();
                            }
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    class AutoDetectAPN extends ConnectionRequest {
        private Vector aps = null;
        private int currentAP;

        @Override
        protected void handleErrorResponseCode(int code, String message) {
            retryWithDifferentAPN();
        }

        @Override
        protected void handleException(Exception err) {
            retryWithDifferentAPN();
        }

        @Override
        protected void readResponse(InputStream input) throws IOException {
            String s = Util.readToString(input);
            if (!"hi".equals(s)) {
                retryWithDifferentAPN();
            }
        }

        private String nextAP() {
            if (aps == null) {
                aps = new Vector();
                String[] ids = getAPIds();
                int idlen = ids.length;
                for (int iter = 0; iter < idlen; iter++) {
                    int t = getAPType(ids[iter]);
                    if (t == ACCESS_POINT_TYPE_WLAN) {
                        aps.insertElementAt(ids[iter], 0);
                    } else {
                        if (t == ACCESS_POINT_TYPE_CORPORATE || t == ACCESS_POINT_TYPE_NETWORK3G) {
                            aps.addElement(ids[iter]);
                        }
                    }
                }

                // add all the 2G networks at the end
                for (int iter = 0; iter < idlen; iter++) {
                    int t = getAPType(ids[iter]);
                    if (t == ACCESS_POINT_TYPE_NETWORK2G) {
                        aps.addElement(ids[iter]);
                    }
                }
            }
            if (currentAP >= aps.size()) {
                return null;
            }
            String s = (String) aps.elementAt(currentAP);
            currentAP++;
            return s;
        }

        private void retryWithDifferentAPN() {
            String n = nextAP();
            if (n == null) {
                return;
            }
            setCurrentAccessPoint(n);
            AutoDetectAPN r = new AutoDetectAPN();
            r.setPost(false);
            r.currentAP = currentAP;
            r.aps = aps;
            r.setUrl(autoDetectURL);
            r.setPriority(ConnectionRequest.PRIORITY_CRITICAL);
            addToQueue(r);
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (aps != null ? aps.hashCode() : 0);
            result = 31 * result + currentAP;
            return result;
        }
    }
}
