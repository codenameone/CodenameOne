package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.CN;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import static com.codename1.ui.Component.RIGHT;
import static com.codename1.ui.ComponentSelector.$;
import static org.junit.jupiter.api.Assertions.*;

class AccordionSamplePortTest extends UITestBase {

    @FormTest
    void openCloseIconsReactToOrientationChanges() {
        implementation.setDisplaySize(1080, 1920);
        implementation.setPortrait(true);

        AccordionSampleHarness harness = new AccordionSampleHarness(false);
        Form form = harness.createForm();
        form.show();

        Accordion accordion = harness.getAccordion();
        Container headerContainer = harness.getFirstHeaderContainer();
        tap(headerContainer);
        assertNotNull(accordion.getCurrentlyExpanded());

        String portraitUiid = accordion.getOpenCloseIconUIID();
        assertEquals("PaddedOpenCloseIconPortrait", portraitUiid);
        Style portraitStyle = UIManager.getInstance().getComponentStyle(portraitUiid);
        assertEquals(harness.getLastPadding(), portraitStyle.getPadding(RIGHT));

        implementation.setPortrait(false);
        implementation.setDisplaySize(1920, 1080);
        form.getStyle().setMargin(0, 0, 0, 0); // Avoid layout adjustments interfering with size change
        form.getStyle().setPadding(0, 0, 0, 0);
        form.revalidate();
        Display.getInstance().sizeChanged(1920, 1080);

        String landscapeUiid = accordion.getOpenCloseIconUIID();
        assertEquals("PaddedOpenCloseIconLandscape", landscapeUiid);
        Style landscapeStyle = UIManager.getInstance().getComponentStyle(landscapeUiid);
        assertEquals(harness.getLastPadding(), landscapeStyle.getPadding(RIGHT));

        Component lead = headerContainer.getLeadComponent();
        assertNotNull(lead);
        assertEquals(landscapeUiid, lead.getUIID());
    }

    @FormTest
    void rtlToggleRebuildsFormWithTranslations() {
        AccordionSampleHarness harness = new AccordionSampleHarness(false);
        Form form = harness.createForm();
        form.show();

        assertFalse(UIManager.getInstance().getLookAndFeel().isRTL());
        assertEquals("Align", harness.getTranslationLabel().getText());

        tap(harness.getRtlToggle());

        Form rtlForm = CN.getCurrentForm();
        AccordionSampleHarness rtlHarness = (AccordionSampleHarness) rtlForm.getClientProperty("accordionHarness");
        assertNotNull(rtlHarness);

        assertTrue(UIManager.getInstance().getLookAndFeel().isRTL());
        assertTrue(rtlHarness.getRtlToggle().isSelected());
        assertEquals("יישר קו", rtlHarness.getTranslationLabel().getText());

        Accordion accordion = rtlHarness.getAccordion();
        Container headerContainer = rtlHarness.getFirstHeaderContainer();
        tap(headerContainer);
        assertNotNull(accordion.getCurrentlyExpanded());
    }

    private void tap(Component component) {
        int x = component.getAbsoluteX() + component.getWidth() / 2;
        int y = component.getAbsoluteY() + component.getHeight() / 2;
        TestCodenameOneImplementation.getInstance().dispatchPointerPressAndRelease(x, y);
    }

    private static class AccordionSampleHarness {
        private final boolean hebrew;
        private Accordion accordion;
        private CheckBox rtlToggle;
        private Label translationLabel;
        private int lastPadding;

        AccordionSampleHarness(boolean hebrew) {
            this.hebrew = hebrew;
        }

        Form createForm() {
            Form form = new Form("Hi world", BoxLayout.y());
            form.putClientProperty("accordionHarness", this);
            accordion = new Accordion();
            accordion.setScrollableY(false);

            setupOpenCloseArrowStyles(accordion);
            form.addOrientationListener(e -> setupOpenCloseArrowStyles(accordion));
            form.addSizeChangedListener(e -> setupOpenCloseArrowStyles(accordion));

            accordion.addContent("Section 1", new Label("Section 1 content"));
            accordion.addContent("Section 2", new Label("Section 2 content"));
            accordion.addContent("Section 3 (Custom)", new Label("Section 3 content"));
            form.add(accordion);

            rtlToggle = new CheckBox("RTL");
            rtlToggle.setSelected(hebrew);
            rtlToggle.addActionListener(e -> {
                UIManager.getInstance().getLookAndFeel().setRTL(rtlToggle.isSelected());
                AccordionSampleHarness refreshed = new AccordionSampleHarness(rtlToggle.isSelected());
                Form rebuilt = refreshed.createForm();
                rebuilt.show();
            });
            form.add(rtlToggle);

            CheckBox sameSide = new CheckBox("Same Side");
            sameSide.setOppositeSide(false);
            form.add(sameSide);

            translationLabel = new Label(tr("Align"));
            form.add(translationLabel);

            return form;
        }

        Accordion getAccordion() {
            return accordion;
        }

        Container getFirstHeaderContainer() {
            Accordion.AccordionContent content = (Accordion.AccordionContent) accordion.getComponentAt(0);
            return (Container) content.getComponentAt(0);
        }

        CheckBox getRtlToggle() {
            return rtlToggle;
        }

        Label getTranslationLabel() {
            return translationLabel;
        }

        int getLastPadding() {
            return lastPadding;
        }

        private void setupOpenCloseArrowStyles(Accordion acc) {
            Style openCloseIconStyle = UIManager.getInstance().getComponentStyle(acc.getOpenCloseIconUIID());
            openCloseIconStyle.setMarginUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);

            String[] labels = new String[]{"Section 1", "Section 2", "Section 3 (Custom)"};
            int maxLabelWidth = 0;
            for (String label : labels) {
                Style style = UIManager.getInstance().getComponentStyle(acc.getHeaderUIID());
                if (style == null || style.getFont() == null) {
                    continue;
                }
                maxLabelWidth = Math.max(maxLabelWidth, style.getFont().stringWidth(label));
            }

            int padding = CN.getDisplayWidth() - maxLabelWidth - CN.convertToPixels(25);
            lastPadding = padding;
            openCloseIconStyle.setPadding(RIGHT, padding);
            openCloseIconStyle.setFgColor(0x0);

            String customUIID = "PaddedOpenCloseIcon" + (CN.isPortrait() ? "Portrait" : "Landscape");
            String oldCustomUIID = "PaddedOpenCloseIcon" + (CN.isPortrait() ? "Landscape" : "Portrait");

            UIManager.getInstance().setComponentStyle(customUIID, openCloseIconStyle);
            acc.setOpenCloseIconUIID(customUIID);

            $(oldCustomUIID, acc).setUIID(customUIID);
            if (acc.getComponentForm() != null) {
                acc.revalidateWithAnimationSafety();
            }
        }

        private String tr(String value) {
            if (!hebrew) {
                return value;
            }
            if ("Align".equals(value)) {
                return "יישר קו";
            }
            if ("Left".equals(value)) {
                return "שמאלה";
            }
            if ("Right".equals(value)) {
                return "ימין";
            }
            if ("Center".equals(value)) {
                return "מרכז";
            }
            if ("Top".equals(value)) {
                return "חלק עליון";
            }
            if ("Bottom".equals(value)) {
                return "בתחתית";
            }
            return value;
        }
    }
}
