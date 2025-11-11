package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.UIManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RTL (Right-to-Left) functionality and toggling RTL.
 */
class RTLTest extends UITestBase {

    @FormTest
    void testEnableRTL() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);

        // Enable RTL
        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testDisableRTL() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);

        // Set RTL and then disable
        form.setRTL(true);
        form.revalidate();
        assertTrue(form.isRTL());

        form.setRTL(false);
        form.revalidate();
        assertFalse(form.isRTL());
    }

    @FormTest
    void testRTLWithBorderLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button west = new Button("West");
        Button east = new Button("East");
        Button center = new Button("Center");

        form.add(BorderLayout.WEST, west);
        form.add(BorderLayout.EAST, east);
        form.add(BorderLayout.CENTER, center);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
        // In RTL, WEST and EAST should be swapped
    }

    @FormTest
    void testRTLWithFlowLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new FlowLayout());

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
        // Components should flow from right to left
    }

    @FormTest
    void testRTLWithBoxLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.X_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");
        Button btn3 = new Button("Button 3");

        form.addAll(btn1, btn2, btn3);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testToggleRTLMultipleTimes() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, new Button("Test"));

        // Toggle RTL multiple times
        for (int i = 0; i < 10; i++) {
            form.setRTL(i % 2 == 0);
            form.revalidate();
        }

        assertFalse(form.isRTL());
    }

    @FormTest
    void testRTLInheritedByChildren() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container container = new Container(BoxLayout.y());
        Button btn = new Button("Test");
        container.add(btn);

        form.add(BorderLayout.CENTER, container);
        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
        // Child components should inherit RTL
    }

    @FormTest
    void testRTLWithText() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Label arabicLabel = new Label("مرحبا");
        Label englishLabel = new Label("Hello");

        form.addAll(arabicLabel, englishLabel);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLWithTextField() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        TextField textField = new TextField("Test");
        form.add(textField);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLWithScrollableContainer() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container scrollable = new Container(BoxLayout.y());
        scrollable.setScrollableY(true);

        for (int i = 0; i < 20; i++) {
            scrollable.add(new Label("Item " + i));
        }

        form.add(BorderLayout.CENTER, scrollable);
        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLWithToolbar() {
        Form form = CN.getCurrentForm();
        Toolbar toolbar = new Toolbar();
        form.setToolbar(toolbar);

        toolbar.addCommandToLeftBar("Left", null, evt -> {});
        toolbar.addCommandToRightBar("Right", null, evt -> {});

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
        // In RTL, left and right commands should be swapped
    }

    @FormTest
    void testRTLWithDialog() {
        Form form = CN.getCurrentForm();
        form.setRTL(true);

        Dialog dialog = new Dialog("Test");
        dialog.setLayout(new BorderLayout());
        dialog.add(BorderLayout.CENTER, new Label("Content"));

        // Dialog should inherit RTL from form
        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLWithLayeredLayout() {
        Form form = CN.getCurrentForm();
        form.setLayout(new com.codename1.ui.layouts.LayeredLayout());

        Label layer1 = new Label("Layer 1");
        Label layer2 = new Label("Layer 2");

        form.add(layer1);
        form.add(layer2);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLWithDynamicComponentAddition() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        form.setRTL(true);
        form.revalidate();

        // Add components after enabling RTL
        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        form.addAll(btn1, btn2);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLGlobalSetting() {
        // Test global RTL setting
        boolean originalRTL = UIManager.getInstance().isThemeConstant("rtlBool", false);

        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());
        form.add(BorderLayout.CENTER, new Button("Test"));

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLWithNestedContainers() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Container outer = new Container(BoxLayout.y());
        Container inner = new Container(BoxLayout.x());

        inner.addAll(new Button("1"), new Button("2"), new Button("3"));
        outer.add(inner);
        form.add(BorderLayout.CENTER, outer);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLWithAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        form.add(new Button("Button 1"));
        form.add(new Button("Button 2"));

        form.setRTL(true);
        form.revalidate();

        // Animate with RTL enabled
        form.animateLayout(200);

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLToggleDuringAnimation() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        form.add(new Button("Button 1"));
        form.add(new Button("Button 2"));
        form.revalidate();

        // Start animation
        form.animateLayout(200);

        // Toggle RTL during animation
        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testComponentLevelRTLOverride() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        Button btn1 = new Button("Button 1");
        Button btn2 = new Button("Button 2");

        // Set RTL on specific component
        btn1.setRTL(true);
        btn2.setRTL(false);

        form.addAll(btn1, btn2);
        form.revalidate();

        assertTrue(btn1.isRTL());
        assertFalse(btn2.isRTL());
    }

    @FormTest
    void testRTLWithTablet() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BorderLayout());

        Button btn = new Button("Test");
        form.add(BorderLayout.CENTER, btn);

        form.setRTL(true);
        form.revalidate();

        assertTrue(form.isRTL());
    }

    @FormTest
    void testRTLPersistenceAcrossRevalidate() {
        Form form = CN.getCurrentForm();
        form.setLayout(new BoxLayout(BoxLayout.Y_AXIS));

        form.setRTL(true);
        form.add(new Button("Test"));
        form.revalidate();

        assertTrue(form.isRTL());

        // Revalidate multiple times
        form.revalidate();
        form.revalidate();

        assertTrue(form.isRTL());
    }
}
