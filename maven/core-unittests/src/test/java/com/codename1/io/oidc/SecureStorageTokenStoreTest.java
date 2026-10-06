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

import com.codename1.io.Storage;
import com.codename1.junit.UITestBase;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.SecureStorage;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.codename1.io.oidc.OidcTestSupport.await;
import static com.codename1.io.oidc.OidcTestSupport.tokens;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SecureStorageTokenStore}: the round trip through a store that works, and what happens
 * on a platform with none -- an error that says so, unless the caller asked for the fallback.
 */
public class SecureStorageTokenStoreTest extends UITestBase {

    private static final String KEY = "https://issuer.example.com|client";

    /** A working non-prompting store, with a gated half that can be told to fail. */
    private static final class FakeSecureStorage extends SecureStorage {
        final Map<String, String> quiet = new HashMap<String, String>();
        final Map<String, String> gated = new HashMap<String, String>();
        BiometricError gatedFailure;
        boolean refuseWrites;

        public boolean set(String account, String value) {
            if (refuseWrites) {
                return false;
            }
            quiet.put(account, value);
            return true;
        }

        public String get(String account) {
            return quiet.get(account);
        }

        public boolean remove(String account) {
            return quiet.remove(account) != null;
        }

        public int entryState(String account) {
            return quiet.containsKey(account) ? ENTRY_PRESENT : ENTRY_ABSENT;
        }

        public AsyncResource<String> get(String reason, String account) {
            AsyncResource<String> r = new AsyncResource<String>();
            if (gatedFailure != null) {
                r.error(new BiometricException(gatedFailure, "simulated"));
            } else {
                r.complete(gated.get(account));
            }
            return r;
        }

        public AsyncResource<Boolean> set(String reason, String account, String value) {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            if (gatedFailure != null) {
                r.error(new BiometricException(gatedFailure, "simulated"));
            } else {
                gated.put(account, value);
                r.complete(Boolean.TRUE);
            }
            return r;
        }

