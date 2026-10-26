/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.security;

import com.codename1.io.Preferences;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.SecureRandom;
import com.codename1.security.SecureStorage;
import com.codename1.security.vault.KdfProfile;
import com.codename1.security.vault.UnlockPolicy;
import com.codename1.security.vault.Vault;
import com.codename1.security.vault.VaultError;
import com.codename1.security.vault.VaultException;
import com.codename1.security.vault.VaultOptions;
import com.codename1.util.Base64;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

/// The app key: 32 random bytes that seal everything Bastion keeps on the
/// device -- the payment-signing key ([DeviceKey]) and the session's refresh
/// token ([SealedStorage]). It is in memory only while the app is unlocked;
/// locking drops it, and nothing sealed under it can be read until one of
/// its copies is opened again:
///
/// - **Biometric copy.** In [SecureStorage]'s prompting tier,
///   `get(reason, account)`: on iOS a Keychain item with
///   `kSecAccessControlTouchIDCurrentSet`, on Android a Keystore key with
///   user authentication required. The operating system releases it only
///   after a fingerprint or face check, and enrolling another biometric
///   revokes it. The app never decides "the check passed"; the key either
///   comes back or it does not.
/// - **PIN copy.** A [Vault] enrolled with the PIN, through the vault's
///   [KdfProfile]. The PIN is joined with a random pepper kept in the quiet,
///   OS-protected tier, so a copy of the app's files is not enough to try
///   PINs offline: that also needs the Keychain or Keystore. On the device,
///   [#PIN_ATTEMPTS] wrong PINs in a row wipe every copy.
/// - **Plain copy.** Only while neither of the above is set up: the key in
///   the quiet tier, as protected as the OS store is, opened without asking.
///   The lock then has nothing to ask for and signs the session out instead.
///
/// Turning a protection on removes the plain copy, so the weaker way in does
/// not sit beside the stronger one.
///
/// The attempt counter lives in the OS secure store beside the pepper
/// ([#pinFailures]), so a copy or rollback of the app's files does not reset
/// it. It is still a PIN checked on the device: someone who controls the OS
/// store controls the counter too, which is why the pepper is there as well.
public final class Keys {
    /// The shortest PIN the app accepts.
    public static final int MIN_PIN = 6;
    /// The longest.
    public static final int MAX_PIN = 12;
    /// Wrong PINs in a row before every copy of the key is wiped.
    public static final int PIN_ATTEMPTS = 5;

    private static final String PLAIN_ACCOUNT = "bastion.keys.plain";
    private static final String BIO_ACCOUNT = "bastion.keys.biometric";
    private static final String PEPPER_ACCOUNT = "bastion.keys.pepper";
    private static final String MADE = "bastion.keys.made";
    private static final String BIO_ON = "bastion.keys.biometricOn";
    private static final String PIN_ON = "bastion.keys.pinOn";
    private static final String FAILURES = "bastion.keys.pinFailures";
    private static final String OWNER = "bastion.keys.owner";
    /// Set while a PIN vault is known to be on the device but its removal has
    /// not been confirmed: the next wipe tries again.
    private static final String VAULT_STALE = "bastion.keys.vaultStale";
    private static final String VAULT = "bastion-pin";
    private static final String SECRET = "app-key";

    /// What a PIN attempt came to.
    public interface PinResult {
        /// The key is open.
        void unlocked();

        /// Wrong PIN; `left` more tries before the wipe.
        void wrong(int left);

        /// Too many wrong PINs: every copy is gone, sign in again.
        void wiped();

        /// The vault could not be read (not the PIN's fault).
        void failed(String why);
    }

    private static byte[] key;
    private static SecureStorage storage;

    private Keys() {
    }

    /// Keeps the copies somewhere else (the tests keep them in memory).
    public static void useStorage(SecureStorage store) {
        storage = store;
    }

    static SecureStorage storage() {
        if (storage != null) {
            return storage;
        }
        SecureStorage platform = SecureStorage.getInstance();
        // The strict binding is opt-in (installed apps keep their keys): on
        // Android it binds the key on every API level and revokes it when a
        // biometric is enrolled. Set before the first write creates the key:
        // a key made before it (an update of an app that ran without it)
        // stays unbound, and isStrictBiometricBinding() still answers true.
        platform.setStrictBiometricBinding(true);
        return platform;
    }

    /// The key while unlocked, else null. Callers must not keep it.
    public static byte[] current() {
        return key;
    }

    /// Whether a key was ever made on this device (and not wiped since).
    public static boolean exists() {
        return Preferences.get(MADE, false);
    }

