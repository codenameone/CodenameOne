package com.codename1.io;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression test for
 * https://github.com/codenameone/CodenameOne/issues/1502
 * -- FileSystemStorage.openInputStream(String) must throw IOException for a
 * non-existent file rather than silently opening an empty stream.
 */
class FileSystemStorageOpenInputStreamTest extends UITestBase {

    @Test
    void openInputStreamThrowsForMissingFile() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String missing = fs.getAppHomePath() + "this-file-must-not-exist-1502.bin";

        IOException thrown = assertThrows(IOException.class,
                () -> fs.openInputStream(missing),
                "FileSystemStorage.openInputStream must throw IOException when the "
                        + "file is missing; otherwise callers cannot distinguish a missing "
                        + "file from a legitimately empty file. See #1502.");
        assertNotNull(thrown.getMessage(),
                "IOException for missing file should include a useful message.");
        // FileInputStream on JavaSE and the iOS port both throw the more
        // specific FileNotFoundException; we don't insist on it for every
        // future port but flag it as the preferred subtype.
        assertTrue(thrown instanceof FileNotFoundException
                        || thrown.getMessage().toLowerCase().contains("not"),
                "expected FileNotFoundException (or a message that signals 'not found'), got "
                        + thrown);
    }
}
