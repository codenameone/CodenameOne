package com.codename1.social;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import static org.junit.jupiter.api.Assertions.*;

class LoginExtrasTest extends UITestBase {

    @FormTest
    void testLoginCallbacks() {
        Login login = new Login() {
            @Override
            public boolean isNativeLoginSupported() { return false; }

            @Override
            protected boolean validateToken(String token) { return true; }
        };

        login.setCallback(new LoginCallback() {
            public void loginSuccessful() {}
            public void loginFailed(String errorMessage) {}
        });

        login.setOauth2URL("http://url");
        login.setClientId("id");
        login.setRedirectURI("uri");
        login.setClientSecret("secret");

        // This will try to show auth UI, which might fail or do nothing in headless without browser component setup
        // But it exercises the code path.
        login.doLogin();
    }
}
