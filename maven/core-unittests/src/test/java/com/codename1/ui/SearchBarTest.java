package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.TextField;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.DataChangedListener;

import static org.junit.jupiter.api.Assertions.*;

class SearchBarTest extends UITestBase {

    private static class TestSearchBar extends SearchBar {
        private String lastQuery;
        private int callCount;
        private Command clearCommand;

        TestSearchBar(Toolbar parent, float iconSize) {
            super(parent, iconSize);
        }

        @Override
        public void onSearch(String text) {
            lastQuery = text;
            callCount++;
        }

        @Override
        public void addCommandToRightBar(Command command) {
            super.addCommandToRightBar(command);
            clearCommand = command;
        }

        void triggerClear() {
            if (clearCommand == null) {
                throw new IllegalStateException("Clear command not initialized");
            }
            clearCommand.actionPerformed(new ActionEvent(this));
        }
    }

    @FormTest
    void testInitSearchBarSetsTitleComponent() {
        Form form = Display.getInstance().getCurrent();
        Toolbar parent = new Toolbar();
        form.setToolBar(parent);
        TestSearchBar searchBar = new TestSearchBar(parent, 0);
        form.setToolBar(searchBar);
        searchBar.initSearchBar();

        assertTrue(searchBar.getTitleComponent() instanceof TextField);
        assertNotNull(form.getBackCommand());

        TextField field = (TextField) searchBar.getTitleComponent();
        field.setText("hello");
        field.fireDataChanged(DataChangedListener.CHANGED, 0);
        assertEquals("hello", searchBar.lastQuery);
        assertTrue(searchBar.callCount >= 1);

        searchBar.triggerClear();
        flushSerialCalls();
        assertEquals("", field.getText());
        assertEquals("", searchBar.lastQuery);
    }
}
