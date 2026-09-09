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
package com.codename1.impl.android;

import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.SurfaceView;

import com.codename1.io.Log;
import com.codename1.util.SuccessCallback;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

/// The system's own location button, hosted in a Codename One peer.
///
/// From Android 17 (API level 37) the platform will draw a location button on an
/// application's behalf, in its own process, and hand back a session-scoped
/// precise-location grant when the user taps it. Google Play requires that route
/// for transactional location use from 2027-01-27. The control arrives as a
/// `SurfaceControlViewHost.SurfacePackage` which this view adopts as its child
/// surface, exactly as `androidx.core.locationbutton.LocationButton` does.
///
/// #### Why every platform call here is reflective
///
/// The AndroidX wrapper cannot be used: its aar metadata declares
/// `minCompileSdk=37` and `minAndroidGradlePluginVersion=9.1.0`, and AGP's
/// `checkAarMetadata` enforces both with no opt-out -- so depending on it would
/// force every generated Codename One project onto AGP 9 before this feature
/// could ship at all.
///
/// Calling the platform directly through reflection removes that floor
/// completely. The Android port compiles against the cn1-binaries `android.jar`,
/// which is API 27 and has neither `android.app.permissionui` nor
/// `SurfaceControlViewHost`; generated applications compile against whatever
/// compile SDK they already use. Nothing here needs either to be raised, and
/// nothing in a build has to be deleted to keep an older platform compiling.
///
/// `LocationButtonClient` is an *interface*, which is what makes this practical:
/// [java.lang.reflect.Proxy] can implement it. (Contrast `BiometricsApi29`,
/// whose callback was an abstract class, where the same trick silently produced
/// a callback that never fired.)
///
/// #### The shape of a session
///
/// - `LocationButtonProviderFactory.create(Context)` yields a provider.
/// - `provider.openSession(activity, hostToken, displayId, request, executor, client)`
///   asks for one. The host token comes from this view and is null until it is
///   attached to a window, so the session is opened from `onSizeChanged` --
///   which also supplies the size the request needs.
/// - `client.onSessionOpened(session)` arrives, and `session.getSurfacePackage()`
///   is set as this view's child surface.
/// - `client.onPermissionResult(boolean)` arrives on every tap.
class AndroidLocationButton extends SurfaceView {

    /// The level at which the platform started drawing this control itself.
    private static final int SYSTEM_RENDERED_SDK = 37;

    /// The colour value [com.codename1.location.LocationButton] passes for "not
    /// chosen"; -1 rather than 0 because black is a colour a caller can ask for.
    private static final int UNSET_COLOR = -1;

    private static final String PKG = "android.app.permissionui.";

    /// `LocationButtonSession`'s text-type constants, in the order of the
    /// `TEXT_` constants on `com.codename1.location.LocationButton`.
    private static final String[] TEXT_TYPE_FIELDS = {
        "TEXT_TYPE_NONE",
        "TEXT_TYPE_PRECISE_LOCATION",
        "TEXT_TYPE_USE_PRECISE_LOCATION",
        "TEXT_TYPE_SHARE_PRECISE_LOCATION",
        "TEXT_TYPE_NEAR_MY_PRECISE_LOCATION",
        "TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION"
    };

    private final Activity activity;
    private final int textType;
    private final int backgroundColor;
    private final int textColor;
    private final SuccessCallback<Boolean> callback;

    /// android.app.permissionui.LocationButtonSession, once one is open.
    ///
    /// Volatile because this one field is read from a second thread. Everything
    /// else here lives on the Android UI thread, but [#hasSession()] is reached
    /// from `LocationButton.isSystemRendered()`, which application code calls on
    /// the Codename One EDT -- a different thread on this port. Without the
    /// publish there is no happens-before between adopting the surface here and
    /// observing it there, so a caller could keep seeing "not ready" after the
    /// control started drawing.
    ///
    /// This is not a contradiction of Codename One being single threaded: the
    /// EDT is still the only thread that runs application code, and this is the
    /// native boundary, which is exactly where a port is supposed to marshal.
    /// AndroidGLSurface.lastFrame and AndroidTextureView.created are the same
    /// shape.
    private volatile Object session;

