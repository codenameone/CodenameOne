package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.CN;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Regression test for
 * https://github.com/codenameone/CodenameOne/issues/1502
 *
 * The 2015 reporter showed that on iOS, calling
 * {@code FileSystemStorage.openInputStream(path)} on a missing file silently
 * returned an empty {@code NSFileInputStream} (because Apple's
 * {@code [NSFileHandle fileHandleForReadingAtPath:]} returns nil for a
 * non-existent path), so callers could not distinguish a missing file from a
 * legitimately empty one.
 *
 * Runs through {@link Cn1ssDeviceRunner} on every native target, so it
 * actually verifies the iOS, Android, JavaScript and JavaSE openInputStream
 * paths, not just JavaSE's {@link java.io.FileInputStream}.
 */
public class FileSystemStorageOpenInputStreamMissingTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String missing = fs.getAppHomePath() + "this-file-must-not-exist-1502-"
                    + System.currentTimeMillis() + ".bin";

            // Be defensive in case a previous run left a file with the same
            // name (System.currentTimeMillis() collision is improbable but
            // guarding makes the test independent of run order).
            if (fs.exists(missing)) {
                fs.delete(missing);
            }

            InputStream is = null;
            try {
                is = fs.openInputStream(missing);
                // If we got here, the contract is broken regardless of what
                // the stream returns - missing files must throw, not return
                // a silent empty stream.
                fail("openInputStream returned a stream (" + is + ") for a "
                        + "missing path " + missing + " instead of throwing. "
                        + "Platform=" + CN.getPlatformName());
                return false;
            } catch (FileNotFoundException expected) {
                // Preferred outcome: matches what JavaSE's FileInputStream
                // throws and lets callers distinguish 'missing' from other
                // I/O errors.
            } catch (IOException ioOnly) {
                // Acceptable fallback for any port that has not yet narrowed
                // its thrown type. We still want to know about it (so the
                // failing case is easy to see in logs) but we don't fail the
                // suite, because the contract the 2015 reporter cared about
                // - 'throws something for a missing file' - is satisfied.
                com.codename1.io.Log.p("openInputStream threw IOException "
                        + "rather than FileNotFoundException on platform "
                        + CN.getPlatformName() + ": " + ioOnly.getMessage());
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException ignore) { }
                }
            }

            done();
            return true;
        } catch (Throwable t) {
            fail("Unexpected error: " + t);
            return false;
        }
    }
}
