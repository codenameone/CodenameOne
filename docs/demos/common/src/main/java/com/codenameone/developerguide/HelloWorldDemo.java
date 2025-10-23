package com.codenameone.developerguide;

import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Simple hello world demo showing a dialog.
 */
public class HelloWorldDemo implements Demo {

    @Override
    public String getTitle() {
        return "Hello World";
    }

    @Override
    public String getDescription() {
        return "Shows a button that pops up a welcome dialog.";
    }

    @Override
    public void show(Form parent) {
        Form form = new Form("Hello World", BoxLayout.y());
        form.setName("helloWorldForm");
        form.getToolbar().setBackCommand("Back", e -> parent.showBack());
        Button helloButton = new Button("Say Hello");
        helloButton.setName("helloButton");
        helloButton.addActionListener(e -> Dialog.show("Hello Codename One", "Welcome to Codename One", "OK", null));
        form.add(helloButton);
        form.show();
    }
}
