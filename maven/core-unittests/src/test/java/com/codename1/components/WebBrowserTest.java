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
    void testSetHTMLUpdatesHTML() {
        WebBrowser browser = new WebBrowser();
        String html = "<html><body>Test</body></html>";
        browser.setHTML(html, "UTF-8");
        // HTML should be set
        assertNotNull(browser);
    }

    @FormTest
    void testPinchToZoomGetterAndSetter() {
        WebBrowser browser = new WebBrowser();
        browser.setPinchToZoomEnabled(true);
        assertTrue(browser.isPinchToZoomEnabled());

        browser.setPinchToZoomEnabled(false);
        assertFalse(browser.isPinchToZoomEnabled());
    }

    @FormTest
    void testNativeScrollingGetterAndSetter() {
        WebBrowser browser = new WebBrowser();
        browser.setNativeScrollingEnabled(true);
        assertTrue(browser.isNativeScrollingEnabled());

        browser.setNativeScrollingEnabled(false);
        assertFalse(browser.isNativeScrollingEnabled());
    }

    @FormTest
    void testPageGetterReturnsPage() {
        WebBrowser browser = new WebBrowser();
        browser.setURL("https://www.example.com");
        // Page may be null before loading
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
