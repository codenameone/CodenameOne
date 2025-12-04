package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.DisplayTest;
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
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        assertNotNull(browser.getInternal(), "Browser peer should be initialized");

        assertEquals("true", Display.getInstance().getProperty("BrowserComponent.useCEF", "false"));
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
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        implementation.tapComponent(hello);
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        implementation.tapComponent(prompt);
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        assertEquals(2, implementation.getBrowserExecuted().size());
        assertTrue(implementation.getBrowserExecuted().get(0).contains("confirm('continue?')"));
        assertTrue(implementation.getBrowserExecuted().get(1).contains("prompt('What is your name')"));

        Button popupButton = toolbar.findCommandComponent(popupCommand);
        assertNotNull(popupButton, "Popup command should create a button in the right bar");

        implementation.tapComponent(popupButton);
        DisplayTest.flushEdt();
        flushSerialCalls();
        DisplayTest.flushEdt();

        Sheet currentSheet = Sheet.getCurrentSheet();
        assertNotNull(currentSheet, "Sheet should be displayed after tapping popup command");
        assertTrue(currentSheet.getContentPane().getComponentCount() > 0, "Sheet should contain components");
        assertNotNull(findLabelWithText(currentSheet, "A Popop"), "Sheet title label should show 'A Popop'");
        assertNotNull(findLabelWithText(currentSheet, "Hello World"), "Sheet body should show 'Hello World'");
    }

    private PeerComponent createBrowserPeer() {
        PeerComponent peer = new PeerComponent(new Object());
        Style style = peer.getUnselectedStyle();
        style.setMargin(0, 0, 0, 0);
        return peer;
    }

    private Label findLabelWithText(Container container, String text) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component cmp = container.getComponentAt(i);
            if (cmp instanceof Label) {
                Label lbl = (Label) cmp;
                if (text.equals(lbl.getText())) {
                    return lbl;
                }
            }
            if (cmp instanceof Container) {
                Label nested = findLabelWithText((Container) cmp, text);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }
}
