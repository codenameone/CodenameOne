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
package com.codename1.io.oidc;

import com.codename1.io.Preferences;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.SecureStorage;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

/// A [TokenStore] that keeps an [OidcClient]'s tokens in the platform's secure storage -- the
/// iOS keychain, the Android keystore, and what each desktop and browser port provides -- instead
/// of the application's ordinary [com.codename1.io.Storage].
///
/// ```java
/// client.setTokenStore(new SecureStorageTokenStore());
/// ```
///
/// A refresh token is a long-lived credential: whoever reads it can keep a session alive without
/// the user. That is the reason to prefer this store over the default one for any application
/// that signs in to something that matters.
///
/// #### Quiet and gated
///
/// By default the store uses the non-prompting half of [SecureStorage]: entries are encrypted by
/// the operating system and read without asking the user, which is what a token attached to
/// every request needs. [#requireBiometrics(String)] switches to the gated half, where reading
/// (and on Android writing) shows a biometric prompt. An application that does this should load
/// its tokens once, when it starts, and keep them in memory -- [OidcRequestAuthorizer] does.
/// Loading prompts only when this store saved something to read: before the first sign-in, and
/// after [#clear(String)], it completes with null without asking the user anything.
///
/// #### A platform with no secure storage
///
/// Nothing is downgraded silently. On a port with no secure storage every operation fails with
/// an [OidcException] whose code is [OidcException#STORAGE_UNAVAILABLE], so the application
/// finds out the first time it runs there rather than discovering later where its refresh tokens
/// went. [#allowPlainStorageFallback(boolean)] is the explicit opt-in: with it, such a platform
/// gets [TokenStore.DefaultStorageTokenStore] instead. The fallback is only used where secure
/// storage cannot be reached at all, never because one write failed.
///
/// Both stores write the same document, so an entry can be copied from one to the other as is.
public final class SecureStorageTokenStore implements TokenStore {

    private static final String PREFIX = "cn1.oidc.";
    /// The preference under which the store notes that it holds a gated entry for a key.
    private static final String HELD = "cn1.oidc.held.";

    private final SecureStorage fixedStorage;
    private final TokenStore plain = new TokenStore.DefaultStorageTokenStore();
    private String biometricReason;
    private boolean plainFallback;

    /// A store over the platform's [SecureStorage#getInstance()].
    public SecureStorageTokenStore() {
        this(null);
    }

    /// A store over a particular [SecureStorage].
    ///
    /// #### Parameters
    ///
    /// - `storage`: the storage to keep entries in, or null for the platform's own, looked up
    ///   on each use
    public SecureStorageTokenStore(SecureStorage storage) {
        this.fixedStorage = storage;
    }

    /// Keeps the tokens behind a biometric prompt.
    ///
    /// Reading then asks the user to authenticate, and on Android so does writing. Entries are
    /// bound to the enrolled biometrics: after the user enrolls a new finger or face the stored
    /// entry is gone for good, [#load(String)] completes with null, and the user signs in again.
    ///
    /// An entry written quietly is not visible to the gated half and the other way around, so
    /// decide once per application.
    ///
    /// #### Parameters
    ///
    /// - `reason`: the text of the prompt, or null to go back to quiet storage
    ///
    /// #### Returns
    ///
    /// this store
    public SecureStorageTokenStore requireBiometrics(String reason) {
        this.biometricReason = reason;
        return this;
    }

    /// Whether a platform with no secure storage may keep the tokens in ordinary
    /// [com.codename1.io.Storage] instead. Off by default.
    ///
    /// #### Parameters
    ///
    /// - `allow`: true to fall back on such a platform, false to fail there
    ///
    /// #### Returns
    ///
    /// this store
    public SecureStorageTokenStore allowPlainStorageFallback(boolean allow) {
        this.plainFallback = allow;
        return this;
    }

