package com.codename1.samples;

import com.codename1.components.WebBrowser;
import com.codename1.io.Util;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;

import static org.junit.jupiter.api.Assertions.*;

public class GoogleLoginSampleTest extends UITestBase {

    @FormTest
    public void testGoogleLoginFlow() {
        Display.getInstance().setProperty("oauth2.useBrowserWindow", "false");

        GoogleLoginSample sample = new GoogleLoginSample();
        sample.init(null);
        sample.start();

        Form current = Display.getInstance().getCurrent();
        assertNotNull(current);

        // Should show login button initially
        Button loginBtn = findButton(current, "Login");
        assertNotNull(loginBtn, "Login button should be visible initially");

        // Mock the user info response
        implementation.setConnectionResponseProvider(url -> {
             if (url.contains("googleapis.com/plus/v1/people/me")) {
                 return ("{\n" +
                         " \"id\": \"123456789\",\n" +
                         " \"displayName\": \"Test User\",\n" +
                         " \"image\": {\n" +
                         "  \"url\": \"http://example.com/image.jpg\"\n" +
                         " }\n" +
                         "}").getBytes();
             }
             return null;
        });

        // Click login
        loginBtn.released();
        flushSerialCalls();

        // Should show login dialog/form with WebBrowser
        Form loginForm = Display.getInstance().getCurrent();
        assertNotSame(current, loginForm, "Should show a new form/dialog for login");

        // If it's a Dialog (InfiniteProgress), dispose it to reveal the login form
        if (loginForm instanceof Dialog) {
            ((Dialog)loginForm).dispose();
            flushSerialCalls();
            loginForm = Display.getInstance().getCurrent();
        }

        WebBrowser browser = findWebBrowser(loginForm);
        assertNotNull(browser, "WebBrowser should be present in login form");

        String browserUrl = browser.getURL();
        String redirectUri = getRedirectUriFromUrl(browserUrl);
        if (redirectUri == null || redirectUri.length() == 0) {
            // fallback if redirect uri is empty
             redirectUri = "http://localhost";
        }

        String successUrl = redirectUri + "#access_token=mock_token&expires_in=3600";
        browser.onLoad(successUrl);
        flushSerialCalls();

        // Wait for login success
        for (int i = 0; i < 20; i++) {
            if (GoogleLoginSample.loginSuccess) break;
            try { Thread.sleep(100); } catch (Exception e) {}
            flushSerialCalls();
        }

        assertTrue(GoogleLoginSample.loginSuccess, "Login success flow should be completed");
    }

    private String getRedirectUriFromUrl(String url) {
        if (url == null) return "";
        int idx = url.indexOf("redirect_uri=");
        if (idx > 0) {
            String sub = url.substring(idx + 13);
            int end = sub.indexOf('&');
            if (end > 0) {
                return Util.decode(sub.substring(0, end), "UTF-8", false);
            }
            return Util.decode(sub, "UTF-8", false);
        }
        return "";
    }

    private WebBrowser findWebBrowser(Container container) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponentAt(i);
            if (c instanceof WebBrowser) {
                return (WebBrowser) c;
            }
            if (c instanceof Container) {
                WebBrowser found = findWebBrowser((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Button findButton(Container container, String text) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponentAt(i);
            if (c instanceof Button) {
                Button b = (Button) c;
                if (text.equals(b.getText())) {
                    return b;
                }
            }
            if (c instanceof Container) {
                Button found = findButton((Container) c, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