    /// Set as soon as openSession has been asked for, so a second layout pass
    /// does not ask again while the first request is still in flight.
    private boolean requested;

    /// Which attachment the live request belongs to.
    ///
    /// Detaching closes the session and clears `requested`, so a detach and a
    /// quick reattach -- an ordinary Codename One form transition -- can put a
    /// second `openSession` in flight before the first one has answered. Every
    /// callback then arrives with the generation it was created for, and one
    /// that is no longer current is closed or dropped rather than acted on.
    /// Without it the first session would be adopted, silently overwritten by
    /// the second and never closed, and an error from the abandoned client
    /// would tear down the live control.
    ///
    /// A plain int, deliberately. Everything that reads or writes it runs on
    /// the Android UI thread: `openSession` is reached from `onSizeChanged` and
    /// from a `post()`, and the platform delivers its callbacks through
    /// [UiThreadExecutor]. There is no second thread here to synchronize with.
    private int generation;

    /// The size the live request or session was last told about, so a layout
    /// that happened while a request was in flight can be spotted when its
    /// session finally arrives.
    private int requestedWidth;

    private int requestedHeight;

    /// A failure is reported once. The component swaps itself for an ordinary
    /// Codename One button when it hears, and there is nothing to say twice.
    private boolean failed;

    AndroidLocationButton(Activity activity, int textType, int backgroundColor,
            int textColor, SuccessCallback<Boolean> callback) {
        super(activity);
        this.activity = activity;
        this.textType = textType;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.callback = callback;
        // Composite above the Codename One surface rather than being punched in
        // behind it -- the same reason AndroidGLSurface does this.
        setZOrderMediaOverlay(true);
    }

    /// Whether the platform has actually opened a session for this control.
    ///
    /// The view exists from the moment it is constructed; the surface the
    /// system draws into arrives later, or not at all. Only this distinguishes
    /// a working control from one that is present and blank.
    ///
    /// #### Returns
    ///
    /// whether a session is open
    boolean hasSession() {
        return session != null;
    }

    /// Whether this device will actually draw the control.
    ///
    /// The SDK check and the class lookup are both needed: the level says the
    /// platform should have it, and the lookup says this particular build of it
    /// does.
    ///
    /// #### Returns
    ///
    /// whether a session can be opened here
    static boolean isSupported() {
        if (Build.VERSION.SDK_INT < SYSTEM_RENDERED_SDK) {
            return false;
        }
        return findClass(PKG + "LocationButtonProviderFactory") != null
                && findClass(PKG + "LocationButtonProvider") != null
                && findClass(PKG + "LocationButtonClient") != null
                && findClass(PKG + "LocationButtonRequest$Builder") != null;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // A re-attach at an unchanged size never calls onSizeChanged, so the
        // session that detaching closed would never be asked for again.
        // Posted, because the host token this needs does not exist until the
        // view has been through a layout pass.
        post(new Runnable() {
            public void run() {
                if (isAttachedToWindow() && session == null
                        && getWidth() > 0 && getHeight() > 0) {
                    openSession(getWidth(), getHeight());
                }
            }
        });
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        if (session != null) {
            call(sessionClass(), session, "resize", new Class[]{int.class, int.class},
                    new Object[]{Integer.valueOf(w), Integer.valueOf(h)});
            requestedWidth = w;
            requestedHeight = h;
            return;
        }
        // The first pass that has a size is also the first pass with a host
        // token, so this is where a session can be asked for at all.
        openSession(w, h);
    }

    protected void onDetachedFromWindow() {
        closeSession();
        super.onDetachedFromWindow();
    }

