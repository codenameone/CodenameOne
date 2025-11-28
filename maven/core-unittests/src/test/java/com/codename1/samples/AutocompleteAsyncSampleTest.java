package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.AutoCompleteTextField;
import com.codename1.ui.CN;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import com.codename1.ui.events.DataChangedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AutocompleteAsyncSampleTest extends UITestBase {

    private static final String[] CITY_DATABASE = new String[]{
            "Dubai",
            "Dibba Al-Fujairah",
            "Dibba Al-Hisn",
            "Sharjah",
            "Abu Dhabi",
            "Ajman",
            "Charikar",
            "Kabul"
    };

    @FormTest
    void asyncFilteringAddsPrefixMatchesAfterDelay() {
        Form form = new Form("Async Autocomplete", BoxLayout.y());
        AsyncAutoCompleteField field = createAsyncField();
        form.add(field);

        form.show();
        startEditing(field);

        typeText("Du");
        waitForAsync(field);

        List<String> suggestions = field.copySuggestions();
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.contains("Dubai"));
    }

    @FormTest
    void latestQueryCancelsPreviousPendingResult() {
        Form form = new Form("Async Autocomplete", BoxLayout.y());
        AsyncAutoCompleteField field = createAsyncField();
        form.add(field);

        form.show();
        startEditing(field);

        typeText("D");
        typeText("i");
        waitForAsync(field);

        List<String> suggestions = field.copySuggestions();
        assertTrue(suggestions.contains("Dibba Al-Fujairah"));
        assertTrue(suggestions.contains("Dibba Al-Hisn"));
        assertFalse(suggestions.contains("Dubai"));
    }

    @FormTest
    void clearingTextRemovesSuggestions() {
        Form form = new Form("Async Autocomplete", BoxLayout.y());
        AsyncAutoCompleteField field = createAsyncField();
        form.add(field);

        form.show();
        startEditing(field);

        typeText("Ka");
        waitForAsync(field);
        assertFalse(field.copySuggestions().isEmpty());

        field.setText("");
        waitForAsync(field);

        assertEquals(0, field.copySuggestions().size());
    }

    private AsyncAutoCompleteField createAsyncField() {
        DefaultListModel<String> options = new DefaultListModel<String>();
        return new AsyncAutoCompleteField(options, CITY_DATABASE, 20);
    }

    private void typeText(String text) {
        for (int i = 0; i < text.length(); i++) {
            implementation.dispatchKeyPress(text.charAt(i));
        }
    }

    private void waitForAsync(AsyncAutoCompleteField field) {
        for (int i = 0; i < 25; i++) {
            DisplayTest.flushEdt();
            flushSerialCalls();
            if (!field.hasPendingQuery()) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        DisplayTest.flushEdt();
        flushSerialCalls();
    }

    private void startEditing(TextField textField) {
        textField.startEditingAsync();
        flushSerialCalls();
        DisplayTest.flushEdt();
        flushSerialCalls();

        if (!implementation.isEditingText(textField)) {
            textField.requestFocus();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();
        }

        if (!implementation.isEditingText(textField)) {
            textField.startEditingAsync();
            flushSerialCalls();
            DisplayTest.flushEdt();
            flushSerialCalls();
        }
    }

    private static class AsyncAutoCompleteField extends AutoCompleteTextField {

        private final DefaultListModel<String> options;
        private final String[] database;
        private final AtomicReference<String> pendingText = new AtomicReference<String>();
        private final int delayMs;
        private boolean ready;

        AsyncAutoCompleteField(DefaultListModel<String> options, String[] database, int delay) {
            super(options);
            this.options = options;
            this.database = database;
            this.delayMs = delay;
            this.ready = true;
            addDataChangeListener(new DataChangedListener() {
                public void dataChanged(int type, int index) {
                    processFilter();
                }
            });
        }

        @Override
        public void keyReleased(int keyCode) {
            super.keyReleased(keyCode);
            processFilter();
        }

        @Override
        public void setText(String text) {
            if (!ready) {
                super.setText(text);
                return;
            }
            super.setText(text);
            processFilter();
        }

        @Override
        protected boolean filter(String text) {
            if (!ready) {
                return false;
            }
            if (text == null) {
                text = "";
            }
            if (text.length() == 0) {
                options.removeAll();
                super.filter(text);
                cancelPending();
                return true;
            }
            if (text.equals(pendingText.get())) {
                return false;
            }
            cancelPending();
            final String filterText = text;
            pendingText.set(filterText);
            CN.callSerially(new Runnable() {
                public void run() {
                    if (!filterText.equals(pendingText.get())) {
                        return;
                    }
                    options.removeAll();
                    for (int i = 0; i < database.length; i++) {
                        String city = database[i];
                        if (city.toLowerCase().startsWith(filterText.toLowerCase())) {
                            options.addItem(city);
                        }
                    }
                    AsyncAutoCompleteField.super.filter(filterText);
                    pendingText.set(null);
                    updateFilterList();
                }
            });
            return false;
        }

        private void cancelPending() {
            pendingText.set(null);
        }

        private void processFilter() {
            if (!ready) {
                return;
            }
            boolean changed = filter(getText());
            if (changed) {
                updateFilterList();
            }
        }

        List<String> copySuggestions() {
            ArrayList<String> suggestions = new ArrayList<String>();
            ListModel<String> model = getSuggestionModel();
            for (int i = 0; i < model.getSize(); i++) {
                suggestions.add(model.getItemAt(i));
            }
            return suggestions;
        }

        boolean hasPendingQuery() {
            return pendingText.get() != null;
        }
    }
}
