package com.codename1.initializr.ui;

import com.codename1.initializr.model.ProjectOptions;
import com.codename1.initializr.model.Template;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.Button;
import com.codename1.ui.Form;

public class TemplatePreviewPanelThemeTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        validateModeAndAccentUiidUpdates();
        validateThemeTogglesStillApplyWithCustomCss();
        validateRepeatedCustomCssEditsDoNotBreakToggles();
        validateRecoveryAfterInvalidIntermediateCss();
        return true;
    }

    private void validateModeAndAccentUiidUpdates() {
        TemplatePreviewPanel panel = new TemplatePreviewPanel(Template.BAREBONES);

        panel.setOptions(options(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.TEAL, true, null));
        Button firstButton = panel.getLastLiveHelloButtonForTesting();
        Form firstForm = panel.getLastLiveFormForTesting();
        assertNotNull(firstButton, "Preview should include a hello button");
        assertNotNull(firstForm, "Preview should include a form");
        assertEqual("InitializrLiveButtonLightTealRound", firstButton.getUIID(), "Light/Teal/Rounded should map to expected UIID");
        assertEqual("Container", firstForm.getContentPane().getUIID(), "Light mode should use container content UIID");

        panel.setOptions(options(ProjectOptions.ThemeMode.DARK, ProjectOptions.Accent.ORANGE, false, null));
        Button secondButton = panel.getLastLiveHelloButtonForTesting();
        Form secondForm = panel.getLastLiveFormForTesting();
        assertEqual("InitializrLiveButtonDarkOrangeSquare", secondButton.getUIID(), "Dark/Orange/Square should map to expected UIID");
        assertEqual("InitializrLiveContentDark", secondForm.getContentPane().getUIID(), "Dark mode should use dark content UIID");
        assertNotEqual(firstButton.getUIID(), secondButton.getUIID(), "Toggling options should change button UIID");
    }

    private void validateThemeTogglesStillApplyWithCustomCss() {
        TemplatePreviewPanel panel = new TemplatePreviewPanel(Template.BAREBONES);
        String customCss = "Button { border-radius: 0; }";

        panel.setOptions(options(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.BLUE, false, customCss));
        Button blueButton = panel.getLastLiveHelloButtonForTesting();
        int blueBg = blueButton.getUnselectedStyle().getBgColor();
        assertEqual("InitializrLiveButtonLightBlueSquare", blueButton.getUIID(), "Custom CSS should not replace computed UIID");

        panel.setOptions(options(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.ORANGE, false, customCss));
        Button orangeButton = panel.getLastLiveHelloButtonForTesting();
        int orangeBg = orangeButton.getUnselectedStyle().getBgColor();
        assertEqual("InitializrLiveButtonLightOrangeSquare", orangeButton.getUIID(), "Accent toggle should continue to update UIID with custom CSS");
        assertNotEqual(blueBg, orangeBg, "Accent toggles should still change applied button colors with custom CSS");
    }

    private ProjectOptions options(ProjectOptions.ThemeMode mode, ProjectOptions.Accent accent, boolean rounded, String customCss) {
        return new ProjectOptions(
                mode,
                accent,
                rounded,
                true,
                ProjectOptions.PreviewLanguage.ENGLISH,
                ProjectOptions.JavaVersion.JAVA_8,
                customCss
        );
    }

    private void validateRepeatedCustomCssEditsDoNotBreakToggles() {
        TemplatePreviewPanel panel = new TemplatePreviewPanel(Template.BAREBONES);

        panel.setOptions(options(ProjectOptions.ThemeMode.DARK, ProjectOptions.Accent.DEFAULT, true,
                "Button { color: pink; }"));
        Button pinkButton = panel.getLastLiveHelloButtonForTesting();
        assertEqual(0xffc0cb, pinkButton.getUnselectedStyle().getFgColor(),
                "First custom CSS edit should apply pink color");

        panel.setOptions(options(ProjectOptions.ThemeMode.DARK, ProjectOptions.Accent.DEFAULT, true,
                "Button { color: orange; }"));
        Button orangeButton = panel.getLastLiveHelloButtonForTesting();
        assertEqual(0xffa500, orangeButton.getUnselectedStyle().getFgColor(),
                "Second custom CSS edit should replace previous color");
        assertEqual("InitializrLiveButtonDarkClean", orangeButton.getUIID(),
                "Dark clean UIID should remain stable after repeated custom CSS edits");

        panel.setOptions(options(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.TEAL, true,
                "Button { color: orange; }"));
        Button lightTealButton = panel.getLastLiveHelloButtonForTesting();
        assertEqual("InitializrLiveButtonLightTealRound", lightTealButton.getUIID(),
                "Theme toggles should keep updating UIID after repeated CSS edits");
        assertEqual(0xffa500, lightTealButton.getUnselectedStyle().getFgColor(),
                "Updated custom CSS should persist across theme toggles");

        panel.setOptions(options(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.TEAL, true, ""));
        Button clearedCssButton = panel.getLastLiveHelloButtonForTesting();
        assertNotEqual(0xffa500, clearedCssButton.getUnselectedStyle().getFgColor(),
                "Clearing custom CSS should remove previous custom fgColor");
    }

    private void validateRecoveryAfterInvalidIntermediateCss() {
        TemplatePreviewPanel panel = new TemplatePreviewPanel(Template.BAREBONES);

        panel.setOptions(options(ProjectOptions.ThemeMode.DARK, ProjectOptions.Accent.DEFAULT, true,
                "Button { color: pink; }"));
        Button pinkButton = panel.getLastLiveHelloButtonForTesting();
        assertEqual(0xffc0cb, pinkButton.getUnselectedStyle().getFgColor(),
                "Known-good CSS should apply before invalid intermediate edit");

        try {
            panel.setOptions(options(ProjectOptions.ThemeMode.DARK, ProjectOptions.Accent.DEFAULT, true,
                    "Button { color: pink;"));
            fail("Invalid custom CSS should fail fast");
        } catch (IllegalArgumentException expected) {
            // expected
        }

        panel.setOptions(options(ProjectOptions.ThemeMode.LIGHT, ProjectOptions.Accent.ORANGE, true,
                "Button { color: orange; }"));
        Button recoveredButton = panel.getLastLiveHelloButtonForTesting();
        assertEqual("InitializrLiveButtonLightOrangeRound", recoveredButton.getUIID(),
                "Preview should recover after invalid intermediate CSS edit");
        assertEqual(0xffa500, recoveredButton.getUnselectedStyle().getFgColor(),
                "Recovered valid CSS should apply after invalid edit");
    }
}
