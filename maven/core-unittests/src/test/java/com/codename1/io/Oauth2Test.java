package com.codename1.io;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.Display;
import com.codename1.testing.TestCodenameOneImplementation;
import org.junit.jupiter.api.Assertions;
import java.util.concurrent.atomic.AtomicBoolean;

public class Oauth2Test extends UITestBase {

    @FormTest
    public void testTokenRequest() throws Exception {
        String clientId = "client123";
        String redirectUri = "http://localhost/callback";
        String tokenUrl = "http://server/token";
        String clientSecret = "secret";

        Oauth2 oauth = new Oauth2("http://server/auth", clientId, redirectUri, "scope", tokenUrl, clientSecret);

        // Mock network response for token request
        // The token request is triggered when handleURL receives a code
        TestCodenameOneImplementation.getInstance().addNetworkMockResponse(tokenUrl, 200, "OK", "access_token=12345&expires=3600".getBytes("UTF-8"));

        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicBoolean error = new AtomicBoolean(false);

        ActionListener callback = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if (evt.getSource() instanceof AccessToken) {
                    AccessToken token = (AccessToken) evt.getSource();
                    if ("12345".equals(token.getToken())) {
                        success.set(true);
                    }
                } else {
                    error.set(true);
                }
            }
        };

        // Trigger handleURL via reflection or just create a subclass that exposes it?
        // Or trigger it via browser interaction?
        // Oauth2 uses WebBrowser.
        // We can inject a browser interaction.

        // But wait, createLoginComponent returns a WebBrowser.
        // We can simulate navigation on that browser.

        com.codename1.ui.Component webCmp = oauth.createAuthComponent(callback);
        Assertions.assertTrue(webCmp instanceof com.codename1.components.WebBrowser);
        com.codename1.components.WebBrowser browser = (com.codename1.components.WebBrowser) webCmp;

        // Trigger navigation to redirect URI with code
        try {
            java.lang.reflect.Method onLoad = com.codename1.components.WebBrowser.class.getDeclaredMethod("onLoad", String.class);
            onLoad.setAccessible(true);
            onLoad.invoke(browser, redirectUri + "?code=auth_code");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // This puts a Network request in queue.
        // We need to process it.
        // NetworkManager handles it. Test implementation might need to drive it?
        // UITestBase generally uses TestCodenameOneImplementation which doesn't automatically run NetworkManager thread fully for all cases,
        // but ConnectionRequest usually runs on Network thread.

        // Wait for result
        int loops = 0;
        while (!success.get() && !error.get() && loops < 20) {
            Thread.sleep(100);
            com.codename1.ui.DisplayTest.flushEdt();
            loops++;
        }

        Assertions.assertTrue(success.get(), "Oauth2 should succeed with token");

        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }
}
