package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;

import static org.junit.jupiter.api.Assertions.*;

public class BasicBrowserComponentSampleTest extends UITestBase {

    @FormTest
    void browserComponentUsesCefPropertyAndLoadsUrl() {
        implementation.setBuiltinSoundsEnabled(false);
        implementation.setBrowserComponent(createBrowserPeer());
        implementation.getBrowserExecuted().clear();

        Display.getInstance().setProperty("BrowserComponent.useCEF", "true");

        Form form = new Form("Hi World", new BorderLayout());
        BrowserComponent browser = new BrowserComponent();
        browser.setURL("https://www.codenameone.com");
        form.add(BorderLayout.CENTER, browser);

        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();
        form.getAnimationManager().flush();

        assertEquals("true", implementation.getProperty("BrowserComponent.useCEF", "false"));
        assertEquals("https://www.codenameone.com", browser.getURL());
        assertEquals(1, browser.getComponentCount());
        assertSame(browser.getComponentAt(0), browser.getInternal());
    }

    @FormTest
    void buttonsExecuteScriptsAndToolbarOpensSheet() {
        implementation.setBuiltinSoundsEnabled(false);
        implementation.setBrowserComponent(createBrowserPeer());
        implementation.getBrowserExecuted().clear();

        Form form = new Form("Hi World", new BorderLayout());
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);

        BrowserComponent browser = new BrowserComponent();
        browser.setURL("https://www.codenameone.com");
        form.add(BorderLayout.CENTER, browser);

        Button hello = new Button("Hello");
        hello.addActionListener(evt -> browser.execute("if (confirm('continue?')) alert('testing');"));

        Button prompt = new Button("Prompt");
        prompt.addActionListener(evt -> browser.execute("var name = prompt('What is your name'); if (name) alert('Hello ' + name);"));

        form.add(BorderLayout.SOUTH, GridLayout.encloseIn(2, hello, prompt));

        Command popupCommand = toolbar.addMaterialCommandToRightBar("Popup", FontImage.MATERIAL_OPEN_IN_NEW, evt -> {
            Sheet sheet = new Sheet(null, "A Popop");
            sheet.add(BorderLayout.CENTER, new Label("Hello World"));
            sheet.show();
        });

        form.show();
        form.getAnimationManager().flush();
        flushSerialCalls();
        form.getAnimationManager().flush();

        implementation.tapComponent(hello);
        flushSerialCalls();
        form.getAnimationManager().flush();

        implementation.tapComponent(prompt);
        flushSerialCalls();
        form.getAnimationManager().flush();

        assertEquals(2, implementation.getBrowserExecuted().size());
        assertTrue(implementation.getBrowserExecuted().get(0).contains("confirm('continue?')"));
        assertTrue(implementation.getBrowserExecuted().get(1).contains("prompt('What is your name')"));

        Button popupButton = toolbar.findCommandComponent(popupCommand);
        assertNotNull(popupButton, "Popup command should create a button in the right bar");

        implementation.tapComponent(popupButton);
        flushSerialCalls();
        form.getAnimationManager().flush();

        Form current = Display.getInstance().getCurrent();
        assertTrue(current instanceof Sheet, "Sheet should be displayed after tapping popup command");
        assertEquals("A Popop", current.getTitle());
        assertTrue(((Sheet) current).getContentPane().getComponentAt(0) instanceof Label);
    }

    private PeerComponent createBrowserPeer() {
        PeerComponent peer = new PeerComponent(new Object());
        Style style = peer.getUnselectedStyle();
        style.setMargin(0, 0, 0, 0);
        return peer;
    }
}
