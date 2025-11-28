package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.AutoCompleteTextField;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

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
        private final int delayMs;
        private Timer pendingTimer;
        private String pendingText;

        AsyncAutoCompleteField(DefaultListModel<String> options, String[] database, int delay) {
            super(options);
            this.options = options;
            this.database = database;
            this.delayMs = delay;
        }

        @Override
        protected boolean filter(final String textParam) {
            String text = textParam;
            if (text == null) {
                text = "";
            }

            if (text.length() == 0) {
                cancelPending();
                options.removeAll();
                updateFilterList();
                return true;
            }

            if (text.equals(pendingText)) {
                return false;
            }

            cancelPending();
            pendingText = text;

            final String query = text;
            pendingTimer = CN.setTimeout(delayMs, new Runnable() {
                public void run() {
                    Timer current = pendingTimer;
                    pendingTimer = null;
                    pendingText = null;

                    options.removeAll();
                    String lower = query.toLowerCase();
                    for (int i = 0; i < database.length; i++) {
                        String city = database[i];
                        if (city.toLowerCase().startsWith(lower)) {
                            options.addItem(city);
                        }
                    }
                    AsyncAutoCompleteField.super.filter(query);
                    updateFilterList();

                    if (current != null) {
                        current.cancel();
                    }
                }
            });

            return false;
        }

        private void cancelPending() {
            if (pendingTimer != null) {
                pendingTimer.cancel();
                pendingTimer = null;
            }
            pendingText = null;
        }

        List<String> copySuggestions() {
            ArrayList<String> suggestions = new ArrayList<String>();
            for (int i = 0; i < options.getSize(); i++) {
                suggestions.add(options.getItemAt(i));
            }
            return suggestions;
        }

        boolean hasPendingQuery() {
            return pendingTimer != null || pendingText != null;
        }
    }
}
