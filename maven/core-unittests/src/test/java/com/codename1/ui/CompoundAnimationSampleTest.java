package com.codename1.ui;

import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.*;

class CompoundAnimationSampleTest extends UITestBase {

    @FormTest
    void compoundAnimationUpdatesToolbarAndTitleOnScroll() {
        implementation.setDisplaySize(1080, 1920);

        Form form = buildSampleForm();
        form.show();
        DisplayTest.flushEdt();

        Toolbar toolbar = form.getToolbar();
        Component titleComponent = toolbar.getTitleComponent();

        assertEquals(0x112233, toolbar.getUnselectedStyle().getBgColor());
        assertEquals(0x111111, titleComponent.getUnselectedStyle().getBgColor());

        TestCodenameOneImplementation impl = implementation;
        scrollContent(impl, form.getContentPane(), 0);
        assertEquals(0x112233, toolbar.getUnselectedStyle().getBgColor());
        assertEquals(0x111111, titleComponent.getUnselectedStyle().getBgColor());

        scrollContent(impl, form.getContentPane(), 200);

        assertEquals(0x223344, toolbar.getUnselectedStyle().getBgColor());
        assertEquals(20, toolbar.getUnselectedStyle().getPaddingTop());
        assertEquals(0x334455, titleComponent.getUnselectedStyle().getBgColor());
        assertEquals(15, titleComponent.getUnselectedStyle().getPaddingBottom());
    }

    private Form buildSampleForm() {
        Form hi = new Form("Toolbar", new BoxLayout(BoxLayout.Y_AXIS));
        hi.getContentPane().setScrollableY(true);

        Style containerStyle = new Style();
        containerStyle.setBgColor(0x223344);
        containerStyle.setBgTransparency(255);
        containerStyle.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        containerStyle.setPadding(20, 10, 4, 4);
        UIManager.getInstance().setComponentStyle("Container", containerStyle);

        Style titleStyle = new Style();
        titleStyle.setBgColor(0x334455);
        titleStyle.setBgTransparency(255);
        titleStyle.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        titleStyle.setPadding(5, 15, 2, 2);
        UIManager.getInstance().setComponentStyle("Title", titleStyle);

        Toolbar.setGlobalToolbar(true);
        Toolbar toolbar = hi.getToolbar();
        toolbar.getUnselectedStyle().setBgTransparency(255);
        toolbar.getUnselectedStyle().setBgColor(0x112233);
        toolbar.getUnselectedStyle().setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        toolbar.getUnselectedStyle().setPadding(0, 0, 0, 0);

        Component titleComponent = toolbar.getTitleComponent();
        titleComponent.getUnselectedStyle().setBgColor(0x111111);
        titleComponent.getUnselectedStyle().setBgTransparency(255);
        titleComponent.getUnselectedStyle().setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
        titleComponent.getUnselectedStyle().setPadding(0, 0, 0, 0);

        int imageWidth = Math.max(1, implementation.getDisplayWidth());
        Image background = Image.createImage(imageWidth, Math.max(1, imageWidth / 5), 0xffff0000);

        Style stitle = toolbar.getTitleComponent().getUnselectedStyle();
        stitle.setBgImage(background);
        stitle.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
        stitle.setPaddingUnit(Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS);
        stitle.setPaddingTop(15);

        SpanButton credit = new SpanButton("Demo credit link");
        credit.addActionListener(e -> Display.getInstance().execute("http://example.com"));
        hi.addComponent(new SpanLabel("Compound animation sample content"));
        for (int i = 0; i < 25; i++) {
            hi.addComponent(new Label("Line " + i));
        }
        hi.addComponent(credit);

        ComponentAnimation titleAnimation = ComponentAnimation.compoundAnimation(
                toolbar.createStyleAnimation("Container", 200),
                toolbar.getTitleComponent().createStyleAnimation("Title", 200)
        );
        hi.getAnimationManager().onTitleScrollAnimation(titleAnimation);

        return hi;
    }

    private void scrollContent(TestCodenameOneImplementation impl, Container content, int scrollY) {
        impl.dispatchScrollToVisible(content, scrollY);
        DisplayTest.flushEdt();
        flushSerialCalls();
    }
}