    public static boolean isOpen() {
        return key != null;
    }

    public static boolean biometricEnabled() {
        return Preferences.get(BIO_ON, false);
    }

    public static boolean pinEnabled() {
        return Preferences.get(PIN_ON, false);
    }

    /// Whether opening the key needs the user: a biometric or the PIN.
    public static boolean protectedByUser() {
        return biometricEnabled() || pinEnabled();
    }

    /// Wrong PINs since the last good one.
    ///
    /// Kept in the OS secure store beside the pepper, not in Preferences: a
    /// plain file is one a thief with the phone's storage (a backup, a rooted
    /// shell) could reset between guesses, and the count is what turns five
    /// wrong PINs into a wipe. Removing the secure store's entry resets it
    /// too, but that store is also where the pepper lives, so the same
    /// access already loses the vault.
    public static int pinFailures() {
        String stored = storage().get(FAILURES);
        if (stored == null) {
            return 0;
        }
        try {
            return Integer.parseInt(stored);
        } catch (NumberFormatException damaged) {
            // Unreadable counts as the last try, never as none.
            return PIN_ATTEMPTS - 1;
        }
    }

    private static void setPinFailures(int failures) {
        storage().set(FAILURES, Integer.toString(failures));
    }

    private static void clearPinFailures() {
        storage().remove(FAILURES);
        // The copy an earlier version kept in Preferences.
        Preferences.delete(FAILURES);
    }

    /// Drops the key from memory: the app lock calls this.
    public static void lock() {
        zero(key);
        key = null;
    }

    /// After a fresh sign-in. Opens the plain copy if there is one; makes a
    /// key if there is none. A key that exists but needs a biometric or PIN
    /// the user did not give is given up: the sign-in proves who the user is
    /// but cannot open it, so the device starts over (its signing key is
    /// registered again when a payment needs it).
    public static void signedIn() {
        if (key != null) {
            return;
        }
        if (Lock.isLocked()) {
            // Never while locked: a refresh that failed behind the lock is
            // not the user choosing to start over. The unlock decides.
            return;
        }
        if (exists() && !protectedByUser() && openPlain()) {
            return;
        }
        startOver();
    }

    /// After a sign-in, once the server has said who `username` is. A key
    /// made for another account -- or a device key registered for one -- is
    /// given up: the next user must not have their session sealed under the
    /// previous user's key, nor sign with the previous user's device id.
    /// Answers true when the key was replaced, and whatever was sealed under
    /// the old one (the tokens just stored) has to be sealed again.
    public static boolean claim(String username) {
        if (username == null) {
            return false;
        }
        String owner = Preferences.get(OWNER, "");
        String deviceOwner = DeviceKey.owner();
        boolean foreign = (owner.length() > 0 && !owner.equals(username))
                || (deviceOwner != null && !deviceOwner.equals(username));
        if (foreign) {
            startOver();
        }
        Preferences.set(OWNER, username);
        return foreign;
    }

    /// Opens the plain copy without asking: only when the user set up
    /// neither a biometric nor a PIN.
    public static boolean openPlain() {
        if (protectedByUser()) {
            return false;
        }
        byte[] stored = decode(storage().get(PLAIN_ACCOUNT));
        if (stored == null) {
            return false;
        }
        key = stored;
        return true;
    }

    /// Wipes every copy and makes a new key, held as a plain copy until the
    /// user protects it. Whatever was sealed under the old key is unreadable
    /// from here: the device key is forgotten with it.
    public static void startOver() {
        wipe();
        key = SecureRandom.bytes(32);
        storage().set(PLAIN_ACCOUNT, encode(key));
        Preferences.set(MADE, true);
    }

    /// Wipes every copy and the key in memory. Signing out calls this too:
    /// the next account to sign in on this device starts from nothing.
    ///
    /// Every copy is removed whether or not the preferences say it was set
    /// up: a flag that was lost or cleared (a revoked biometric, an
    /// interrupted setup) must not leave a copy behind. The PIN vault's
    /// removal is asynchronous; the answer completes when it is done, with
    /// false when it failed -- the vault is then marked and removed again by
    /// the next wipe or PIN setup, and is useless meanwhile: its pepper is
    /// already gone.
    public static AsyncResource<Boolean> wipe() {
        lock();
        SecureStorage store = storage();
        store.remove(PLAIN_ACCOUNT);
        store.remove(PEPPER_ACCOUNT);
        store.remove(null, BIO_ACCOUNT);
        Preferences.delete(MADE);
        Preferences.delete(BIO_ON);
        Preferences.delete(PIN_ON);
        clearPinFailures();
        Preferences.delete(OWNER);
        DeviceKey.forget();
        final AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        final Vault vault = vault();
        if (vault == null || (!vault.isEnrolled() && !Preferences.get(VAULT_STALE, false))) {
            Preferences.delete(VAULT_STALE);
            out.complete(Boolean.TRUE);
            return out;
        }
        Preferences.set(VAULT_STALE, true);
        vault.destroyLocalData().ready(gone -> {
            Preferences.delete(VAULT_STALE);
            out.complete(Boolean.TRUE);
        }).except(err -> out.complete(Boolean.FALSE));
        return out;
    }

