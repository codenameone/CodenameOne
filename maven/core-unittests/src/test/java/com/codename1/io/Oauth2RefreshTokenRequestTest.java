package com.codename1.io;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class Oauth2RefreshTokenRequestTest extends UITestBase {

    @FormTest
    public void testRefreshTokenRequest() {
        // Just instantiate to cover the class definition
        Oauth2.RefreshTokenRequest req = new Oauth2("url", "id", "uri").new RefreshTokenRequest();
        Assertions.assertNotNull(req);
    }
}