    /// Asks the platform for a session, and reports a failure if it will not
    /// give one.
    private void openSession(int w, int h) {
        if (requested || failed) {
            return;
        }
        requested = true;
        generation++;
        try {
            Object provider = createProvider();
            if (provider == null) {
                fail(new IllegalStateException("no location button provider"));
                return;
            }
            IBinder hostToken = hostToken();
            if (hostToken == null) {
                fail(new IllegalStateException("the location button has no host token"));
                return;
            }
            Object request = createRequest(w, h);
            if (request == null) {
                fail(new IllegalStateException("the location button request could not be built"));
                return;
            }
            Class clientClass = findClass(PKG + "LocationButtonClient");
            Class requestClass = findClass(PKG + "LocationButtonRequest");
            // Looked up on the INTERFACE, never on provider.getClass(): the
            // object the factory returns is a platform-internal class, and a
            // Method taken from it is inaccessible to us however public the
            // method itself is.
            Method openSession = findClass(PKG + "LocationButtonProvider")
                    .getMethod("openSession",
                    new Class[]{Activity.class, IBinder.class, int.class,
                        requestClass, Executor.class, clientClass});
            openSession.setAccessible(true);
            openSession.invoke(provider, new Object[]{activity, hostToken,
                Integer.valueOf(displayId()), request, new UiThreadExecutor(),
                newClient(clientClass, generation)});
        } catch (Throwable t) {
            fail(t);
        }
    }

    /// Adopts the surface the platform drew the button into.
    private void sessionOpened(Object opened, int forGeneration) {
        if (opened == null) {
            fail(new IllegalStateException("the location button session was null"));
            return;
        }
        if (forGeneration != generation || !isAttachedToWindow()) {
            // Either the view went away while the session was being opened, or
            // a later attachment has already asked for one of its own. Closing
            // is what makes this safe: the abandoned session holds a surface in
            // another process, and dropping the reference would leak it.
            callQuietlyOn(sessionClass(), opened, "close", new Class[0], new Object[0]);
            return;
        }
        // NOT published yet. `session` is what hasSession() answers, and that is
        // read from the Codename One EDT as "the platform is drawing this", so
        // assigning it here would make it true for the length of a binder call
        // and a reflective invoke while the surface is still blank -- and true
        // for a moment before an adoption that fails, which ends in the
        // fallback. It is assigned once the surface is actually adopted, which
        // is the first instant the answer is true.
        Object surfacePackage = null;
        try {
            surfacePackage = callQuietlyOn(sessionClass(), opened,
                    "getSurfacePackage", new Class[0], new Object[0]);
            if (surfacePackage == null) {
                abandon(opened, new IllegalStateException(
                        "the location button session had no surface"));
                return;
            }
            Method setChild = null;
            Method[] methods = SurfaceView.class.getMethods();
            for (int iter = 0; iter < methods.length; iter++) {
                if ("setChildSurfacePackage".equals(methods[iter].getName())
                        && methods[iter].getParameterTypes().length == 1) {
                    setChild = methods[iter];
                    break;
                }
            }
            if (setChild == null) {
                abandon(opened, new NoSuchMethodException(
                        "SurfaceView.setChildSurfacePackage"));
                return;
            }
            setVisibility(VISIBLE);
            setChild.invoke(this, new Object[]{surfacePackage});
            session = opened;
            // The system's own control belongs in front of anything else this
            // surface carries; the platform's wrapper asks for the same order.
            callQuietly(this, "setCompositionOrder", new Class[]{int.class},
                    new Object[]{Integer.valueOf(1)});
            // The request carried the size this view had when it was made, and
            // a relayout in the meantime -- an orientation change is the easy
            // one -- was dropped: onSizeChanged has no session to resize yet and
            // will not ask for a second one while this request is in flight. So
            // the size is reconciled here, where both numbers finally exist.
            resizeToCurrent();
            invalidate();
        } catch (Throwable t) {
            abandon(opened, t);
        } finally {
            // The package is the caller's to release: setChildSurfacePackage
            // takes what it needs out of it, and holding this one keeps a
            // native SurfaceControl alive for nothing. Every rebuilt peer and
            // every detach/reattach opens another session, so keeping them
            // piles up graphics handles until a finalizer happens to run.
            //
            // In a finally because the adoption can leave by more routes than
            // the successful one -- a missing setChildSurfacePackage, or an
            // invoke that throws on some build of the platform -- and those are
            // exactly the devices that would then accumulate handles fastest.
            // Verified on an Android 17 emulator that the control still renders
            // and still grants after the release, including across a rotation,
            // which opens a second session and releases a second package.
            if (surfacePackage != null) {
                callQuietly(surfacePackage, "release", new Class[0], new Object[0]);
            }
        }
    }