    @Override
    public AsyncResource<OidcTokens> load(final String key) {
        final AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        final SecureStorage storage = storage();
        final String account = PREFIX + key;
        if (biometricReason != null) {
            // Nothing was ever saved here: signed out, and no prompt to find that out.
            // The gated half of SecureStorage cannot be asked whether an entry exists.
            // Reading one that does not is a failure on every port, and the same failure
            // as a keychain that could not answer -- so the first launch of an application
            // that asked for biometrics failed with "storage unavailable" before anyone
            // had signed in. The store therefore notes for itself, in the application's
            // preferences, that it saved an entry; the note says that and nothing else.
            if (!Preferences.get(HELD + key, false)) {
                out.complete(null);
                return out;
            }
            storage.get(biometricReason, account)
                    .ready(new SuccessCallback<String>() {
                        @Override
                        public void onSucess(String stored) {
                            completeLoad(out, stored);
                        }
                    })
                    .except(new SuccessCallback<Throwable>() {
                        @Override
                        public void onSucess(Throwable err) {
                            if (isRevoked(err)) {
                                // The entry was bound to biometrics that have since changed.
                                // It cannot be read again by anyone, which for the caller is
                                // the same as not being signed in.
                                Preferences.delete(HELD + key);
                                out.complete(null);
                            } else if (isUnavailable(err) && plainFallback) {
                                forward(plain.load(key), out);
                            } else {
                                out.error(failure(err, "read"));
                            }
                        }
                    });
            return out;
        }
        try {
            String stored = storage.get(account);
            if (stored != null) {
                completeLoad(out, stored);
                return out;
            }
            int state = storage.entryState(account);
            if (state == SecureStorage.ENTRY_ABSENT) {
                out.complete(null);
            } else if (state == SecureStorage.ENTRY_PRESENT) {
                // There, and unreadable. Answering null would tell the application it is
                // signed out and invite it to overwrite an entry that may only be locked.
                out.error(new OidcException(OidcException.STORAGE_UNAVAILABLE,
                        "The stored tokens exist but could not be read from secure storage"));
            } else if (plainFallback) {
                forward(plain.load(key), out);
            } else {
                out.error(unavailable("read"));
            }
        } catch (RuntimeException err) {
            out.error(failure(err, "read"));
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> save(final String key, OidcTokens tokens) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        final SecureStorage storage = storage();
        final String account = PREFIX + key;
        final OidcTokens toSave = tokens;
        final String json = TokenJson.toJson(tokens);
        if (biometricReason != null) {
            storage.set(biometricReason, account, json)
                    .ready(new SuccessCallback<Boolean>() {
                        @Override
                        public void onSucess(Boolean stored) {
                            noteHeld(key, stored);
                            out.complete(stored);
                        }
                    })
                    .except(new SuccessCallback<Throwable>() {
                        @Override
                        public void onSucess(Throwable err) {
                            if (isUnavailable(err) && plainFallback) {
                                plain.save(key, toSave).ready(new SuccessCallback<Boolean>() {
                                    @Override
                                    public void onSucess(Boolean stored) {
                                        noteHeld(key, stored);
                                        out.complete(stored);
                                    }
                                }).except(new SuccessCallback<Throwable>() {
                                    @Override
                                    public void onSucess(Throwable failed) {
                                        out.error(failed);
                                    }
                                });
                            } else {
                                out.error(failure(err, "write"));
                            }
                        }
                    });
            return out;
        }
        try {
            if (storage.set(account, json)) {
                out.complete(Boolean.TRUE);
            } else if (storage.entryState(account) != SecureStorage.ENTRY_UNKNOWN) {
                // A store that answers about its entries is there; this write failed. That is
                // not what the fallback is for -- the next write may well succeed, and the
                // tokens would then live in two places.
                out.error(new OidcException(OidcException.STORAGE_UNAVAILABLE,
                        "Secure storage refused to store the tokens"));
            } else if (plainFallback) {
                forward(plain.save(key, toSave), out);
            } else {
                out.error(unavailable("write"));
            }
        } catch (RuntimeException err) {
            out.error(failure(err, "write"));
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> clear(final String key) {
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        final SecureStorage storage = storage();
        final String account = PREFIX + key;
        if (biometricReason != null) {
            // Forgotten first: whatever the removal answers, the caller asked to be
            // signed out, and the next load must not prompt for an entry it gave up.
            Preferences.delete(HELD + key);
            storage.remove(biometricReason, account)
                    .ready(new SuccessCallback<Boolean>() {
                        @Override
                        public void onSucess(Boolean removed) {
                            out.complete(removed);
                        }
                    })
                    .except(new SuccessCallback<Throwable>() {
                        @Override
                        public void onSucess(Throwable err) {
                            if (isUnavailable(err) && plainFallback) {
                                forward(plain.clear(key), out);
                            } else {
                                out.error(failure(err, "remove"));
                            }
                        }
                    });
            return out;
        }
        try {
            if (storage.remove(account)) {
                out.complete(Boolean.TRUE);
            } else if (storage.entryState(account) != SecureStorage.ENTRY_UNKNOWN) {
                // Nothing was stored, which is what the caller wanted to end up with.
                out.complete(Boolean.FALSE);
            } else if (plainFallback) {
                forward(plain.clear(key), out);
            } else {
                out.error(unavailable("remove"));
            }
        } catch (RuntimeException err) {
            out.error(failure(err, "remove"));
        }
        return out;
    }

    /// Notes that a gated entry was saved for `key`; see [#load(String)].
    private static void noteHeld(String key, Boolean stored) {
        if (Boolean.TRUE.equals(stored)) {
            Preferences.set(HELD + key, true);
        }
    }

    private SecureStorage storage() {
        return fixedStorage != null ? fixedStorage : SecureStorage.getInstance();
    }

    private static void completeLoad(AsyncResource<OidcTokens> out, String stored) {
        try {
            out.complete(TokenJson.fromJson(stored));
        } catch (Exception err) {
            out.error(new OidcException(OidcException.STORAGE_UNAVAILABLE,
                    "The stored tokens could not be parsed", err));
        }
    }

    private static <T> void forward(AsyncResource<T> from, final AsyncResource<T> to) {
        from.ready(new SuccessCallback<T>() {
            @Override
            public void onSucess(T value) {
                to.complete(value);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable err) {
                to.error(err);
            }
        });
    }

    private static boolean isUnavailable(Throwable err) {
        return err instanceof BiometricException
                && ((BiometricException) err).getError() == BiometricError.NOT_AVAILABLE;
    }

    private static boolean isRevoked(Throwable err) {
        return err instanceof BiometricException
                && ((BiometricException) err).getError() == BiometricError.KEY_REVOKED;
    }

    private static OidcException unavailable(String operation) {
        return new OidcException(OidcException.STORAGE_UNAVAILABLE,
                "Cannot " + operation + " tokens: this platform has no secure storage. "
                + "Call allowPlainStorageFallback(true) to keep them in ordinary storage here, "
                + "or use another TokenStore");
    }

    private static OidcException failure(Throwable err, String operation) {
        if (isUnavailable(err)) {
            return unavailable(operation);
        }
        return new OidcException(OidcException.STORAGE_UNAVAILABLE,
                "Secure storage failed to " + operation + " the tokens: " + err.getMessage(), err);
    }
}
