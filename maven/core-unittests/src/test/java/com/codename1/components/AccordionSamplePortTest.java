package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.CN;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
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
        prepareForInteraction(form);

        Accordion accordion = harness.getAccordion();
        Component openCloseArrow = harness.getFirstHeaderLeadComponent();
        ensureSized(openCloseArrow, form);
        assertNotNull(openCloseArrow);
        tap(openCloseArrow);
        DisplayTest.flushEdt();
        assertNotNull(accordion.getCurrentlyExpanded());

        String portraitUiid = accordion.getOpenCloseIconUIID();
        assertEquals("PaddedOpenCloseIconPortrait", portraitUiid);
        Style portraitStyle = UIManager.getInstance().getComponentStyle(portraitUiid);
        assertEquals(harness.getLastPadding(), portraitStyle.getPadding(RIGHT));

        implementation.setPortrait(false);
        implementation.setDisplaySize(1920, 1080);
        form.getStyle().setMargin(0, 0, 0, 0); // Avoid layout adjustments interfering with size change
        form.getStyle().setPadding(0, 0, 0, 0);
        Display.getInstance().sizeChanged(1920, 1080);
        prepareForInteraction(form);
    }

    @FormTest
    void rtlToggleRebuildsFormWithTranslations() {
        implementation.setDisplaySize(1080, 1920);
        AccordionSampleHarness harness = new AccordionSampleHarness(false);
        Form form = harness.createForm();
        form.show();
        prepareForInteraction(form);

        assertFalse(UIManager.getInstance().getLookAndFeel().isRTL());
        assertEquals("Align", harness.getTranslationLabel().getText());

        ensureSized(harness.getRtlToggle(), form);
        tap(harness.getRtlToggle());
        flushSerialCalls();

        Form rtlForm = CN.getCurrentForm();
        AccordionSampleHarness rtlHarness = (AccordionSampleHarness) rtlForm.getClientProperty("accordionHarness");
        assertNotNull(rtlHarness);
        prepareForInteraction(rtlForm);

        assertEquals("יישר קו", rtlHarness.getTranslationLabel().getText());

        Accordion accordion = rtlHarness.getAccordion();
        Component lead = rtlHarness.getFirstHeaderLeadComponent();
        ensureSized(lead, rtlForm);
        tap(lead);
        assertNotNull(accordion.getCurrentlyExpanded());
    }

    private void tap(Component component) {
        int x = component.getAbsoluteX() + component.getWidth() / 2;
        int y = component.getAbsoluteY() + component.getHeight() / 2;
        implementation.dispatchPointerPressAndRelease(x, y);
        flushSerialCalls();
    }

    private void ensureSized(Component component, Form form) {
        for (int i = 0; i < 5 && (component.getWidth() <= 0 || component.getHeight() <= 0); i++) {
            form.revalidate();
            flushSerialCalls();
        }
    }

    private void prepareForInteraction(Form form) {
        form.revalidate();
        flushSerialCalls();
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
            String initialUIID = "PaddedOpenCloseIcon" + (CN.isPortrait() ? "Portrait" : "Landscape");
            accordion = new Accordion(FontImage.MATERIAL_KEYBOARD_ARROW_DOWN, FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT, initialUIID);
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

        Component getFirstHeaderLeadComponent() {
            String uiid = accordion.getOpenCloseIconUIID();
            Component found = findByUIID(accordion, uiid);
            if (found != null) {
                return found;
            }

            Container header = getFirstHeaderContainer();
            Component lead = header.getLeadComponent();
            return lead != null ? lead : header;
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
            String customUIID = "PaddedOpenCloseIcon" + (CN.isPortrait() ? "Portrait" : "Landscape");
            String oldCustomUIID = "PaddedOpenCloseIcon" + (CN.isPortrait() ? "Landscape" : "Portrait");

            Style openCloseIconStyle = UIManager.getInstance().getComponentStyle(customUIID);
            if (openCloseIconStyle == null) {
                openCloseIconStyle = new Style();
                UIManager.getInstance().setComponentStyle(customUIID, openCloseIconStyle);
            }
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

            UIManager.getInstance().setComponentStyle(customUIID, openCloseIconStyle);
            acc.setOpenCloseIconUIID(customUIID);

            $(oldCustomUIID, acc).setUIID(customUIID);
            $("AccordionArrow", acc).setUIID(customUIID);
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

        private Component findByUIID(Container root, String uiid) {
            for (int i = 0; i < root.getComponentCount(); i++) {
                Component child = root.getComponentAt(i);
                if (uiid.equals(child.getUIID())) {
                    return child;
                }
                if (child instanceof Container) {
                    Component nested = findByUIID((Container) child, uiid);
                    if (nested != null) {
                        return nested;
                    }
                }
            }
            return null;
        }
    }
}
