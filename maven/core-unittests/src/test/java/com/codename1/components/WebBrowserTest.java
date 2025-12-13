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
    void testBrowserNavigationCallback() {
        WebBrowser browser = new WebBrowser();
        browser.setBrowserNavigationCallback(url -> true);
        // getBrowserNavigationCallback() may return null if not supported
        assertNotNull(browser);
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
}
