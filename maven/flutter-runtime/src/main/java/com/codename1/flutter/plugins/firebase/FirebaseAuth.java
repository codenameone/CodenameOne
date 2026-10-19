/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.ui.Display;

import dart.async.Completer;
import dart.async.Future;
import dart.async.Stream;
import dart.async.StreamController;
import dart.convert.Convert;
import dart.core.DartMap;
import dart.runtime.Funcs;

/**
 * firebase_auth over Google's Identity Toolkit REST API, authenticated with the
 * {@code apiKey} the app passed to {@code Firebase.initializeApp}. Email/password and
 * anonymous accounts are supported; the signed-in session (its refresh token) survives
 * a restart in Preferences. Providers that need a platform SDK -- Google, Apple, phone
 * -- fail with {@code operation-not-allowed}.
 */
public class FirebaseAuth {
    /** {@code FirebaseAuth.instance}. */
    public static final FirebaseAuth instance = new FirebaseAuth();

    private static final String TOOLKIT = "https://identitytoolkit.googleapis.com/v1/accounts:";
    private static final String SECURE_TOKEN = "https://securetoken.googleapis.com/v1/token?key=";
    private static final String PREF = "cn1_firebase_auth_";

    private final List<StreamController<User>> listeners = new ArrayList<StreamController<User>>();
    private User current;
    private boolean restored;

    FirebaseAuth() {
    }

    public static FirebaseAuth instanceFor(FirebaseApp app, Object persistence) {
        return instance;
    }

    public User currentUser() {
        restore();
        return current;
    }

    public Stream<User> authStateChanges() {
        return changes();
    }

    public Stream<User> idTokenChanges() {
        return changes();
    }

    public Stream<User> userChanges() {
        return changes();
    }