    private static Vault testVault;
    private static boolean useTestVault;

    /// Keeps the PIN vault somewhere else (the tests); null for none.
    static void useVault(Vault vault) {
        testVault = vault;
        useTestVault = true;
    }

    private static Vault vault() {
        return useTestVault ? testVault : Vault.named(VAULT);
    }

    // ---------------------------------------------------------- biometric

    /// Stores the biometric copy (Android asks for the biometric to write
    /// it). `failed` gets a reason to show when the platform has no
    /// biometric-bound store or the user cancelled.
    public static void enableBiometric(String reason, final Runnable done,
            final SuccessCallback<String> failed) {
        if (key == null) {
            failed.onSucess("Unlock first.");
            return;
        }
        storage().set(reason, BIO_ACCOUNT, encode(key)).ready(ok -> {
            if (ok != null && ok.booleanValue()) {
                Preferences.set(BIO_ON, true);
                storage().remove(PLAIN_ACCOUNT);
                done.run();
            } else {
                failed.onSucess("This device could not keep a biometric-bound key.");
            }
        }).except(err -> failed.onSucess(unavailable(err)
                ? "This device has no biometric-bound key store."
                : "Biometric setup was cancelled."));
    }

    /// Removes the biometric copy; with no PIN either, the plain copy comes
    /// back so the key is not lost.
    public static void disableBiometric() {
        if (!biometricEnabled()) {
            return;
        }
        storage().remove(null, BIO_ACCOUNT);
        Preferences.delete(BIO_ON);
        restorePlainIfUnprotected();
    }

    /// Opens the key with a biometric: the OS prompts, and only a match
    /// releases the copy. `failed` gets true when the copy is gone for good
    /// (a new fingerprint was enrolled) and the PIN or a sign-in is next.
    public static void unlockWithBiometric(String reason, final Runnable done,
            final SuccessCallback<Boolean> failed) {
        if (!biometricEnabled()) {
            failed.onSucess(Boolean.TRUE);
            return;
        }
        storage().get(reason, BIO_ACCOUNT).ready(stored -> {
            byte[] opened = decode(stored);
            if (opened == null) {
                failed.onSucess(Boolean.FALSE);
                return;
            }
            key = opened;
            done.run();
        }).except(err -> {
            boolean gone = err instanceof BiometricException
                    && ((BiometricException) err).getError() == BiometricError.KEY_REVOKED;
            if (gone) {
                // Revoked by the OS: a biometric was enrolled since. The copy
                // is dead; say so instead of offering it again, and remove
                // the entry itself -- biometricEnabled() is false from here,
                // so nothing else would.
                Preferences.delete(BIO_ON);
                storage().remove(null, BIO_ACCOUNT);
                restorePlainIfUnprotected();
            }
            failed.onSucess(Boolean.valueOf(gone));
        });
    }

    // ---------------------------------------------------------------- PIN

    /// Whether `pin` is acceptable: [#MIN_PIN] to [#MAX_PIN] digits, not one
    /// digit repeated and not a run up or down (123456, 987654).
    public static boolean validPin(String pin) {
        if (pin == null || pin.length() < MIN_PIN || pin.length() > MAX_PIN) {
            return false;
        }
        boolean same = true;
        boolean up = true;
        boolean down = true;
        for (int iter = 0; iter < pin.length(); iter++) {
            char c = pin.charAt(iter);
            if (c < '0' || c > '9') {
                return false;
            }
            if (iter > 0) {
                int step = c - pin.charAt(iter - 1);
                same &= step == 0;
                up &= step == 1;
                down &= step == -1;
            }
        }
        return !same && !up && !down;
    }

