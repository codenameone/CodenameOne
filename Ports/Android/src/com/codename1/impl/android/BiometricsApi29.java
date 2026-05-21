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
package com.codename1.impl.android;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import com.codename1.io.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Executor;

/**
 * Reflection adapter for {@code android.hardware.biometrics.BiometricPrompt}
 * (API 28+) and {@code android.hardware.biometrics.BiometricManager}
 * (API 29+). The cn1-binaries {@code android.jar} predates API 28, so direct
 * symbol references would fail to compile; reflection lets us call these APIs
 * at runtime on supported devices without lifting the compile-time SDK
 * requirement of the Android port.
 *
 * <p>Only invoked from code paths guarded by
 * {@code Build.VERSION.SDK_INT >= 29} in {@link AndroidBiometrics} and
 * {@link AndroidSecureStorage}; on older devices the reflection class is
 * never loaded.</p>
 */
final class BiometricsApi29 {

    interface AuthCallback {
        void onSuccess();

        void onError(int errorCode, String errString);
    }

    interface CipherAuthCallback {
        /** Invoked with the authenticated {@code javax.crypto.Cipher}. */
        void onSuccess(Object cipher);

        void onError(int errorCode, String errString);
    }

    private BiometricsApi29() {
    }

    /** {@code BiometricManager.canAuthenticate() == BIOMETRIC_SUCCESS}. */
    static boolean canAuthenticate(Activity act) {
        try {
            Object bm = act.getSystemService("biometric");
            if (bm == null) {
                return false;
            }
            Object result = bm.getClass().getMethod("canAuthenticate").invoke(bm);
            return ((Integer) result).intValue() == 0; // BIOMETRIC_SUCCESS == 0
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /** Builds + shows a BiometricPrompt for plain authentication (no crypto). */
    static void authenticate(Activity act, String title, String subtitle,
                             String description, String negative,
                             CancellationSignal cs, AuthCallback cb) {
        try {
            Object prompt = buildPrompt(act, title, subtitle, description, negative, cb, null);
            Class<?> promptCls = Class.forName("android.hardware.biometrics.BiometricPrompt");
            Class<?> authCbCls = Class.forName("android.hardware.biometrics.BiometricPrompt$AuthenticationCallback");
            Executor exec = mainExecutor(act);
            Method authM = promptCls.getMethod("authenticate", CancellationSignal.class,
                    Executor.class, authCbCls);
            authM.invoke(prompt, cs, exec, makeAuthProxy(authCbCls, cb, null));
        } catch (Throwable t) {
            Log.e(t);
            cb.onError(AndroidBiometrics.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                    "Failed to invoke BiometricPrompt: " + t.getMessage());
        }
    }

    /**
     * Builds + shows a BiometricPrompt that wraps a CryptoObject around the
     * supplied {@code javax.crypto.Cipher}; on success the same cipher
     * (now authenticated) is passed to {@code cb.onSuccess}.
     */
    static void authenticateWithCipher(Activity act, String title, String subtitle,
                                       String description, String negative,
                                       Object cipher, CancellationSignal cs,
                                       CipherAuthCallback cb) {
        try {
            Object prompt = buildPrompt(act, title, subtitle, description, negative,
                    null, cb);
            Class<?> promptCls = Class.forName("android.hardware.biometrics.BiometricPrompt");
            Class<?> cryptoCls = Class.forName("android.hardware.biometrics.BiometricPrompt$CryptoObject");
            Class<?> authCbCls = Class.forName("android.hardware.biometrics.BiometricPrompt$AuthenticationCallback");
            Constructor<?> cryptoCtor = cryptoCls.getConstructor(Class.forName("javax.crypto.Cipher"));
            Object crypto = cryptoCtor.newInstance(cipher);
            Executor exec = mainExecutor(act);
            Method authM = promptCls.getMethod("authenticate", cryptoCls,
                    CancellationSignal.class, Executor.class, authCbCls);
            authM.invoke(prompt, crypto, cs, exec, makeAuthProxy(authCbCls, null, cb));
        } catch (Throwable t) {
            Log.e(t);
            cb.onError(AndroidBiometrics.BIOMETRIC_ERROR_HW_UNAVAILABLE,
                    "Failed to invoke BiometricPrompt with cipher: " + t.getMessage());
        }
    }

    private static Object buildPrompt(final Activity act, String title, String subtitle,
                                      String description, String negative,
                                      final AuthCallback acb, final CipherAuthCallback ccb) throws Exception {
        Class<?> builderCls = Class.forName("android.hardware.biometrics.BiometricPrompt$Builder");
        Object builder = builderCls.getConstructor(Context.class).newInstance(act);
        builderCls.getMethod("setTitle", CharSequence.class).invoke(builder, title);
        if (subtitle != null) {
            builderCls.getMethod("setSubtitle", CharSequence.class).invoke(builder, subtitle);
        }
        if (description != null) {
            builderCls.getMethod("setDescription", CharSequence.class).invoke(builder, description);
        }
        Executor exec = mainExecutor(act);
        DialogInterface.OnClickListener neg = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                if (acb != null) {
                    acb.onError(AndroidBiometrics.BIOMETRIC_ERROR_USER_CANCELED, "Cancelled");
                } else if (ccb != null) {
                    ccb.onError(AndroidBiometrics.BIOMETRIC_ERROR_USER_CANCELED, "Cancelled");
                }
            }
        };
        builderCls.getMethod("setNegativeButton", CharSequence.class, Executor.class,
                DialogInterface.OnClickListener.class).invoke(builder, negative, exec, neg);
        return builderCls.getMethod("build").invoke(builder);
    }

    private static Object makeAuthProxy(Class<?> authCbCls, final AuthCallback acb,
                                        final CipherAuthCallback ccb) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                try {
                    if ("onAuthenticationSucceeded".equals(name)) {
                        if (ccb != null) {
                            // BiometricPrompt$AuthenticationResult.getCryptoObject().getCipher()
                            Object ar = args[0];
                            Object crypto = ar.getClass().getMethod("getCryptoObject").invoke(ar);
                            Object cipher = crypto.getClass().getMethod("getCipher").invoke(crypto);
                            ccb.onSuccess(cipher);
                        } else if (acb != null) {
                            acb.onSuccess();
                        }
                    } else if ("onAuthenticationError".equals(name)) {
                        int code = ((Integer) args[0]).intValue();
                        String msg = args[1] == null ? "" : args[1].toString();
                        if (ccb != null) {
                            ccb.onError(code, msg);
                        } else if (acb != null) {
                            acb.onError(code, msg);
                        }
                    }
                    // onAuthenticationFailed / Help: ignore (soft-failure stream).
                } catch (Throwable t) {
                    Log.e(t);
                }
                return null;
            }
        };
        return Proxy.newProxyInstance(authCbCls.getClassLoader(),
                new Class<?>[]{authCbCls}, handler);
    }

    /** Activity.getMainExecutor() is API 28+; fall back to a Handler-backed executor. */
    static Executor mainExecutor(Context ctx) {
        try {
            return (Executor) Context.class.getMethod("getMainExecutor").invoke(ctx);
        } catch (Throwable t) {
            final Handler h = new Handler(Looper.getMainLooper());
            return new Executor() {
                @Override
                public void execute(Runnable r) {
                    h.post(r);
                }
            };
        }
    }
}