    /** A stream that starts with the current user, as firebase_auth's does. */
    private Stream<User> changes() {
        final StreamController<User> c = new StreamController<User>();
        c.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                c.add(currentUser());
            }
        });
        synchronized (listeners) {
            listeners.add(c);
        }
        return c.stream();
    }

    private void publish() {
        List<StreamController<User>> copy;
        synchronized (listeners) {
            copy = new ArrayList<StreamController<User>>(listeners);
        }
        for (StreamController<User> c : copy) {
            if (c.isClosed()) {
                synchronized (listeners) {
                    listeners.remove(c);
                }
            } else if (c.hasListener()) {
                c.add(current);
            }
        }
    }

    // --- sign in / out --------------------------------------------------------

    public Future<UserCredential> signInWithEmailAndPassword(String email, String password) {
        return signIn("signInWithPassword", DartMap.<String, Object>of("email", email, "password", password,
                "returnSecureToken", Boolean.TRUE), false);
    }

    public Future<UserCredential> createUserWithEmailAndPassword(String email, String password) {
        return signIn("signUp", DartMap.<String, Object>of("email", email, "password", password,
                "returnSecureToken", Boolean.TRUE), false);
    }

    public Future<UserCredential> signInAnonymously() {
        return signIn("signUp", DartMap.<String, Object>of("returnSecureToken", Boolean.TRUE), true);
    }

    public Future<UserCredential> signInWithCredential(Object credential) {
        return Future.error(new FirebaseAuthException("operation-not-allowed",
                "Only email/password and anonymous sign-in are supported in Codename One"));
    }

    public Future<UserCredential> signInWithProvider(Object provider) {
        return signInWithCredential(provider);
    }

    public Future<Object> signOut() {
        current = null;
        persist();
        publish();
        return Future.value(null);
    }

    public Future<Object> sendPasswordResetEmail(String email, Object actionCodeSettings) {
        return sendOob(null, "PASSWORD_RESET", email);
    }

    public Future<Object> setLanguageCode(String code) {
        return Future.value(null);
    }

    private Future<UserCredential> signIn(String op, Map<String, Object> body, final boolean anonymous) {
        final Completer<UserCredential> done = new Completer<UserCredential>();
        call(TOOLKIT + op + "?key=", body, new Funcs.VoidFunc1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> r) {
                User u = new User(str(r.get("localId")), str(r.get("email")), anonymous);
                u.displayName = str(r.get("displayName"));
                u.idToken = str(r.get("idToken"));
                u.refreshToken = str(r.get("refreshToken"));
                u.expiresAt = expiry(r.get("expiresIn"));
                current = u;
                persist();
                publish();
                done.complete(new UserCredential(u));
            }
        }, done);
        return done.future();
    }

    Future<Object> sendOob(User user, String type, String email) {
        final Completer<Object> done = new Completer<Object>();
        DartMap<String, Object> body = DartMap.<String, Object>of("requestType", type);
        if (email != null) {
            body.put("email", email);
        }
        if (user != null) {
            body.put("idToken", user.idToken);
        }
        call(TOOLKIT + "sendOobCode?key=", body, new Funcs.VoidFunc1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> r) {
                done.complete(null);
            }
        }, done);
        return done.future();
    }

    Future<Object> updateProfile(final User user, final String name, final String photo) {
        final Completer<Object> done = new Completer<Object>();
        DartMap<String, Object> body = DartMap.<String, Object>of("idToken", user.idToken,
                "returnSecureToken", Boolean.FALSE);
        if (name != null) {
            body.put("displayName", name);
        }
        if (photo != null) {
            body.put("photoUrl", photo);
        }
        call(TOOLKIT + "update?key=", body, new Funcs.VoidFunc1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> r) {
                user.displayName = name;
                user.photoURL = photo;
                persist();
                publish();
                done.complete(null);
            }
        }, done);
        return done.future();
    }

    Future<Object> deleteUser(User user) {
        final Completer<Object> done = new Completer<Object>();
        call(TOOLKIT + "delete?key=", DartMap.<String, Object>of("idToken", user.idToken),
                new Funcs.VoidFunc1<Map<String, Object>>() {
                    @Override
                    public void call(Map<String, Object> r) {
                        current = null;
                        persist();
                        publish();
                        done.complete(null);
                    }
                }, done);
        return done.future();
    }

    Future<String> idToken(final User user, boolean force) {
        if (!force && user.idToken != null && System.currentTimeMillis() < user.expiresAt - 60000) {
            return Future.value(user.idToken);
        }
        final Completer<String> done = new Completer<String>();
        String form = "grant_type=refresh_token&refresh_token=" + user.refreshToken;
        call(SECURE_TOKEN, form, new Funcs.VoidFunc1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> r) {
                user.idToken = str(r.get("id_token"));
                user.refreshToken = str(r.get("refresh_token"));
                user.expiresAt = expiry(r.get("expires_in"));
                persist();
                done.complete(user.idToken);
            }
        }, done);
        return done.future();
    }

    // --- transport --------------------------------------------------------------

    private static String apiKey() {
        FirebaseApp app = Firebase.find(Firebase.DEFAULT_APP_NAME);
        return app == null ? null : app.options().apiKey();
    }

    /**
     * POSTs {@code body} (a map sent as JSON, or a form string) to {@code url + apiKey}
     * and hands the decoded JSON reply to {@code ok}, or fails {@code failed} with the
     * FirebaseAuthException the reply's error code maps to.
     */
    private static void call(String url, Object body, final Funcs.VoidFunc1<Map<String, Object>> ok,
            final Completer<?> failed) {
        String key = apiKey();
        if (key == null) {
            failed.completeError(new FirebaseAuthException("no-app",
                    "Call Firebase.initializeApp with FirebaseOptions that carry an apiKey first"));
            return;
        }
        if (!Display.isInitialized()) {
            failed.completeError(new FirebaseAuthException("network-request-failed",
                    "A network error has occurred: Codename One is not initialized"));
            return;
        }
        final boolean form = body instanceof String;
        final byte[] payload;
        try {
            payload = (form ? (String) body : Convert.jsonEncode(body)).getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            failed.completeError(e);
            return;
        }
        final ByteArrayOutputStream received = new ByteArrayOutputStream();
        ConnectionRequest req = new ConnectionRequest() {
            private Exception failure;

            @Override
            protected void buildRequestBody(OutputStream os) throws IOException {
                os.write(payload);
            }

            @Override
            protected void readResponse(InputStream input) throws IOException {
                byte[] buf = new byte[4096];
                int n;
                while ((n = input.read(buf)) > 0) {
                    received.write(buf, 0, n);
                }
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // the error body carries Firebase's code; read below
            }

            @Override
            protected void handleException(Exception err) {
                failure = err;
            }

            @Override
            protected void postResponse() {
                if (failure != null) {
                    failed.completeError(new FirebaseAuthException("network-request-failed",
                            String.valueOf(failure.getMessage())));
                    return;
                }
                Map<String, Object> r;
                try {
                    Object decoded = Convert.jsonDecode(new String(received.toByteArray(), "UTF-8"));
                    r = decoded instanceof Map ? asMap(decoded) : new DartMap<String, Object>();
                } catch (Exception e) {
                    failed.completeError(new FirebaseAuthException("internal-error", String.valueOf(e)));
                    return;
                }
                Object err = r.get("error");
                if (err instanceof Map || getResponseCode() >= 400) {
                    String raw = err instanceof Map ? str(((Map<?, ?>) err).get("message")) : null;
                    failed.completeError(mapError(raw == null ? "HTTP " + getResponseCode() : raw));
                    return;
                }
                ok.call(r);
            }
        };
        req.setUrl(url + key);
        req.setPost(true);
        req.setHttpMethod("POST");
        req.setContentType(form ? "application/x-www-form-urlencoded" : "application/json");
        req.setFailSilently(true);
        req.setReadResponseForErrors(true);
        req.setDuplicateSupported(true);
        NetworkManager.getInstance().addToQueue(req);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (Map<String, Object>) o;
    }

    /** Identity Toolkit's error strings ("EMAIL_NOT_FOUND : ...") to firebase_auth codes. */
    static FirebaseAuthException mapError(String raw) {
        int sp = raw.indexOf(' ');
        String key = sp < 0 ? raw : raw.substring(0, sp);
        String code;
        if (key.equals("EMAIL_NOT_FOUND")) {
            code = "user-not-found";
        } else if (key.equals("INVALID_PASSWORD")) {
            code = "wrong-password";
        } else if (key.equals("INVALID_LOGIN_CREDENTIALS")) {
            code = "invalid-credential";
        } else if (key.equals("EMAIL_EXISTS")) {
            code = "email-already-in-use";
        } else if (key.equals("WEAK_PASSWORD")) {
            code = "weak-password";
        } else if (key.equals("INVALID_EMAIL")) {
            code = "invalid-email";
        } else if (key.equals("USER_DISABLED")) {
            code = "user-disabled";
        } else if (key.equals("TOO_MANY_ATTEMPTS_TRY_LATER")) {
            code = "too-many-requests";
        } else if (key.equals("OPERATION_NOT_ALLOWED")) {
            code = "operation-not-allowed";
        } else if (key.equals("TOKEN_EXPIRED") || key.equals("INVALID_ID_TOKEN")
                || key.equals("INVALID_REFRESH_TOKEN")) {
            code = "user-token-expired";
        } else {
            code = "unknown";
        }
        return new FirebaseAuthException(code, raw);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static long expiry(Object seconds) {
        long s = 3600;
        if (seconds != null) {
            try {
                s = Long.parseLong(String.valueOf(seconds));
            } catch (NumberFormatException ignore) {
                // keep the hour Firebase issues
            }
        }
        return System.currentTimeMillis() + s * 1000;
    }

    // --- persistence ------------------------------------------------------------

    private void persist() {
        if (!Display.isInitialized()) {
            return;
        }
        if (current == null) {
            Preferences.delete(PREF + "uid");
            return;
        }
        Preferences.set(PREF + "uid", current.uid);
        Preferences.set(PREF + "email", current.email == null ? "" : current.email);
        Preferences.set(PREF + "name", current.displayName == null ? "" : current.displayName);
        Preferences.set(PREF + "refresh", current.refreshToken == null ? "" : current.refreshToken);
        Preferences.set(PREF + "anonymous", current.anonymous ? "true" : "false");
    }

    private void restore() {
        if (restored || !Display.isInitialized()) {
            return;
        }
        restored = true;
        String uid = Preferences.get(PREF + "uid", null);
        if (uid == null || current != null) {
            return;
        }
        User u = new User(uid, blankToNull(Preferences.get(PREF + "email", "")),
                "true".equals(Preferences.get(PREF + "anonymous", "false")));
        u.displayName = blankToNull(Preferences.get(PREF + "name", ""));
        u.refreshToken = blankToNull(Preferences.get(PREF + "refresh", ""));
        current = u;
    }

    private static String blankToNull(String s) {
        return s == null || s.length() == 0 ? null : s;
    }
}
