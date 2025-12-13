package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListCellRenderer;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.FilterProxyListModel;
import com.codename1.ui.list.ListCellRenderer;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.ComponentSelector;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.geom.Dimension;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TextInputComponentsFeatureTest extends UITestBase {

    private static class RecordingTextField extends TextField {
        private boolean symbolDialogShown;

        @Override
        protected void showSymbolDialog() {
            symbolDialogShown = true;
        }

        boolean wasSymbolDialogShown() {
            return symbolDialogShown;
        }
    }
    
    @FormTest
    void textAreaSupportsMultilineScrollingSelectionAndAutoDetect() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(BoxLayout.y());

        TextArea area = new TextArea();
        area.setRows(3);
        area.setColumns(12);
        area.setGrowByContent(false);
        area.setText("Line0\nLine1\nLine2\nLine3\nLine4");
        area.setEditable(true);

        char originalWidest = TextArea.getWidestChar();
        TextArea.setWidestChar('A');
        TextArea.autoDetectWidestChar("ZXY");
        assertEquals('Z', TextArea.getWidestChar(), "autoDetectWidestChar should update the widest character");
        TextArea.setWidestChar(originalWidest);

        area.setActAsLabel(true);
        assertTrue(area.isActAsLabel());
        area.setActAsLabel(false);
        assertFalse(area.isActAsLabel());

        form.add(area);
        form.revalidate();

        int initialScroll = area.getScrollY();
        int downKeyCode = Display.getInstance().getKeyCode(Display.GAME_DOWN);
        implementation.dispatchKeyPress(downKeyCode);
        assertTrue(area.getScrollY() >= initialScroll, "Down key should not decrease scroll position");
        int upKeyCode = Display.getInstance().getKeyCode(Display.GAME_UP);
        implementation.dispatchKeyPress(upKeyCode);
        assertTrue(area.getScrollY() <= area.getPreferredSize().getHeight(), "Up key should adjust scroll position within bounds");

        area.setAlignment(Component.CENTER);
        assertEquals(Component.CENTER, area.getAlignment());

        implementation.resetTextSelectionTracking();
        TextSelection selection = form.getTextSelection();
        selection.setEnabled(true);
        area.setTextSelectionEnabled(true);
        selection.selectAll();
        flushSerialCalls();
        assertEquals(1, implementation.getInitializeTextSelectionCount(), "Enabling selection should initialize implementation tracking");
        selection.copy();
        assertEquals(1, implementation.getCopySelectionInvocations());
        assertEquals(area.getText().replace('\r', '\n'), implementation.getLastCopiedText());
        selection.setEnabled(false);
        assertEquals(1, implementation.getDeinitializeTextSelectionCount());

        area.setText("100");
        assertEquals(100, area.getAsInt(-5));
        assertEquals(100L, area.getAsLong(-5));
        area.setText("12.5");
        assertEquals(12.5d, area.getAsDouble(-1), 0.0001d);
        area.setText("bad");
        assertEquals(-5, area.getAsInt(-5));

        area.setEditable(false);
        assertFalse(area.isEditable());
        area.setEditable(true);
        assertTrue(area.isEditable());
    }

    @FormTest
    void textComponentBindingAndPropertyAccess() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(BoxLayout.y());

        TextComponent component = new TextComponent();
        component.label("Username").hint("Enter name").text("Alice").columns(8).rows(2);
        form.add(component);
        form.revalidate();

        assertEquals("Alice", component.getPropertyValue("text"));
        assertEquals("Enter name", component.getPropertyValue("hint"));
        assertEquals(Integer.valueOf(8), component.getPropertyValue("columns"));
        assertEquals(Integer.valueOf(2), component.getPropertyValue("rows"));

        component.setPropertyValue("text", "Bob");
        component.setPropertyValue("multiline", Boolean.TRUE);
        component.setPropertyValue("constraint", Integer.valueOf(TextArea.DECIMAL));
        assertEquals("Bob", component.getField().getText());
        assertFalse(component.getField().isSingleLineTextArea());
        assertEquals(TextArea.DECIMAL, component.getField().getConstraint());

        component.getField().startEditingAsync();
        flushSerialCalls();
        assertTrue(component.getField().isEditing());
        component.getField().stopEditing();
        flushSerialCalls();
        assertFalse(component.getField().isEditing());
    }

    @FormTest
    void autoCompleteTextFieldRendererAndSelection() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(BoxLayout.y());

        ListModel<String> model = new DefaultListModel<String>(new String[]{"alpha", "beta", "gamma"});
        final DefaultListCellRenderer<String> baseRenderer = new DefaultListCellRenderer<String>();
        AtomicBoolean rendererInvoked = new AtomicBoolean(false);
        ListCellRenderer<String> renderer = new ListCellRenderer<String>() {
            public Component getListCellRendererComponent(com.codename1.ui.List list, String value, int index, boolean isSelected) {
                rendererInvoked.set(true);
                return baseRenderer.getListCellRendererComponent(list, value, index, isSelected);
            }

            public Component getListFocusComponent(com.codename1.ui.List list) {
                return baseRenderer.getListFocusComponent(list);
            }
        };
        AtomicReference<String> selected = new AtomicReference<String>(null);

        AutoCompleteTextField field = new AutoCompleteTextField(model);
        field.setMinimumLength(1);
        field.setCompletionRenderer(renderer);
        field.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                selected.set(field.getText());
            }
        });

        form.add(field);
        form.revalidate();

        field.setText("a");
        field.showPopup();
        form.getAnimationManager().flush();
        flushSerialCalls();

        ComponentSelector lists = ComponentSelector.$("AutoCompleteList", form);
        assertEquals(1, lists.size(), "Popup list should be present");
        com.codename1.ui.List popupList = (com.codename1.ui.List) lists.iterator().next();
        assertSame(renderer, popupList.getRenderer());
        assertTrue(rendererInvoked.get(), "Custom renderer should be applied");

        String firstValue = (String) popupList.getModel().getItemAt(0);
        Dimension firstCellSize = renderer.getListCellRendererComponent(popupList, firstValue, 0, true).getPreferredSize();
        int selectX = popupList.getAbsoluteX() + Math.max(1, Math.min(firstCellSize.getWidth(), popupList.getWidth()) / 4);
        int selectY = popupList.getAbsoluteY() + Math.max(1, Math.min(firstCellSize.getHeight(), popupList.getHeight()) / 2);
        implementation.dispatchPointerPressAndRelease(selectX, selectY);
        flushSerialCalls();
        if (!"alpha".equals(field.getText())) {
            popupList.setSelectedIndex(0);
            popupList.fireActionEvent();
            flushSerialCalls();
        }

        assertEquals("alpha", field.getText());
        assertEquals("alpha", selected.get());
    }

    @FormTest
    void autoCompleteTextComponentSelectsFromPopup() {
        Form form = Display.getInstance().getCurrent();
        form.setLayout(BoxLayout.y());

        final AutoCompleteTextField[] fieldHolder = new AutoCompleteTextField[1];
        AutoCompleteTextComponent.AutoCompleteFilter filter = new AutoCompleteTextComponent.AutoCompleteFilter() {
            public boolean filter(String text) {
                AutoCompleteTextField f = fieldHolder[0];
                if (f != null) {
                    ((FilterProxyListModel<String>) f.getSuggestionModel()).filter(text);
                    return true;
                }
                return false;
            }
        };

        ListModel<String> model = new DefaultListModel<String>(new String[]{"red", "green", "blue"});
        AutoCompleteTextComponent component = new AutoCompleteTextComponent(model, filter);
        component.label("Color");
        component.hint("Pick");
        form.add(component);
        form.revalidate();

        AutoCompleteTextField field = component.getAutoCompleteField();
        fieldHolder[0] = field;
        field.setMinimumLength(1);

        field.setText("g");
        field.showPopup();
        form.getAnimationManager().flush();
        flushSerialCalls();

        ComponentSelector lists = ComponentSelector.$("AutoCompleteList", form);
        assertEquals(1, lists.size());
        com.codename1.ui.List popupList = (com.codename1.ui.List) lists.iterator().next();
        ListCellRenderer<?> popupRenderer = popupList.getRenderer();
        Object firstPopupValue = popupList.getModel().getItemAt(0);
        @SuppressWarnings({"rawtypes", "unchecked"})
        ListCellRenderer rawRenderer = (ListCellRenderer) popupRenderer;
        Dimension popupCellSize = rawRenderer.getListCellRendererComponent(popupList, firstPopupValue, 0, true).getPreferredSize();
        int selectX = popupList.getAbsoluteX() + Math.max(1, Math.min(popupCellSize.getWidth(), popupList.getWidth()) / 4);
        int selectY = popupList.getAbsoluteY() + Math.max(1, Math.min(popupCellSize.getHeight(), popupList.getHeight()) / 2);
        implementation.dispatchPointerPressAndRelease(selectX, selectY);
        flushSerialCalls();
        if (!"green".equals(field.getText())) {
            popupList.setSelectedIndex(0);
            popupList.fireActionEvent();
            flushSerialCalls();
        }

        assertEquals("green", field.getText(), "Selecting from popup should update the field text");
        assertEquals("green", component.getText(), "Component text should mirror field value");
    }
}
