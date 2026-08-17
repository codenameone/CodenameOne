package com.example.notes.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.example.notes.model.Note;
import com.example.notes.model.NoteStore;

/** A Form subclass, which is the case that needs a generated shim. */
public class NotesForm extends Form {
    private final NoteStore store = new NoteStore().seed();
    private final Label status = new Label("ready");

    public NotesForm() {
        super("Notes", BoxLayout.y());
        for (Note n : store.sorted()) {
            add(new Label(n.toString()));
        }
        Button b = new Button("count");
        b.addActionListener(e -> status.setText("notes=" + store.count()));
        add(b).add(status);
        b.pressed();
        b.released();
        System.out.println("REALAPP: sorted=" + store.sorted()
            + " status=" + status.getText());
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        System.out.println("REALAPP: initComponent reached interpreted override");
    }
}