        public AsyncResource<Boolean> remove(String reason, String account) {
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.valueOf(gated.remove(account) != null));
            return r;
        }
    }

    /** What a port without secure storage hands out: the base class, which supports nothing. */
    private static final class NoSecureStorage extends SecureStorage {
    }

    @AfterEach
    void clearPlainStorage() {
        Storage.getInstance().deleteStorageFile("cn1.oidc." + KEY);
    }

    private static void assertUnavailable(Throwable error) {
        assertInstanceOf(OidcException.class, error);
        assertEquals(OidcException.STORAGE_UNAVAILABLE, ((OidcException) error).getError());
    }

    @Test
    void savedTokensComeBackWithEveryField() {
        FakeSecureStorage storage = new FakeSecureStorage();
        SecureStorageTokenStore store = new SecureStorageTokenStore(storage);

        assertEquals(Boolean.TRUE, await(store.save(KEY, tokens("AT", "RT"))).value);
        assertTrue(storage.quiet.containsKey("cn1.oidc." + KEY));
        assertTrue(storage.gated.isEmpty(), "quiet by default: nothing may prompt");

        OidcTokens back = await(store.load(KEY)).value;
        assertNotNull(back);
        assertEquals("AT", back.getAccessToken());
        assertEquals("RT", back.getRefreshToken());
        assertEquals("Bearer", back.getTokenType());
        assertNotNull(back.getExpiresAt());
        assertFalse(back.isExpired());
    }

    @Test
    void aRefreshTokenCarriedOverFromAnEarlierResponseIsStored() {
        // A refresh response that omits refresh_token means "keep the one you have". The set
        // then holds it in its field and not in its raw response.
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("access_token", "AT-2");
        OidcTokens carried = OidcTokens.fromTokenResponse(json, "RT-kept");
        FakeSecureStorage storage = new FakeSecureStorage();
        SecureStorageTokenStore store = new SecureStorageTokenStore(storage);

        await(store.save(KEY, carried));

        assertEquals("RT-kept", await(store.load(KEY)).value.getRefreshToken());
    }

    @Test
    void theDefaultStoreKeepsACarriedOverRefreshTokenToo() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("access_token", "AT-2");
        TokenStore plain = new TokenStore.DefaultStorageTokenStore();

        await(plain.save(KEY, OidcTokens.fromTokenResponse(json, "RT-kept")));

        assertEquals("RT-kept", await(plain.load(KEY)).value.getRefreshToken());
    }

    @Test
    void loadAnswersNullWhenNothingWasStored() {
        OidcTestSupport.Outcome<OidcTokens> r =
                await(new SecureStorageTokenStore(new FakeSecureStorage()).load(KEY));
        assertNull(r.error);
        assertNull(r.value);
    }

    @Test
    void clearRemovesTheEntryAndReportsWhetherThereWasOne() {
        FakeSecureStorage storage = new FakeSecureStorage();
        SecureStorageTokenStore store = new SecureStorageTokenStore(storage);
        await(store.save(KEY, tokens("AT", "RT")));

        assertEquals(Boolean.TRUE, await(store.clear(KEY)).value);
        assertTrue(storage.quiet.isEmpty());
        assertEquals(Boolean.FALSE, await(store.clear(KEY)).value);
    }

    @Test
    void aPlatformWithoutSecureStorageFailsEveryOperationAndSaysWhy() {
        SecureStorageTokenStore store = new SecureStorageTokenStore(new NoSecureStorage());

        OidcTestSupport.Outcome<Boolean> saved = await(store.save(KEY, tokens("AT", "RT")));
        assertNull(saved.value);
        assertUnavailable(saved.error);
        assertTrue(saved.error.getMessage().contains("no secure storage"), saved.error.getMessage());
        assertTrue(saved.error.getMessage().contains("allowPlainStorageFallback"));

        assertUnavailable(await(store.load(KEY)).error);
        assertUnavailable(await(store.clear(KEY)).error);
        assertFalse(Storage.getInstance().exists("cn1.oidc." + KEY),
                "nothing may reach plain storage without the opt-in");
    }

    @Test
    void theFallbackIsUsedOnlyWhenAskedFor() {
        SecureStorageTokenStore store = new SecureStorageTokenStore(new NoSecureStorage())
                .allowPlainStorageFallback(true);

        assertEquals(Boolean.TRUE, await(store.save(KEY, tokens("AT", "RT"))).value);
        assertTrue(Storage.getInstance().exists("cn1.oidc." + KEY));
        assertEquals("RT", await(store.load(KEY)).value.getRefreshToken());
        assertEquals(Boolean.TRUE, await(store.clear(KEY)).value);
        assertFalse(Storage.getInstance().exists("cn1.oidc." + KEY));
    }

    @Test
    void aWriteAWorkingStoreRefusesDoesNotFallBack() {
        FakeSecureStorage storage = new FakeSecureStorage();
        storage.refuseWrites = true;
        SecureStorageTokenStore store = new SecureStorageTokenStore(storage)
                .allowPlainStorageFallback(true);

        OidcTestSupport.Outcome<Boolean> saved = await(store.save(KEY, tokens("AT", "RT")));

        assertUnavailable(saved.error);
        assertTrue(saved.error.getMessage().contains("refused"), saved.error.getMessage());
        assertFalse(Storage.getInstance().exists("cn1.oidc." + KEY));
    }

    @Test
    void theBiometricOptionUsesTheGatedHalf() {
        FakeSecureStorage storage = new FakeSecureStorage();
        SecureStorageTokenStore store = new SecureStorageTokenStore(storage)
                .requireBiometrics("Unlock your session");

        await(store.save(KEY, tokens("AT", "RT")));
        assertTrue(storage.quiet.isEmpty());
        assertTrue(storage.gated.containsKey("cn1.oidc." + KEY));
        assertEquals("AT", await(store.load(KEY)).value.getAccessToken());
        assertEquals(Boolean.TRUE, await(store.clear(KEY)).value);
        assertTrue(storage.gated.isEmpty());
    }

    @Test
    void anEntryRevokedByNewBiometricsReadsAsSignedOut() {
        FakeSecureStorage storage = new FakeSecureStorage();
        SecureStorageTokenStore store = new SecureStorageTokenStore(storage)
                .requireBiometrics("Unlock your session");
        storage.gatedFailure = BiometricError.KEY_REVOKED;

        OidcTestSupport.Outcome<OidcTokens> r = await(store.load(KEY));

        assertNull(r.error);
        assertNull(r.value);
    }

    @Test
    void aCancelledPromptIsAnErrorAndNotASignOut() {
        FakeSecureStorage storage = new FakeSecureStorage();
        SecureStorageTokenStore store = new SecureStorageTokenStore(storage)
                .requireBiometrics("Unlock your session");
        storage.gatedFailure = BiometricError.USER_CANCELED;

        OidcTestSupport.Outcome<OidcTokens> r = await(store.load(KEY));

        assertUnavailable(r.error);
        assertInstanceOf(BiometricException.class, r.error.getCause());
    }

    @Test
    void gatedStorageMissingFromThePlatformFailsOrFallsBack() {
        SecureStorageTokenStore strict = new SecureStorageTokenStore(new NoSecureStorage())
                .requireBiometrics("Unlock");
        OidcTestSupport.Outcome<Boolean> refused = await(strict.save(KEY, tokens("AT", "RT")));
        assertUnavailable(refused.error);
        assertTrue(refused.error.getMessage().contains("no secure storage"));

        SecureStorageTokenStore lenient = new SecureStorageTokenStore(new NoSecureStorage())
                .requireBiometrics("Unlock").allowPlainStorageFallback(true);
        assertEquals(Boolean.TRUE, await(lenient.save(KEY, tokens("AT", "RT"))).value);
        assertEquals("AT", await(lenient.load(KEY)).value.getAccessToken());
    }
}