    /// Gives up on a session that was opened but never adopted.
    ///
    /// It has to be closed here rather than left to [#closeSession()], which
    /// only knows about the published one: this session never became that, so
    /// dropping the reference would leak a surface in another process.
    private void abandon(Object opened, Throwable cause) {
        callQuietlyOn(sessionClass(), opened, "close", new Class[0], new Object[0]);
        fail(cause);
    }

    /// Tells an open session the size this view actually has.
    ///
    /// A no-op when they already agree, which is the common case; the platform
    /// is only asked when a layout pass happened while the session was opening.
    private void resizeToCurrent() {
        if (session == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || (w == requestedWidth && h == requestedHeight)) {
            return;
        }
        call(sessionClass(), session, "resize", new Class[]{int.class, int.class},
                new Object[]{Integer.valueOf(w), Integer.valueOf(h)});
        requestedWidth = w;
        requestedHeight = h;
    }

    private void closeSession() {
        if (session != null) {
            // Quiet: a session that will not close is being torn down anyway,
            // and reporting it would replace a control that is going away.
            callQuietlyOn(sessionClass(), session, "close", new Class[0], new Object[0]);
            session = null;
        }
        callQuietly(this, "clearChildSurfacePackage", new Class[0], new Object[0]);
        requested = false;
        // Closing ends the generation too, not just the session. A callback the
        // platform had already posted to this view's executor still runs after
        // this returns, and without the bump it would arrive looking current: a
        // queued grant would start acquiring a location for a control that is
        // gone, and a queued error would mark the view failed for good.
        generation++;
    }

    /// Reports that the platform's control is not going to work here.
    ///
    /// Null rather than FALSE: a decline and a broken control are different
    /// answers, and only one of them is worth replacing the control over.
    private void fail(Throwable t) {
        if (failed) {
            return;
        }
        failed = true;
        if (t != null) {
            Log.e(t);
        }
        closeSession();
        if (callback != null) {
            callback.onSucess(null);
        }
    }

    private Object createProvider() throws Exception {
        Class factory = findClass(PKG + "LocationButtonProviderFactory");
        if (factory == null) {
            return null;
        }
        Method create = factory.getMethod("create",
                new Class[]{android.content.Context.class});
        create.setAccessible(true);
        return create.invoke(null, new Object[]{activity});
    }

    private Object createRequest(int w, int h) throws Exception {
        Class builderClass = findClass(PKG + "LocationButtonRequest$Builder");
        if (builderClass == null) {
            return null;
        }
        Constructor ctor = builderClass.getConstructor(new Class[]{int.class,
            int.class, android.content.res.Configuration.class});
        Object builder = ctor.newInstance(new Object[]{Integer.valueOf(w),
            Integer.valueOf(h), getResources().getConfiguration()});
        requestedWidth = w;
        requestedHeight = h;
        callQuietly(builder, "setTextType", new Class[]{int.class},
                new Object[]{Integer.valueOf(platformTextType(textType))});
        if (backgroundColor != UNSET_COLOR) {
            callQuietly(builder, "setBackgroundColor", new Class[]{int.class},
                    new Object[]{Integer.valueOf(opaque(backgroundColor))});
        }
        if (textColor != UNSET_COLOR) {
            callQuietly(builder, "setTextColor", new Class[]{int.class},
                    new Object[]{Integer.valueOf(opaque(textColor))});
        }
        Method build = builderClass.getMethod("build", new Class[0]);
        return build.invoke(builder, new Object[0]);
    }

    /// Codename One colours are RRGGBB; Android's are ARGB and a zero alpha is
    /// invisible rather than opaque.
    private static int opaque(int color) {
        return color | 0xff000000;
    }

    private Object newClient(Class clientClass, int forGeneration) {
        return Proxy.newProxyInstance(clientClass.getClassLoader(),
                new Class[]{clientClass}, new ClientHandler(forGeneration));
    }

    private IBinder hostToken() {
        Object token = callQuietly(this, "getHostToken", new Class[0], new Object[0]);
        if (token instanceof IBinder) {
            return (IBinder) token;
        }
        return null;
    }

    private int displayId() {
        android.view.Display display = getDisplay();
        if (display != null) {
            return display.getDisplayId();
        }
        return android.view.Display.DEFAULT_DISPLAY;
    }

