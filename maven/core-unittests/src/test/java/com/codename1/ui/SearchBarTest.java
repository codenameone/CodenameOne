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

        TestSearchBar(Toolbar parent, float iconSize) {
            super(parent, iconSize);
        }

        @Override
        public void onSearch(String text) {
            lastQuery = text;
            callCount++;
        }
    }

    @FormTest
    void testInitSearchBarSetsTitleComponent() {
        Form form = Display.getInstance().getCurrent();
        Toolbar parent = new Toolbar();
        form.setToolbar(parent);
        TestSearchBar searchBar = new TestSearchBar(parent, 0);
        searchBar.initSearchBar();

        assertTrue(searchBar.getTitleComponent() instanceof TextField);
        assertNotNull(form.getBackCommand());

        TextField field = (TextField) searchBar.getTitleComponent();
        field.setText("hello");
        field.fireDataChanged(DataChangedListener.CHANGED, 0);
        assertEquals("hello", searchBar.lastQuery);
        assertEquals(1, searchBar.callCount);

        Command clearCommand = searchBar.getRightBarCommands().iterator().next();
        clearCommand.actionPerformed(new ActionEvent(searchBar));
        assertEquals("", field.getText());
    }
}
