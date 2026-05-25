/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.webauthn;

import android.app.Activity;

import com.codename1.impl.android.AndroidNativeUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Android implementation of {@link WebAuthnNative}. Uses the
 * {@code androidx.credentials.CredentialManager} API (passkeys, FIDO2,
 * password manager hub) when the {@code androidx.credentials:credentials}
 * dependency is on the runtime classpath -- which {@code AndroidGradleBuilder}
 * auto-injects when the app references {@code com.codename1.io.webauthn.*}.
 *
 * <p>Lookup is performed via reflection so the Codename One Android port
 * itself (which ships as Java sources) can build without
 * {@code androidx.credentials} on its compile classpath.
 *
 * <p>Flow (registration; sign-in is symmetrical):
 * <ol>
 *   <li>{@link #createPasskey(String)} is invoked on a worker thread.</li>
 *   <li>We construct a {@code CreatePublicKeyCredentialRequest} from the
 *       server-supplied options JSON and submit it via
 *       {@code CredentialManager.createCredentialAsync(...)}.</li>
 *   <li>{@code CredentialManager} shows the OS passkey sheet and invokes
 *       our callback on completion or error.</li>
 *   <li>The worker thread unblocks via {@link CountDownLatch} and returns
 *       the registration response JSON (or throws
 *       {@link WebAuthnException}).</li>
 * </ol>
 */
public class WebAuthnNativeImpl implements WebAuthnNative {

    /** Invoked from the generated Android app stub at startup. */
    public static void init() {
        WebAuthnClient.setProvider(new WebAuthnNativeImpl());
    }

    public boolean isSupported() {
        if (AndroidNativeUtil.getActivity() == null) {
            return false;
        }
        try {
            Class.forName("androidx.credentials.CredentialManager");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public String createPasskey(String optionsJson) throws WebAuthnException {
        return runCredentialFlow(optionsJson, /* create= */ true);
    }

    public String getPasskey(String optionsJson) throws WebAuthnException {
        return runCredentialFlow(optionsJson, /* create= */ false);
    }

    /**
     * Submits the request to {@code CredentialManager} via reflection and
     * blocks the calling worker thread until the OS sheet resolves.
     */
    private static String runCredentialFlow(String optionsJson, boolean create)
            throws WebAuthnException {
        final Activity activity = AndroidNativeUtil.getActivity();
        if (activity == null) {
            throw new WebAuthnException(WebAuthnException.NOT_IMPLEMENTED,
                    "No Activity available");
        }
        if (optionsJson == null) {
            throw new WebAuthnException(WebAuthnException.INVALID_OPTIONS,
                    "optionsJson must not be null");
        }
        try {
            // androidx.credentials.CredentialManager.create(context);
            Class<?> cmCls = Class.forName("androidx.credentials.CredentialManager");
            Method cmFactory = cmCls.getMethod("create", android.content.Context.class);
            final Object cm = cmFactory.invoke(null, activity);

            // Build the request: CreatePublicKeyCredentialRequest(json) /
            // GetCredentialRequest.Builder().addCredentialOption(
            //     GetPublicKeyCredentialOption(json)).build()
            final Object request = create
                    ? buildCreateRequest(optionsJson)
                    : buildGetRequest(optionsJson);

            // Callback is a dynamic proxy of CredentialManagerCallback. We
            // block on a latch and capture the result / exception below.
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Object> resultRef = new AtomicReference<Object>();
            final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();

            Class<?> callbackCls = Class.forName(
                    "androidx.credentials.CredentialManagerCallback");
            Object callback = Proxy.newProxyInstance(
                    callbackCls.getClassLoader(),
                    new Class<?>[]{callbackCls},
                    new InvocationHandler() {
                        public Object invoke(Object proxy, Method m, Object[] args) {
                            String name = m.getName();
                            if ("onResult".equals(name)) {
                                resultRef.set(args[0]);
                                latch.countDown();
                            } else if ("onError".equals(name)) {
                                errorRef.set(args.length > 0 && args[0] instanceof Throwable
                                        ? (Throwable) args[0] : null);
                                latch.countDown();
                            }
                            return null;
                        }
                    });

            // Run synchronously on the calling worker thread; the OS sheet
            // is launched on the main thread by CredentialManager itself.
            Executor inlineExecutor = new Executor() {
                public void execute(Runnable r) { r.run(); }
            };

            // The method name differs between createCredentialAsync /
            // getCredentialAsync; their signature is identical.
            String methodName = create ? "createCredentialAsync" : "getCredentialAsync";
            Class<?> requestParamCls = create
                    ? Class.forName("androidx.credentials.CreateCredentialRequest")
                    : Class.forName("androidx.credentials.GetCredentialRequest");
            Method asyncCall = cmCls.getMethod(
                    methodName,
                    android.content.Context.class,
                    requestParamCls,
                    android.os.CancellationSignal.class,
                    Executor.class,
                    callbackCls);

            // Some pre-1.3 versions delivered the call on the main thread,
            // which would deadlock if we also blocked the main thread.
            // We're on a worker (WebAuthnClient.create/get spawns a Thread)
            // so a latch.await is safe.
            activity.runOnUiThread(new InvokeAsyncRunnable(
                    asyncCall, cm, activity, request, inlineExecutor, callback));
            latch.await();

            if (errorRef.get() != null) {
                throw mapCredentialError(errorRef.get());
            }
            Object response = resultRef.get();
            if (response == null) {
                return null;
            }
            return create ? extractRegistrationJson(response)
                          : extractAuthenticationJson(response);
        } catch (WebAuthnException wae) {
            throw wae;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new WebAuthnException(WebAuthnException.ABORTED,
                    "Passkey ceremony interrupted", ie);
        } catch (ClassNotFoundException cnfe) {
            throw new WebAuthnException(WebAuthnException.NOT_IMPLEMENTED,
                    "androidx.credentials is not on the runtime classpath", cnfe);
        } catch (NoSuchMethodException nsme) {
            throw new WebAuthnException(WebAuthnException.NOT_IMPLEMENTED,
                    "androidx.credentials API surface unexpected: " + nsme.getMessage(), nsme);
        } catch (Throwable t) {
            throw new WebAuthnException(WebAuthnException.TRANSPORT_ERROR,
                    "Native passkey flow failed: " + t.getMessage(), t);
        }
    }

    /**
     * Schedule the async call on the UI thread (CredentialManager touches
     * the WindowManager when presenting the OS sheet, and 1.2.x crashes when
     * called off-main without an explicit Looper).
     */
    private static final class InvokeAsyncRunnable implements Runnable {
        private final Method method;
        private final Object cm;
        private final android.content.Context ctx;
        private final Object request;
        private final Executor executor;
        private final Object callback;

        InvokeAsyncRunnable(Method method, Object cm, android.content.Context ctx,
                            Object request, Executor executor, Object callback) {
            this.method = method;
            this.cm = cm;
            this.ctx = ctx;
            this.request = request;
            this.executor = executor;
            this.callback = callback;
        }

        public void run() {
            try {
                method.invoke(cm, ctx, request, null, executor, callback);
            } catch (Throwable t) {
                // The reflective call itself failed (rare; reflects an API
                // surface mismatch). Surface via callback so the worker can
                // unblock.
                try {
                    Method onError = callback.getClass().getMethod("onError",
                            Throwable.class);
                    onError.invoke(callback, t);
                } catch (Throwable ignored) {
                    // Already failing; nothing more we can do here.
                }
            }
        }
    }

    private static Object buildCreateRequest(String optionsJson) throws Exception {
        // new CreatePublicKeyCredentialRequest(optionsJson)
        Class<?> reqCls = Class.forName(
                "androidx.credentials.CreatePublicKeyCredentialRequest");
        Constructor<?> ctor = reqCls.getConstructor(String.class);
        return ctor.newInstance(optionsJson);
    }

    private static Object buildGetRequest(String optionsJson) throws Exception {
        // new GetCredentialRequest.Builder()
        //     .addCredentialOption(new GetPublicKeyCredentialOption(json))
        //     .build();
        Class<?> optCls = Class.forName(
                "androidx.credentials.GetPublicKeyCredentialOption");
        Constructor<?> optCtor = optCls.getConstructor(String.class);
        Object option = optCtor.newInstance(optionsJson);

        Class<?> builderCls = Class.forName(
                "androidx.credentials.GetCredentialRequest$Builder");
        Object builder = builderCls.getConstructor().newInstance();
        Class<?> optionBaseCls = Class.forName(
                "androidx.credentials.CredentialOption");
        Method addOpt = builderCls.getMethod("addCredentialOption", optionBaseCls);
        addOpt.invoke(builder, option);
        Method build = builderCls.getMethod("build");
        return build.invoke(builder);
    }

    /**
     * Returns the {@code registrationResponseJson} from a
     * {@code CreatePublicKeyCredentialResponse}.
     */
    private static String extractRegistrationJson(Object response) throws Exception {
        Class<?> rspCls = Class.forName(
                "androidx.credentials.CreatePublicKeyCredentialResponse");
        if (!rspCls.isInstance(response)) {
            throw new WebAuthnException(WebAuthnException.INVALID_RESPONSE,
                    "Unexpected CreateCredentialResponse type: "
                            + response.getClass().getName());
        }
        Method m = rspCls.getMethod("getRegistrationResponseJson");
        Object json = m.invoke(response);
        return json == null ? null : json.toString();
    }

    /**
     * Returns the {@code authenticationResponseJson} from the
     * {@code PublicKeyCredential} attached to a {@code GetCredentialResponse}.
     */
    private static String extractAuthenticationJson(Object response) throws Exception {
        Class<?> rspCls = Class.forName("androidx.credentials.GetCredentialResponse");
        Method getCred = rspCls.getMethod("getCredential");
        Object credential = getCred.invoke(response);
        if (credential == null) {
            return null;
        }
        Class<?> pkCredCls = Class.forName("androidx.credentials.PublicKeyCredential");
        if (!pkCredCls.isInstance(credential)) {
            throw new WebAuthnException(WebAuthnException.INVALID_RESPONSE,
                    "Unexpected Credential type: " + credential.getClass().getName());
        }
        Method m = pkCredCls.getMethod("getAuthenticationResponseJson");
        Object json = m.invoke(credential);
        return json == null ? null : json.toString();
    }

    /**
     * Maps androidx.credentials exception class names onto the W3C-style
     * codes used by {@link WebAuthnException}. The names follow a stable
     * naming convention so we recognise them by simple name even if the
     * package has been shaded.
     */
    private static WebAuthnException mapCredentialError(Throwable t) {
        String name = t.getClass().getSimpleName();
        String message = t.getMessage();
        String code;
        if (name.contains("UserCanceled")
                || name.contains("InterruptedException")
                || name.equals("CreateCredentialCancellationException")
                || name.equals("GetCredentialCancellationException")) {
            code = WebAuthnException.NOT_ALLOWED;
        } else if (name.contains("NoCredentialException")
                || name.contains("CreateCredentialInterrupted")) {
            code = WebAuthnException.NOT_ALLOWED;
        } else if (name.contains("NotSupported")
                || name.equals("CreateCredentialProviderConfigurationException")
                || name.equals("GetCredentialProviderConfigurationException")) {
            code = WebAuthnException.NOT_SUPPORTED;
        } else if (name.contains("Domain")) {
            code = WebAuthnException.SECURITY_ERROR;
        } else if (name.contains("UnsupportedException")) {
            code = WebAuthnException.NOT_SUPPORTED;
        } else {
            code = WebAuthnException.TRANSPORT_ERROR;
        }
        return new WebAuthnException(code,
                message != null ? message : t.getClass().getName(), t);
    }
}
