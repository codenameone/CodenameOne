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
    private Object session;

    /// Set as soon as openSession has been asked for, so a second layout pass
    /// does not ask again while the first request is still in flight.
    private boolean requested;

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
                newClient(clientClass)});
        } catch (Throwable t) {
            fail(t);
        }
    }

    /// Adopts the surface the platform drew the button into.
    private void sessionOpened(Object opened) {
        if (opened == null) {
            fail(new IllegalStateException("the location button session was null"));
            return;
        }
        if (!isAttachedToWindow()) {
            // The view went away while the session was being opened; there is
            // nothing to show it in.
            callQuietlyOn(sessionClass(), opened, "close", new Class[0], new Object[0]);
            return;
        }
        session = opened;
        try {
            Object surfacePackage = call(sessionClass(), opened, "getSurfacePackage",
                    new Class[0], new Object[0]);
            if (surfacePackage == null) {
                fail(new IllegalStateException("the location button session had no surface"));
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
                fail(new NoSuchMethodException("SurfaceView.setChildSurfacePackage"));
                return;
            }
            setVisibility(VISIBLE);
            setChild.invoke(this, new Object[]{surfacePackage});
            // The system's own control belongs in front of anything else this
            // surface carries; the platform's wrapper asks for the same order.
            callQuietly(this, "setCompositionOrder", new Class[]{int.class},
                    new Object[]{Integer.valueOf(1)});
            invalidate();
        } catch (Throwable t) {
            fail(t);
        }
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

    private Object newClient(Class clientClass) {
        return Proxy.newProxyInstance(clientClass.getClassLoader(),
                new Class[]{clientClass}, new ClientHandler());
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

        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("onSessionOpened".equals(name)) {
                sessionOpened(args == null || args.length == 0 ? null : args[0]);
                return null;
            }
            if ("onPermissionResult".equals(name)) {
                Object granted = args == null || args.length == 0 ? null : args[0];
                if (callback != null && granted instanceof Boolean) {
                    callback.onSucess((Boolean) granted);
                }
                return null;
            }
            if ("onSessionError".equals(name)) {
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
