package com.codename1.samples;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.*;

class AutoCapitalizationSampleTest extends UITestBase {

    @FormTest
    void sentenceConstraintCapitalizesFirstLetters() {
        implementation.setDisplaySize(1080, 1920);

        Form form = new Form("Hi World", BoxLayout.y());
        form.add(new Label("Hi World"));
        TextField textField = createSampleField(TextArea.INITIAL_CAPS_SENTENCE);
        form.add(textField);

        form.show();
        startEditing(textField);

        implementation.dispatchKeyPress('h');
        implementation.dispatchKeyPress('e');
        implementation.dispatchKeyPress('l');
        implementation.dispatchKeyPress('l');
        implementation.dispatchKeyPress('o');
        implementation.dispatchKeyPress('.');
        implementation.dispatchKeyPress(' ');
        implementation.dispatchKeyPress('n');
        implementation.dispatchKeyPress('e');
        implementation.dispatchKeyPress('x');
        implementation.dispatchKeyPress('t');
        implementation.dispatchKeyPress(' ');
        implementation.dispatchKeyPress('s');
        implementation.dispatchKeyPress('e');
        implementation.dispatchKeyPress('n');
        implementation.dispatchKeyPress('t');
        implementation.dispatchKeyPress('e');
        implementation.dispatchKeyPress('n');
        implementation.dispatchKeyPress('c');
        implementation.dispatchKeyPress('e');

        assertEquals("Hello. Next sentence", textField.getText());
    }

    @FormTest
    void wordConstraintCapitalizesEachWordAcrossLines() {
        implementation.setDisplaySize(1080, 1920);

        Form form = new Form("Hi World", BoxLayout.y());
        form.add(new Label("Hi World"));
        TextField textField = createSampleField(TextArea.INITIAL_CAPS_WORD);
        form.add(textField);

        form.show();
        startEditing(textField);

        implementation.dispatchKeyPress('m');
        implementation.dispatchKeyPress('u');
        implementation.dispatchKeyPress('l');
        implementation.dispatchKeyPress('t');
        implementation.dispatchKeyPress('i');
        implementation.dispatchKeyPress(' ');
        implementation.dispatchKeyPress('l');
        implementation.dispatchKeyPress('i');
        implementation.dispatchKeyPress('n');
        implementation.dispatchKeyPress('e');
        implementation.dispatchKeyPress('\n');
        implementation.dispatchKeyPress('a');
        implementation.dispatchKeyPress('u');
        implementation.dispatchKeyPress('t');
        implementation.dispatchKeyPress('o');
        implementation.dispatchKeyPress(' ');
        implementation.dispatchKeyPress('c');
        implementation.dispatchKeyPress('a');
        implementation.dispatchKeyPress('p');
        implementation.dispatchKeyPress('i');
        implementation.dispatchKeyPress('t');
        implementation.dispatchKeyPress('a');
        implementation.dispatchKeyPress('l');
        implementation.dispatchKeyPress('i');
        implementation.dispatchKeyPress('z');
        implementation.dispatchKeyPress('a');
        implementation.dispatchKeyPress('t');
        implementation.dispatchKeyPress('i');
        implementation.dispatchKeyPress('o');
        implementation.dispatchKeyPress('n');

        assertEquals("Multi Line\nAuto Capitalization", textField.getText());
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

    private TextField createSampleField(int constraint) {
        TextField textField = new TextField();
        textField.setSingleLineTextArea(false);
        textField.setConstraint(constraint);
        textField.setGrowLimit(-1);
        textField.setMaxSize(1600);
        textField.setHint("Type Something...");
        textField.setQwertyInput(true);
        return textField;
    }
}
