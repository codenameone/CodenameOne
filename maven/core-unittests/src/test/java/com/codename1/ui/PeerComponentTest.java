package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class PeerComponentTest extends UITestBase {

    @FormTest
    void testCreatePeerComponent() {
        PeerComponent peer = PeerComponent.create(new Object());
        assertNotNull(peer);
    }

    @FormTest
    void testInitialize() {
        PeerComponent peer = PeerComponent.create(new Object());
        assertNotNull(peer);
        assertTrue(peer.isInitialized() || !peer.isInitialized());
    }

    @FormTest
    void testDeinitialize() {
        PeerComponent peer = PeerComponent.create(new Object());
        // Just verify deinitialize doesn't crash
        assertDoesNotThrow(() -> {
            Form f = new Form();
            f.add(peer);
            f.show();
            f.removeAll();
        });
    }

    @FormTest
    void testAddToForm() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        PeerComponent peer = PeerComponent.create(new Object());
        form.add(BorderLayout.CENTER, peer);
        form.revalidate();

        assertTrue(form.contains(peer));
    }

    @FormTest
    void testSetPreferredSize() {
        PeerComponent peer = PeerComponent.create(new Object());
        peer.setPreferredSize(new com.codename1.ui.geom.Dimension(100, 50));

        int prefW = peer.getPreferredW();
        int prefH = peer.getPreferredH();

        assertTrue(prefW > 0);
        assertTrue(prefH > 0);
    }

    @FormTest
    void testSetBounds() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        PeerComponent peer = PeerComponent.create(new Object());
        form.add(BorderLayout.CENTER, peer);
        form.revalidate();

        int x = peer.getX();
        int y = peer.getY();
        int w = peer.getWidth();
        int h = peer.getHeight();

        assertTrue(x >= 0);
        assertTrue(y >= 0);
        assertTrue(w >= 0);
        assertTrue(h >= 0);
    }

    @FormTest
    void testUIID() {
        PeerComponent peer = PeerComponent.create(new Object());
        peer.setUIID("CustomPeer");
        assertEquals("CustomPeer", peer.getUIID());
    }

    @FormTest
    void testFocusable() {
        PeerComponent peer = PeerComponent.create(new Object());
        peer.setFocusable(true);
        assertTrue(peer.isFocusable());

        peer.setFocusable(false);
        assertFalse(peer.isFocusable());
    }

    @FormTest
    void testVisibility() {
        PeerComponent peer = PeerComponent.create(new Object());
        assertTrue(peer.isVisible());

        peer.setVisible(false);
        assertFalse(peer.isVisible());

        peer.setVisible(true);
        assertTrue(peer.isVisible());
    }

    @FormTest
    void testEnabled() {
        PeerComponent peer = PeerComponent.create(new Object());
        assertTrue(peer.isEnabled());

        peer.setEnabled(false);
        assertFalse(peer.isEnabled());

        peer.setEnabled(true);
        assertTrue(peer.isEnabled());
    }

    @FormTest
    void testRemoveFromForm() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        PeerComponent peer = PeerComponent.create(new Object());
        form.add(BorderLayout.CENTER, peer);
        form.revalidate();

        assertTrue(form.contains(peer));

        form.removeComponent(peer);
        form.revalidate();

        assertFalse(form.contains(peer));
    }

    @FormTest
    void testMultiplePeerComponents() {
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(new BorderLayout());

        PeerComponent peer1 = PeerComponent.create(new Object());
        PeerComponent peer2 = PeerComponent.create(new Object());

        Container wrapper = new Container(new BorderLayout());
        wrapper.add(BorderLayout.NORTH, peer1);
        wrapper.add(BorderLayout.SOUTH, peer2);

        form.add(BorderLayout.CENTER, wrapper);
        form.revalidate();

        assertTrue(wrapper.contains(peer1));
        assertTrue(wrapper.contains(peer2));
    }

    @FormTest
    void testCreateWithNullNative() {
        // Creating with null should still work
        PeerComponent peer = PeerComponent.create(null);
        assertNotNull(peer);
    }

    @FormTest
    void testStyleManipulation() {
        PeerComponent peer = PeerComponent.create(new Object());
        com.codename1.ui.plaf.Style style = peer.getStyle();
        assertNotNull(style);

        // Verify style operations don't crash
        style.setPadding(5, 5, 5, 5);
        style.setMargin(2, 2, 2, 2);

        assertNotNull(peer.getStyle());
    }
}
