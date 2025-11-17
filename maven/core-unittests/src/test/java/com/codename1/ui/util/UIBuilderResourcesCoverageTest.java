package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.Resources;
import com.codename1.ui.util.UIBuilder;
import com.codename1.ui.util.EmbeddedContainer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage for UIBuilder and Resources utilities without reflection.
 */
class UIBuilderResourcesCoverageTest extends UITestBase {

    @FormTest
    void uiBuilderNavigationAndRegistryLifecycle() throws Exception {
        UIBuilder builder = new UIBuilder();
        assertFalse(UIBuilder.isBlockAnalytics());
        UIBuilder.setBlockAnalytics(true);
        assertTrue(UIBuilder.isBlockAnalytics());

        builder.setBackCommandEnabled(true);
        assertTrue(builder.isBackCommandEnabled());
        builder.setBackCommandEnabled(false);
        assertFalse(builder.isBackCommandEnabled());

        UIBuilder.registerCustomComponent("MyLabel", CustomComponent.class);
        Component created = builder.createComponentType("MyLabel");
        assertTrue(created instanceof CustomComponent);

        builder.setBackCommandEnabled(true);
        builder.popNavigationStack();
        builder.setBackDestination("nowhere");
        assertTrue(builder.formNavigationStackDebug().contains("["));

        assertTrue(builder.allowBackTo("any"));
    }

    @FormTest
    void uiBuilderCommandListenersAndState() {
        UIBuilder builder = new UIBuilder();
        final StringBuilder buffer = new StringBuilder();
        builder.addCommandListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                buffer.append("global");
            }
        });
        builder.addCommandListener("test", new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                buffer.append("local");
            }
        });
        com.codename1.ui.Form f = new com.codename1.ui.Form("test");
        f.setName("test");
        f.show();
        Command cmd = builder.createCommand("Hello", null, 0, null);
        com.codename1.ui.events.ActionEvent event = new com.codename1.ui.events.ActionEvent(cmd, com.codename1.ui.events.ActionEvent.Type.Command, f, 0, 0);
        UIBuilder.FormListener listener = builder.new FormListener();
        listener.actionPerformed(event);
        listener.actionPerformed(new com.codename1.ui.events.ActionEvent(new Command("Other"), com.codename1.ui.events.ActionEvent.Type.Command, f, 0, 0));
        builder.removeCommandListener("test", null);
        builder.removeCommandListener(null);
        assertTrue(buffer.indexOf("global") >= 0);
        assertTrue(buffer.indexOf("local") >= 0);
    }

    @FormTest
    void resourcesToggleFlagsAndSystemResourceCreation() throws Exception {
        Resources.setFailOnMissingTruetype(false);
        Resources.setEnableMediaQueries(true);
        Resources.setRuntimeMultiImageEnabled(true);
        Resources.setClassLoader(UIBuilderResourcesCoverageTest.class);
        assertFalse(Resources.isFailOnMissingTruetype());
        assertTrue(Resources.isEnableMediaQueries());

        byte[] data = createMinimalResource();
        Resources res = new Resources(new ByteArrayInputStream(data), -1);
        assertEquals(Resources.MAGIC_DATA, res.getResourceType("data"));
        assertTrue(res.isData("data"));
        assertFalse(res.isImage("data"));
        assertArrayEquals(new String[]{"data"}, res.getDataResourceNames());
    }

    private static byte[] createMinimalResource() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeShort(2);
        dos.writeByte(Resources.MAGIC_HEADER);
        dos.writeUTF("h");
        dos.writeShort(0);
        dos.writeShort(1);
        dos.writeShort(0);
        dos.writeShort(0);

        dos.writeByte(Resources.MAGIC_DATA);
        dos.writeUTF("data");
        dos.writeInt(1);
        dos.writeByte(5);
        dos.flush();
        return baos.toByteArray();
    }

    public static class CustomComponent extends Component {
    }
}
