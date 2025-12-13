package com.codename1.social;

import com.codename1.io.AccessToken;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.CN;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class LoginTest extends UITestBase {

    static class TestLogin extends Login {
        boolean validateTokenResult = true;
        boolean nativeLoginSupported = false;

        @Override
        public boolean isNativeLoginSupported() {
            return nativeLoginSupported;
        }

        @Override
        public void doLogin() {
            // Simulate login process started
        }

        @Override
        protected boolean validateToken(String token) {
            return validateTokenResult;
        }
    }

    @FormTest
    public void testLoginCallbackProxy() throws Exception {
        TestLogin login = new TestLogin();
        // Setup fields to avoid validation errors
        login.setClientId("id");
        login.setClientSecret("secret");
        login.setOauth2URL("url");
        login.setRedirectURI("uri");
        login.setAccessToken(new AccessToken("token", null));

        AtomicBoolean successCalled = new AtomicBoolean(false);
        AtomicBoolean failedCalled = new AtomicBoolean(false);

        LoginCallback cb = new LoginCallback() {
            @Override
            public void loginSuccessful() {
                successCalled.set(true);
            }

            @Override
            public void loginFailed(String errorMessage) {
                failedCalled.set(true);
            }
        };

        // Register callback via doLogin
        login.doLogin(cb);

        // Access package-private callback proxy
        final LoginCallback proxy = login.callback;
        assertNotNull(proxy);

        // Trigger success from background thread (to force callSerially)
        Thread t = new Thread(() -> {
            proxy.loginSuccessful();
        });
        t.start();
        t.join();

        flushSerialCalls();
        assertTrue(successCalled.get());

        // Test failure
        login.doLogin(cb);
        Thread t2 = new Thread(() -> {
            proxy.loginFailed("Error");
        });
        t2.start();
        t2.join();

        flushSerialCalls();
        assertTrue(failedCalled.get());
    }

    @FormTest
    public void testConnect() {
        TestLogin login = new TestLogin();
        login.setClientId("id");
        login.setClientSecret("secret");
        login.setOauth2URL("url");
        login.setRedirectURI("uri");
        login.setAccessToken(new AccessToken("token", null));

        // Mock doLogin to immediately succeed
        login = new TestLogin() {
            @Override
            public void doLogin() {
                callback.loginSuccessful();
            }
        };
        // Need to set fields again on new instance
        login.setClientId("id");
        login.setClientSecret("secret");
        login.setOauth2URL("url");
        login.setRedirectURI("uri");
        login.setAccessToken(new AccessToken("token", null));

        AsyncResource<Login> res = login.connect();
        assertNotNull(res.get());
        assertEquals(login, res.get());
    }

    @FormTest
    public void testValidateTokenWaitsForLogin() throws Exception {
        TestLogin login = new TestLogin() {
             @Override
            public void doLogin() {
                // Simulate async login
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {}
                    // Complete login
                    if (!CN.isEdt()) {
                        CN.callSerially(() -> callback.loginSuccessful());
                    } else {
                        callback.loginSuccessful();
                    }
                }).start();
            }
        };
        login.validateTokenResult = false;
        login.setClientId("id");
        login.setClientSecret("secret");
        login.setOauth2URL("url");
        login.setRedirectURI("uri");
        login.setAccessToken(new AccessToken("token", null));

        // validateToken should block until doLogin completes
        login.validateToken();
        // If it returns, it means it waited and succeeded.
    }
}
