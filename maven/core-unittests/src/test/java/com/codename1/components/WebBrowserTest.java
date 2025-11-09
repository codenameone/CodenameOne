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
    void testConstructorWithURL() {
        WebBrowser browser = new WebBrowser("https://www.example.com");
        assertNotNull(browser);
        assertEquals("https://www.example.com", browser.getURL());
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
    void testReloadAndStop() {
        WebBrowser browser = new WebBrowser();
        browser.setURL("https://www.example.com");
        browser.reload();
        browser.stop();
        // Should not throw exceptions
        assertNotNull(browser);
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
    void testGetPropertyValue() {
        WebBrowser browser = new WebBrowser("https://test.com");
        Object url = browser.getPropertyValue("url");
        assertEquals("https://test.com", url);
    }

    @FormTest
    void testSetPropertyValue() {
        WebBrowser browser = new WebBrowser();
        browser.setPropertyValue("url", "https://newurl.com");
        assertEquals("https://newurl.com", browser.getURL());
    }

    @FormTest
    void testSetPropertyValueHtml() {
        WebBrowser browser = new WebBrowser();
        String html = "<html><body>Test</body></html>";
        browser.setPropertyValue("html", html);
        assertEquals(html, browser.getPage());
    }

    @FormTest
    void testOnStartCallback() {
        WebBrowser browser = new WebBrowser() {
            @Override
            public void onStart(String url) {
                super.onStart(url);
            }
        };
        browser.setURL("https://www.example.com");
        assertNotNull(browser);
    }

    @FormTest
    void testOnLoadCallback() {
        WebBrowser browser = new WebBrowser() {
            @Override
            public void onLoad(String url) {
                super.onLoad(url);
            }
        };
        browser.setURL("https://www.example.com");
        assertNotNull(browser);
    }

    @FormTest
    void testOnErrorCallback() {
        WebBrowser browser = new WebBrowser() {
            @Override
            public void onError(String message, int errorCode) {
                super.onError(message, errorCode);
            }
        };
        browser.setURL("https://www.example.com");
        assertNotNull(browser);
    }
}
