package com.codenameone.examples.hellocodenameone;

import android.util.Base64;

import java.nio.charset.StandardCharsets;

public class Base64NativeImpl {
    public String encodeUtf8(String plainText) {
        if (plainText == null) {
            return null;
        }
        byte[] data = plainText.getBytes(StandardCharsets.UTF_8);
        return Base64.encodeToString(data, Base64.NO_WRAP);
    }

    public String decodeToUtf8(String base64Text) {
        if (base64Text == null) {
            return null;
        }
        byte[] data = Base64.decode(base64Text, Base64.DEFAULT);
        return new String(data, StandardCharsets.UTF_8);
    }

    public boolean isSupported() {
        return true;
    }
}