    /// Sets (or replaces) the PIN copy. The key must be open.
    public static void setPin(final String pin, final Runnable done,
            final SuccessCallback<String> failed) {
        if (key == null) {
            failed.onSucess("Unlock first.");
            return;
        }
        if (!validPin(pin)) {
            failed.onSucess("Choose 6 to 12 digits that are not a simple run.");
            return;
        }
        final Vault vault = vault();
        final char[] secret = encode(key).toCharArray();
        Runnable enroll = () -> {
            String pepper = encode(SecureRandom.bytes(32));
            if (!storage().set(PEPPER_ACCOUNT, pepper)) {
                failed.onSucess("This device has no secure storage for the PIN.");
                return;
            }
            vault.enroll(pinSecret(pin, pepper), new VaultOptions()
                    .kdf(KdfProfile.current())
                    .policy(UnlockPolicy.SESSION_ONLY))
                    .ready(enrolled -> vault.putSecret(SECRET, secret).ready(stored -> {
                        vault.lock();
                        Preferences.set(PIN_ON, true);
                        clearPinFailures();
                        storage().remove(PLAIN_ACCOUNT);
                        done.run();
                    }).except(err -> failed.onSucess("The PIN could not be saved.")))
                    .except(err -> failed.onSucess("The PIN could not be saved."));
        };
        if (vault.isEnrolled()) {
            vault.destroyLocalData().ready(gone -> enroll.run())
                    .except(err -> failed.onSucess("The old PIN could not be removed."));
        } else {
            enroll.run();
        }
    }

    /// Removes the PIN copy; with no biometric either, the plain copy comes
    /// back. `failed` gets the reason, untranslated, when the vault could not
    /// be destroyed: the PIN then still unlocks, and saying otherwise would
    /// leave the customer believing it is gone.
    public static void removePin(final Runnable done, final SuccessCallback<String> failed) {
        if (!pinEnabled()) {
            done.run();
            return;
        }
        vault().destroyLocalData().ready(gone -> {
            storage().remove(PEPPER_ACCOUNT);
            Preferences.delete(PIN_ON);
            clearPinFailures();
            restorePlainIfUnprotected();
            done.run();
        }).except(err -> failed.onSucess("The PIN could not be removed."));
    }

    /// Opens the key with the PIN. A wrong one counts; [#PIN_ATTEMPTS] in a
    /// row wipe every copy of the key.
    public static void unlockWithPin(String pin, final PinResult result) {
        if (!pinEnabled()) {
            result.failed("No PIN is set on this device.");
            return;
        }
        final String pepper = storage().get(PEPPER_ACCOUNT);
        if (pepper == null) {
            // Without the pepper no PIN can be right: the OS store lost it.
            wipe().ready(done -> result.wiped());
            return;
        }
        final Vault vault = vault();
        vault.unlockWithPassword(pinSecret(pin, pepper)).ready(ok -> vault.getSecret(SECRET)
                .ready(chars -> {
                    vault.lock();
                    byte[] opened = chars == null ? null : decode(new String(chars));
                    if (opened == null) {
                        result.failed("The PIN vault is damaged.");
                        return;
                    }
                    clearPinFailures();
                    key = opened;
                    result.unlocked();
                }).except(err -> {
                    vault.lock();
                    result.failed("The PIN vault could not be read.");
                })).except(err -> {
                    if (err instanceof VaultException
                            && ((VaultException) err).getError() == VaultError.AUTHENTICATION_FAILED) {
                        int failures = pinFailures() + 1;
                        if (failures >= PIN_ATTEMPTS) {
                            wipe().ready(done -> result.wiped());
                        } else {
                            setPinFailures(failures);
                            result.wrong(PIN_ATTEMPTS - failures);
                        }
                    } else {
                        result.failed("The PIN vault could not be read.");
                    }
                });
    }

    // ------------------------------------------------------------ helpers

    /// What the vault's KDF is given: the PIN and the device's pepper.
    static char[] pinSecret(String pin, String pepper) {
        return (pin + ":" + pepper).toCharArray();
    }

    private static void restorePlainIfUnprotected() {
        if (!protectedByUser() && key != null) {
            storage().set(PLAIN_ACCOUNT, encode(key));
        }
    }

    private static boolean unavailable(Throwable err) {
        return err instanceof BiometricException
                && ((BiometricException) err).getError() == BiometricError.NOT_AVAILABLE;
    }

    private static String encode(byte[] bytes) {
        return Base64.encodeUrlSafe(bytes);
    }

    private static byte[] decode(String stored) {
        if (stored == null) {
            return null;
        }
        byte[] bytes = Base64.decodeUrlSafe(stored);
        return bytes != null && bytes.length == 32 ? bytes : null;
    }

    private static void zero(byte[] bytes) {
        if (bytes != null) {
            for (int iter = 0; iter < bytes.length; iter++) {
                bytes[iter] = 0;
            }
        }
    }
}
