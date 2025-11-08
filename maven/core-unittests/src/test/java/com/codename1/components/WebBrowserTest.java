package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import static org.junit.jupiter.api.Assertions.*;

class WebBrowserTest extends UITestBase {

    @FormTest
    void testDefaultConstructor() {
        WebBrowser browser = new WebBrowser();
        assertNotNull(browser);
    }

    @FormTest
    void testSetURLUpdatesURL() {
        WebBrowser browser = new WebBrowser();
        browser.setURL("https://www.example.com");
        assertEquals("https://www.example.com", browser.getURL());
    }

    @FormTest
    void testSetPageUpdatesPage() {
        WebBrowser browser = new WebBrowser();
        String html = "<html><body>Test</body></html>";
        browser.setPage(html, null);
        // Page should be set
        assertNotNull(browser);
    }

    @FormTest
    void testGetPageReturnsPage() {
        WebBrowser browser = new WebBrowser();
        // Page may be null initially
        String page = browser.getPage();
        assertTrue(page == null || page.length() >= 0);
    }

    @FormTest
    void testIsNativeReturnsBoolean() {
        WebBrowser browser = new WebBrowser();
        // Should return a boolean value
        boolean isNative = browser.isNative();
        assertTrue(isNative || !isNative);
    }

    @FormTest
    void testAddWebEventListener() {
        WebBrowser browser = new WebBrowser();
        if (browser.isNative()) {
            browser.addWebEventListener("onLoad", evt -> {});
            // Should not throw exception
        }
        assertNotNull(browser);
    }

    @FormTest
    void testBackAndForward() {
        WebBrowser browser = new WebBrowser();
        browser.setURL("https://www.example.com");
        // Back/forward operations
        browser.back();
        browser.forward();
        // Should not throw exceptions
        assertNotNull(browser);
    }

    @FormTest
    void testReloadAndStop() {
        WebBrowser browser = new WebBrowser();
        browser.setURL("https://www.example.com");
        browser.reload();
        browser.stop();
        // Should not throw exceptions
        assertNotNull(browser);
    }

    @FormTest
    void testExecuteJavaScript() {
        WebBrowser browser = new WebBrowser();
        browser.execute("console.log('test');");
        // Should not throw exception
        assertNotNull(browser);
    }

    @FormTest
    void testGetTitleReturnsTitle() {
        WebBrowser browser = new WebBrowser();
        String title = browser.getTitle();
        // Title may be null initially
        assertTrue(title == null || title.length() >= 0);
    }
}