    private static Class findClass(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable t) {
            return null;
        }
    }

    /// The platform's own value for one of Codename One's text types.
    ///
    /// The two happen to agree today, and reading the constant by NAME is what
    /// keeps that from being a coincidence this code depends on silently. A
    /// name the platform does not have falls back to the number, which is the
    /// documented value and is what an older or newer platform would want
    /// anyway.
    private static int platformTextType(int textType) {
        if (textType < 0 || textType >= TEXT_TYPE_FIELDS.length) {
            return textType;
        }
        Class session = sessionClass();
        if (session == null) {
            return textType;
        }
        try {
            java.lang.reflect.Field f = session.getField(TEXT_TYPE_FIELDS[textType]);
            Object value = f.get(null);
            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }
        } catch (Throwable t) {
            // Fall through to the documented value.
        }
        return textType;
    }

    private static Class sessionClass() {
        return findClass(PKG + "LocationButtonSession");
    }

    /// Invokes a platform method, reporting a failure if it is not there.
    ///
    /// #### Parameters
    ///
    /// - `declaring`: the class to take the [java.lang.reflect.Method] from.
    ///   For anything the platform handed us this is the public *interface*: the
    ///   concrete object is an internal class of another package, and a Method
    ///   read off it throws IllegalAccessException on invoke no matter how
    ///   public the method is.
    private Object call(Class declaring, Object target, String name, Class[] signature,
            Object[] args) {
        if (declaring == null || target == null) {
            fail(new IllegalStateException("no " + name + " to call"));
            return null;
        }
        try {
            Method m = declaring.getMethod(name, signature);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            fail(t);
            return null;
        }
    }

    /// [#call] without the failure report, for a call whose failure changes
    /// nothing the caller can act on.
    private static Object callQuietlyOn(Class declaring, Object target, String name,
            Class[] signature, Object[] args) {
        if (declaring == null || target == null) {
            return null;
        }
        try {
            Method m = declaring.getMethod(name, signature);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    /// Invokes a platform method that this control can live without.
    ///
    /// Styling setters and the composition order are optional refinements of a
    /// button that works without them, and `getHostToken` legitimately answers
    /// nothing on a view that is not attached yet.
    private static Object callQuietly(Object target, String name, Class[] signature,
            Object[] args) {
        try {
            Method m = target.getClass().getMethod(name, signature);
            m.setAccessible(true);
            return m.invoke(target, args);
        } catch (Throwable t) {
            return null;
        }
    }

    /// Runs the platform's callbacks on the Android UI thread, which is where
    /// this view's surface has to be touched from.
    private static final class UiThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        public void execute(Runnable command) {
            handler.post(command);
        }
    }

    /// The `LocationButtonClient` this control hands to the platform.
    private final class ClientHandler implements InvocationHandler {

        /// The attachment this client was created for; see [#generation].
        private final int forGeneration;

        ClientHandler(int forGeneration) {
            this.forGeneration = forGeneration;
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("onSessionOpened".equals(name)) {
                sessionOpened(args == null || args.length == 0 ? null : args[0],
                        forGeneration);
                return null;
            }
            // A tap or an error from a session this view has already moved on
            // from says nothing about the control it is showing now, and acting
            // on it would grant against, or tear down, the wrong one.
            if ("onPermissionResult".equals(name)) {
                Object granted = args == null || args.length == 0 ? null : args[0];
                if (forGeneration == generation && callback != null
                        && granted instanceof Boolean) {
                    callback.onSucess((Boolean) granted);
                }
                return null;
            }
            if ("onSessionError".equals(name)) {
                if (forGeneration != generation) {
                    return null;
                }
                Object error = args == null || args.length == 0 ? null : args[0];
                fail(error instanceof Throwable ? (Throwable) error : null);
                return null;
            }
            // The three Object methods a Proxy is also asked to answer.
            if ("equals".equals(name)) {
                return Boolean.valueOf(proxy == (args == null ? null : args[0]));
            }
            if ("hashCode".equals(name)) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if ("toString".equals(name)) {
                return "CodenameOneLocationButtonClient";
            }
            return null;
        }
    }
}
