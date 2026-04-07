package com.codenameone.examples.hellocodenameone;

import com.codename1.system.NativeInterface;

public interface Base64Native extends NativeInterface {
    String encodeUtf8(String plainText);
    String decodeToUtf8(String base64Text);
}
