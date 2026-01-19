package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.Storage;
import com.codename1.ui.Display;
import com.codename1.io.Properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

public class StorageAndPropertiesRegressionTest extends BaseTest {
    private static final String STORE_KEY_ROLL_CALL_USER = "roll-call-user";

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        try {
            verifyPropertiesLoad();
            verifyStorageReadWrite();
        } catch (Exception exception) {
            fail("Storage/properties regression test failed: " + exception);
            return true;
        }
        done();
        return true;
    }

    private void verifyPropertiesLoad() throws IOException {
        Properties props = new Properties();
        try (InputStream stream = Display.getInstance().getResourceAsStream(
                StorageAndPropertiesRegressionTest.class, "/app-resources.properties")) {
            if (stream == null) {
                throw new IOException("app-resources.properties not found on classpath.");
            }
            props.load(stream);
        }
        String version = props.getProperty("app.version");
        if (version == null || version.length() == 0) {
            throw new IOException("app.version missing from app-resources.properties.");
        }
    }

    private void verifyStorageReadWrite() throws IOException {
        Storage storage = Storage.getInstance();
        Object store = storage.readObject(STORE_KEY_ROLL_CALL_USER);
        if (store == null) {
            Hashtable<String, String> newStore = new Hashtable<>();
            newStore.put("status", "created");
            storage.writeObject(STORE_KEY_ROLL_CALL_USER, newStore);
            storage.flushStorageCache();
            store = storage.readObject(STORE_KEY_ROLL_CALL_USER);
        }
        if (!(store instanceof Hashtable)) {
            throw new IOException("Storage read returned unexpected type: " + store);
        }
    }
}
