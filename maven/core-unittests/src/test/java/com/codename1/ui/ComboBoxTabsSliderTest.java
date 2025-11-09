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

        class ExposedComboBox<T> extends ComboBox<T> {
            ExposedComboBox(T... values) {
                super(values);
            }

            void triggerActionEvent() {
                fireActionEvent();
            }
        }

        ExposedComboBox<String> combo = new ExposedComboBox<String>("One", "Two", "Three");
        combo.setPreferredSize(new Dimension(200, 40));
        form.add(BorderLayout.CENTER, combo);
        form.revalidate();

        assertTrue(combo.isActAsSpinnerDialog(), "New ComboBox should inherit default spinner setting");
        assertFalse(combo.isIncludeSelectCancel(), "New ComboBox should inherit include select/cancel default");

        final boolean[] actionFired = {false};
        final int[] selectionEvent = {-1};
        combo.addActionListener(evt -> {
            actionFired[0] = true;
            assertEquals("Three", combo.getSelectedItem());
        });
        combo.addSelectionListener((oldSel, newSel) -> selectionEvent[0] = newSel);

        combo.setSelectedIndex(2);
        combo.triggerActionEvent();

        assertEquals("Three", combo.getSelectedItem());
        assertEquals(2, selectionEvent[0], "Selection listener should capture updated index");
        assertTrue(actionFired[0], "Explicit trigger should invoke action listeners");

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

    @FormTest
    void sliderBasicConstructorAndGetters() {
        Slider slider = new Slider();
        assertEquals(0, slider.getProgress());
        assertEquals(100, slider.getMaxValue());
        assertEquals(0, slider.getMinValue());
        assertFalse(slider.isEditable());
        assertFalse(slider.isVertical());
        assertFalse(slider.isInfinite());
    }

    @FormTest
    void sliderSettersAndGetters() {
        Slider slider = new Slider();

        slider.setProgress(50);
        assertEquals(50, slider.getProgress());

        slider.setMaxValue(200);
        assertEquals(200, slider.getMaxValue());

        slider.setMinValue(10);
        assertEquals(10, slider.getMinValue());

        slider.setEditable(true);
        assertTrue(slider.isEditable());

        slider.setVertical(true);
        assertTrue(slider.isVertical());

        slider.setInfinite(true);
        assertTrue(slider.isInfinite());
    }

    @FormTest
    void sliderIncrementsConfiguration() {
        Slider slider = new Slider();
        slider.setIncrements(10);
        assertEquals(10, slider.getIncrements());
    }

    @FormTest
    void sliderRenderPercentageOnTop() {
        Slider slider = new Slider();
        slider.setRenderPercentageOnTop(true);
        assertTrue(slider.isRenderPercentageOnTop());

        slider.setProgress(75);
        assertEquals("75%", slider.getText());
    }

    @FormTest
    void sliderRenderValueOnTop() {
        Slider slider = new Slider();
        slider.setRenderValueOnTop(true);
        assertTrue(slider.isRenderValueOnTop());

        slider.setProgress(42);
        assertEquals("42", slider.getText());
    }

    @FormTest
    void sliderThumbImage() {
        Slider slider = new Slider();
        Image thumb = Image.createImage(10, 10);
        slider.setThumbImage(thumb);
        assertSame(thumb, slider.getThumbImage());
    }

    @FormTest
    void sliderInfiniteToggle() {
        Form form = Display.getInstance().getCurrent();
        Slider slider = new Slider();
        form.add(slider);
        form.revalidate();

        assertFalse(slider.isInfinite());
        slider.setInfinite(true);
        assertTrue(slider.isInfinite());
        slider.setInfinite(false);
        assertFalse(slider.isInfinite());
    }

    @FormTest
    void sliderVerticalOrientation() {
        Slider slider = new Slider();
        assertFalse(slider.isVertical());
        slider.setVertical(true);
        assertTrue(slider.isVertical());
    }

    @FormTest
    void sliderStyleAccessors() {
        Slider slider = new Slider();
        assertNotNull(slider.getSliderFullUnselectedStyle());
        assertNotNull(slider.getSliderFullSelectedStyle());
        assertNotNull(slider.getSliderEmptyUnselectedStyle());
        assertNotNull(slider.getSliderEmptySelectedStyle());
    }

    @FormTest
    void sliderPreferredSizeCalculation() {
        Slider slider = new Slider();
        Dimension pref = slider.getPreferredSize();
        assertTrue(pref.getWidth() > 0);
        assertTrue(pref.getHeight() > 0);
    }

    @FormTest
    void sliderInfiniteAnimation() {
        Form form = Display.getInstance().getCurrent();
        Slider slider = Slider.createInfinite();
        form.add(slider);
        form.revalidate();

        int progress1 = slider.getProgress();
        for (int i = 0; i < 100; i++) {
            slider.animate();
        }
        int progress2 = slider.getProgress();

        // Progress should have changed due to animation
        assertNotEquals(progress1, progress2);
    }

    @FormTest
    void tabsBasicConstructorAndMethods() {
        Tabs tabs = new Tabs();
        assertEquals(0, tabs.getTabCount());

        tabs.addTab("Tab1", new Label("Content1"));
        assertEquals(1, tabs.getTabCount());

        tabs.addTab("Tab2", new Label("Content2"));
        assertEquals(2, tabs.getTabCount());
    }

    @FormTest
    void tabsSelectedIndex() {
        Tabs tabs = new Tabs();
        tabs.addTab("Tab1", new Label("Content1"));
        tabs.addTab("Tab2", new Label("Content2"));
        tabs.addTab("Tab3", new Label("Content3"));

        assertEquals(0, tabs.getSelectedIndex());
        tabs.setSelectedIndex(1);
        assertEquals(1, tabs.getSelectedIndex());
        tabs.setSelectedIndex(2);
        assertEquals(2, tabs.getSelectedIndex());
    }

    @FormTest
    void tabsTabPlacement() {
        Tabs tabs = new Tabs();
        assertEquals(Component.TOP, tabs.getTabPlacement());

        tabs.setTabPlacement(Component.BOTTOM);
        assertEquals(Component.BOTTOM, tabs.getTabPlacement());

        tabs.setTabPlacement(Component.LEFT);
        assertEquals(Component.LEFT, tabs.getTabPlacement());

        tabs.setTabPlacement(Component.RIGHT);
        assertEquals(Component.RIGHT, tabs.getTabPlacement());
    }

    @FormTest
    void tabsSwipeConfiguration() {
        Tabs tabs = new Tabs();
        assertTrue(tabs.isSwipeActivated());

        tabs.setSwipeActivated(false);
        assertFalse(tabs.isSwipeActivated());

        tabs.setSwipeOnXAxis(true);
        assertTrue(tabs.isSwipeOnXAxis());
    }

    @FormTest
    void tabsRemoveTab() {
        Tabs tabs = new Tabs();
        Label content1 = new Label("Content1");
        Label content2 = new Label("Content2");

        tabs.addTab("Tab1", content1);
        tabs.addTab("Tab2", content2);
        assertEquals(2, tabs.getTabCount());

        tabs.removeTabAt(0);
        assertEquals(1, tabs.getTabCount());
    }

    @FormTest
    void tabsInsertTab() {
        Tabs tabs = new Tabs();
        tabs.addTab("Tab1", new Label("Content1"));
        tabs.addTab("Tab3", new Label("Content3"));

        tabs.insertTab("Tab2", null, new Label("Content2"), 1);
        assertEquals(3, tabs.getTabCount());
    }

    @FormTest
    void tabsTabTitle() {
        Tabs tabs = new Tabs();
        tabs.addTab("Original", new Label("Content"));
        assertEquals("Original", tabs.getTabTitle(0));

        tabs.setTabTitle("Updated", null, 0);
        assertEquals("Updated", tabs.getTabTitle(0));
    }

    @FormTest
    void tabsChangeUIID() {
        Tabs tabs = new Tabs();
        tabs.setUIID("CustomTabs");
        assertEquals("CustomTabs", tabs.getUIID());
    }

    @FormTest
    void tabsEagerSwipeMode() {
        Tabs tabs = new Tabs();
        tabs.setEagerSwipeMode(true);
        assertTrue(tabs.isEagerSwipeMode());
    }
}
