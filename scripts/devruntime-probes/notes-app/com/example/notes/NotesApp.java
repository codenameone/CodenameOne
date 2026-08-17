package com.example.notes;

import com.codename1.system.Lifecycle;
import com.example.notes.ui.NotesForm;

/** A real application shape: a Lifecycle entry point, no main anywhere. */
public class NotesApp extends Lifecycle {
    @Override
    public void start() {
        if (isStartedBefore()) {
            return;
        }
        new NotesForm().show();
        System.out.println("REALAPP: started, form shown");
    }

    private boolean started;
    private boolean isStartedBefore() {
        boolean was = started;
        started = true;
        return was;
    }
}
