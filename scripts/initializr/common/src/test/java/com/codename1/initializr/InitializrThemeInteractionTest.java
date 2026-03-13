package com.codename1.initializr;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

public class InitializrThemeInteractionTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        new Initializr().runApp();
        Form current = Display.getInstance().getCurrent();
        assertNotNull(current, "Initializr should show a form");

        Button initialHello = getPreviewHelloButton();
        assertNotNull(initialHello, "Preview should include Hello World button");
        assertEqual("Button", initialHello.getUIID(), "Default preview should start in clean light mode");

        clickByLabel("DARK");
        Button darkHello = getPreviewHelloButton();
        assertNotNull(darkHello, "Preview button should still exist after switching to dark mode");
        assertEqual("InitializrLiveButtonDarkClean", darkHello.getUIID(), "Dark mode should update preview button UIID");

        clickByLabel("TEAL");
        Button darkTealHello = getPreviewHelloButton();
        assertNotNull(darkTealHello, "Preview button should still exist after switching accent");
        assertEqual("InitializrLiveButtonDarkTealRound", darkTealHello.getUIID(), "Teal accent should update preview button UIID");

        clickByLabel("LIGHT");
        Button lightTealHello = getPreviewHelloButton();
        assertNotNull(lightTealHello, "Preview button should still exist after switching back to light mode");
        assertEqual("InitializrLiveButtonLightTealRound", lightTealHello.getUIID(), "Light mode should update preview button UIID");

        clickByLabel("CLEAN");
        Button cleanBeforeCustom = getPreviewHelloButton();
        int baselineCleanBg = cleanBeforeCustom.getUnselectedStyle().getBgColor();
        assertNotEqual(0x010203, baselineCleanBg, "Baseline clean mode color should differ from custom CSS probe color");

        setText("appendCustomCssEditor",
                "Button { background-color: #010203; color: #ffffff; }\n"
                        + "InitializrLiveButtonDarkBlueRound { background-color: #112233; color: #ffffff; }");
        waitFor(100);

        Button cleanCustomHello = getPreviewHelloButton();
        assertNotNull(cleanCustomHello, "Preview button should exist after custom CSS in clean mode");
        assertEqual("Button", cleanCustomHello.getUIID(), "Clean accent should map to base Button UIID");
        assertNotEqual(baselineCleanBg, cleanCustomHello.getUnselectedStyle().getBgColor(),
                "Custom CSS should change preview button color from baseline");
        assertEqual(0x010203, cleanCustomHello.getUnselectedStyle().getBgColor(),
                "Custom CSS should apply to preview button style");

        clickByLabel("DARK");
        clickByLabel("BLUE");
        Button darkBlueHello = getPreviewHelloButton();
        assertNotNull(darkBlueHello, "Preview button should exist after applying custom CSS and toggling");
        assertEqual("InitializrLiveButtonDarkBlueRound", darkBlueHello.getUIID(),
                "Mode/accent toggles should still update preview with custom CSS");
        assertEqual(0x112233, darkBlueHello.getUnselectedStyle().getBgColor(),
                "Custom CSS should apply to non-clean preview UIID selectors");

        clickByLabel("LIGHT");
        clickByLabel("ORANGE");
        Button lightOrangeHello = getPreviewHelloButton();
        assertNotNull(lightOrangeHello, "Preview button should exist after switching back with custom CSS");
        assertEqual("InitializrLiveButtonLightOrangeRound", lightOrangeHello.getUIID(),
                "Accent toggles should keep working after custom CSS is set");

        setText("appendCustomCssEditor", "Button { color: pink; text-align: center; }");
        waitFor(100);
        clickByLabel("CLEAN");
        Button pinkCenteredHello = getPreviewHelloButton();
        assertEqual(0xffc0cb, pinkCenteredHello.getUnselectedStyle().getFgColor(),
                "Named color 'pink' should normalize and apply in preview");
        assertEqual(Component.CENTER, pinkCenteredHello.getUnselectedStyle().getAlignment(),
                "text-align: center should apply in preview");

        clickByLabel("DARK");
        clickByLabel("TEAL");
        Button pinkDarkTealHello = getPreviewHelloButton();
        assertEqual("InitializrLiveButtonDarkTealRound", pinkDarkTealHello.getUIID(),
                "Mode/accent toggles should still update UIID after pink custom CSS");
        assertEqual(0xffc0cb, pinkDarkTealHello.getUnselectedStyle().getFgColor(),
                "Button selector custom color should apply even in dark/teal mode");
        assertEqual(Component.CENTER, pinkDarkTealHello.getUnselectedStyle().getAlignment(),
                "Button selector custom alignment should apply even in dark/teal mode");

        clickByLabel("LIGHT");
        clickByLabel("CLEAN");
        Button pinkBackToCleanHello = getPreviewHelloButton();
        assertEqual(0xffc0cb, pinkBackToCleanHello.getUnselectedStyle().getFgColor(),
                "Button selector custom color should still apply after returning to clean mode");

        // Invalid intermediate CSS edits should not freeze theme/accent toggles in preview.
        setText("appendCustomCssEditor", "Button { color: pink;");
        waitFor(100);
        clickByLabel("DARK");
        clickByLabel("ORANGE");
        Button fallbackDarkOrangeHello = getPreviewHelloButton();
        assertEqual("InitializrLiveButtonDarkOrangeRound", fallbackDarkOrangeHello.getUIID(),
                "Theme toggles should continue updating while CSS is temporarily invalid");
        assertEqual(0xffc0cb, fallbackDarkOrangeHello.getUnselectedStyle().getFgColor(),
                "Preview should retain last valid custom CSS while invalid CSS is being edited");

        setText("appendCustomCssEditor", "Button { color: orange; }");
        waitFor(100);
        clickByLabel("LIGHT");
        clickByLabel("TEAL");
        Button recoveredLightTealHello = getPreviewHelloButton();
        assertEqual("InitializrLiveButtonLightTealRound", recoveredLightTealHello.getUIID(),
                "Theme toggles should still work after recovering from invalid CSS");
        assertEqual(0xffa500, recoveredLightTealHello.getUnselectedStyle().getFgColor(),
                "Recovered valid CSS should apply after invalid intermediate edit");
        return true;
    }

    private void clickByLabel(String text) {
        clickButtonByLabel(text);
        waitFor(50);
    }

    private Button getPreviewHelloButton() {
        Component component = findByName("previewHelloButton");
        assertNotNull(component, "Unable to find preview hello button");
        assertTrue(component instanceof Button, "previewHelloButton should be a Button");
        return (Button) component;
    }
}
