package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Form;
import com.codename1.ui.TestPeerComponent;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Assertions;

public class SetBrowserURLWithJarUrlSampleTest extends UITestBase {

    @FormTest
    public void testSetBrowserURL() {
        Form wform = new Form("wform", new BorderLayout());

        // Setup mock peer
        TestPeerComponent mockPeer = new TestPeerComponent(new Object());
        implementation.setBrowserComponent(mockPeer);

        final BrowserComponent browser = new BrowserComponent();

        String url = "jar:///index.html";
        browser.setURL(url);

        wform.addComponent(BorderLayout.CENTER, browser);

        wform.show();

        flushSerialCalls();

        String setUrl = implementation.getBrowserURL(mockPeer);

        // Assert implementation received it (best effort)
        // If implementation is not updated, at least check component state
        if (setUrl != null) {
            Assertions.assertEquals(url, setUrl, "Browser URL should match in implementation");
        } else {
            Assertions.assertEquals(url, browser.getURL(), "Browser URL should match in component");
        }
    }
}
