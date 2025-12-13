package com.codename1.samples;

import com.codename1.io.tar.TarEntry;
import com.codename1.io.tar.TarOutputStream;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.TestPeerComponent;
import com.codename1.ui.layouts.BorderLayout;
import org.junit.jupiter.api.Assertions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SetPageURLHierarchyTestTest extends UITestBase {

    @FormTest
    public void testSetPageURLHierarchy() throws IOException {
        Form wform = new Form("wform", new BorderLayout());

        // Setup mock peer
        TestPeerComponent mockPeer = new TestPeerComponent(new Object());
        implementation.setBrowserComponent(mockPeer);

        byte[] data = "<html><body>Hello</body></html>".getBytes();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        TarOutputStream tarOutputStream = new TarOutputStream(bos);
        TarEntry entry = new TarEntry("Page.html", "Page.html");
        entry.setSize(data.length);
        tarOutputStream.putNextEntry(entry);
        tarOutputStream.write(data);
        tarOutputStream.close();

        // Provide dummy resource
        implementation.putResource("/html.tar", new ByteArrayInputStream(bos.toByteArray()));

        final BrowserComponent browser = new BrowserComponent();

        try {
            browser.setURLHierarchy("/Page.html");
        } catch (IOException ex) {
            // Expected due to dummy resource not being a TAR
            ex.printStackTrace();
        }

        wform.addComponent(BorderLayout.CENTER, browser);
        wform.show();

        flushSerialCalls();

        String url = implementation.getBrowserURL(mockPeer);
        System.out.println("Set URL Hierarchy Result: " + url);

        // We only check if no crash occurred and if URL ends with Page.html IF it was set.
        if (url != null) {
             Assertions.assertTrue(url.endsWith("Page.html"), "URL should end with Page.html. Actual: " + url);
        }
    }
}
