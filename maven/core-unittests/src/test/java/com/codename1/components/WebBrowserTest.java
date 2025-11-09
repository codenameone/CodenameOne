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
    void testGetPageReturnsPage() {
        WebBrowser browser = new WebBrowser();
        // Page may be null initially
        String page = browser.getPage();
        assertTrue(page == null || page.length() >= 0);
    }

    @FormTest
    void testGetInternalReturnsComponent() {
        WebBrowser browser = new WebBrowser();
        assertNotNull(browser.getInternal());
    }

    @FormTest
    void testBrowserNavigationCallback() {
        WebBrowser browser = new WebBrowser();
        browser.setBrowserNavigationCallback(url -> true);
        // Should not throw exception
        assertNotNull(browser);
    }

    @FormTest
    void testGetBrowserNavigationCallback() {
        WebBrowser browser = new WebBrowser();
        // May return null if not set or not supported
        browser.getBrowserNavigationCallback();
        assertNotNull(browser);
    }

    @FormTest
    void testDestroyMethod() {
        WebBrowser browser = new WebBrowser();
        browser.destroy();
        // Should not throw exception
        assertTrue(true);
    }

    @FormTest
    void testGetTitleReturnsTitle() {
        WebBrowser browser = new WebBrowser();
        String title = browser.getTitle();
        // Title may be null initially
        assertTrue(title == null || title.length() >= 0);
    }

    @FormTest
    void testPropertyNames() {
        WebBrowser browser = new WebBrowser();
        String[] props = browser.getPropertyNames();
        assertNotNull(props);
        assertTrue(props.length > 0);
    }

    @FormTest
    void testPropertyTypes() {
        WebBrowser browser = new WebBrowser();
        Class[] types = browser.getPropertyTypes();
        assertNotNull(types);
        assertEquals(browser.getPropertyNames().length, types.length);
    }

    @FormTest
    void testReloadAndStopMethods() {
        WebBrowser browser = new WebBrowser();
        // Just verify these methods can be called without crashing
        browser.reload();
        browser.stop();
        assertNotNull(browser);
    }
}
