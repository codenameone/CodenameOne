package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.security.SecureStorage;

/**
 * Round-trips a secret through the platform {@link SecureStorage} non-prompting
 * store: set, read back, remove, confirm it is gone. On platforms that do not
 * provide non-prompting secure storage ({@code set} returns {@code false}, e.g.
 * the JavaScript port) the test self-skips. Validates the Windows DPAPI backend
 * (and the iOS keychain / Android EncryptedSharedPreferences) end-to-end.
 */
public class SecureStorageTest extends BaseTest {
    @Override
    public boolean runTest() {
        try {
            SecureStorage ss = SecureStorage.getInstance();
            String account = "cn1ss_roundtrip";
            String secret = "s3cr3t-value-éñ-42";

            // Clean any stale entry from a previous run.
            ss.remove(account);

            if (!ss.set(account, secret)) {
                // Non-prompting secure storage unsupported on this platform; skip.
                done();
                return true;
            }

            String read = ss.get(account);
            if (!secret.equals(read)) {
                fail("SecureStorage read-back mismatch: " + read);
                return false;
            }

            ss.remove(account);
            String afterRemove = ss.get(account);
            if (afterRemove != null) {
                fail("SecureStorage entry survived remove: " + afterRemove);
                return false;
            }

            done();
            return true;
        } catch (Throwable t) {
            fail("SecureStorageTest failed: " + t);
            return false;
        }
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
