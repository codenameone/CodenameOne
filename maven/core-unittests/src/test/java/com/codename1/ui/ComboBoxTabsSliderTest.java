package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.SelectionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.geom.Dimension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class ComboBoxTabsSliderTest extends UITestBase {
    private boolean originalActAsSpinner;
    private boolean originalIncludeSelectCancel;

    @BeforeEach
    void stashComboBoxDefaults() {
        originalActAsSpinner = ComboBox.isDefaultActAsSpinnerDialog();
        originalIncludeSelectCancel = ComboBox.isDefaultIncludeSelectCancel();
    }

    @AfterEach
    void restoreComboBoxDefaults() {
        ComboBox.setDefaultActAsSpinnerDialog(originalActAsSpinner);
        ComboBox.setDefaultIncludeSelectCancel(originalIncludeSelectCancel);
    }

    @FormTest
    void comboBoxRespectsDefaultsAndFiresActionOnSelectionChange() {
        implementation.setBuiltinSoundsEnabled(false);
        ComboBox.setDefaultActAsSpinnerDialog(true);
        ComboBox.setDefaultIncludeSelectCancel(false);

        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        ComboBox<String> combo = new ComboBox<String>("One", "Two", "Three");
        combo.setPreferredSize(new Dimension(200, 40));
        form.add(BorderLayout.CENTER, combo);
        form.revalidate();

        assertTrue(combo.isActAsSpinnerDialog(), "New ComboBox should inherit default spinner setting");
        assertFalse(combo.isIncludeSelectCancel(), "New ComboBox should inherit include select/cancel default");

        final boolean[] fired = {false};
        combo.addActionListener(evt -> {
            fired[0] = true;
            assertEquals("Three", combo.getSelectedItem());
        });

        combo.setHandlesInput(true);
        combo.setSelectedIndex(2);
        combo.pointerReleased(combo.getAbsoluteX() + combo.getWidth() / 2, combo.getAbsoluteY() + combo.getHeight() / 2);

        assertEquals("Three", combo.getSelectedItem());
        assertTrue(fired[0], "pointerReleased while handlesInput is true should fire action listeners");

        Image icon = Image.createImage(8, 8);
        combo.setComboBoxImage(icon);
        assertSame(icon, combo.getComboBoxImage());

        combo.setActAsSpinnerDialog(false);
        combo.setIncludeSelectCancel(true);
        assertFalse(combo.isActAsSpinnerDialog());
        assertTrue(combo.isIncludeSelectCancel());
    }

    @FormTest
    void tabsSelectionAndSwipeConfiguration() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Tabs tabs = new Tabs();
        tabs.addTab("First", new Label("Content A"));
        tabs.addTab("Second", new Label("Content B"));
        tabs.addTab("Third", new Label("Content C"));

        final int[] lastSelection = {-1};
        tabs.addSelectionListener(new SelectionListener() {
            public void selectionChanged(int oldSelected, int newSelected) {
                lastSelection[0] = newSelected;
            }
        });

        form.add(BorderLayout.CENTER, tabs);
        form.revalidate();

        tabs.setSelectedIndex(1);
        assertEquals(1, tabs.getSelectedIndex());
        assertEquals(1, lastSelection[0], "Selection listener should be notified of changes");

        tabs.setTabPlacement(Component.BOTTOM);
        assertEquals(Component.BOTTOM, tabs.getTabPlacement());

        tabs.setSwipeActivated(false);
        assertFalse(tabs.isSwipeActivated());
        assertFalse(tabs.shouldBlockSideSwipe(), "Swipe disabled should allow side swipes");

        tabs.setSwipeOnXAxis(false);
        assertFalse(tabs.isSwipeOnXAxis());

        tabs.setSwipeActivated(true);
        assertTrue(tabs.isSwipeActivated());
    }

    @FormTest
    void sliderPointerInteractionUpdatesValueAndFiresEvents() {
        implementation.setBuiltinSoundsEnabled(false);
        Form form = Display.getInstance().getCurrent();
        form.setLayout(new BorderLayout());

        Slider slider = new Slider();
        slider.setEditable(true);
        slider.setMinValue(0);
        slider.setMaxValue(100);
        slider.setPreferredSize(new Dimension(300, 30));

        final int[] dataChangeValue = {-1};
        final int[] actionValue = {-1};
        slider.addDataChangedListener((type, index) -> dataChangeValue[0] = index);
        slider.addActionListener(evt -> actionValue[0] = slider.getProgress());

        form.add(BorderLayout.CENTER, slider);
        form.revalidate();

        int pointerX = slider.getAbsoluteX() + slider.getWidth() * 3 / 4;
        int pointerY = slider.getAbsoluteY() + slider.getHeight() / 2;

        form.pointerPressed(pointerX, pointerY);
        form.pointerDragged(pointerX, pointerY);
        form.pointerReleased(pointerX, pointerY);

        assertTrue(slider.getProgress() > slider.getMinValue(), "Pointer drag should increase slider value");
        assertEquals(slider.getProgress(), dataChangeValue[0], "Data changed listener should receive new progress value");
        assertEquals(slider.getProgress(), actionValue[0], "Action listener should be invoked on release");

        Slider infinite = Slider.createInfinite();
        assertTrue(infinite.isInfinite());
        int before = infinite.getProgress();
        infinite.animate();
        assertNotEquals(before, infinite.getProgress(), "Infinite slider should change value during animation");
    }
}
