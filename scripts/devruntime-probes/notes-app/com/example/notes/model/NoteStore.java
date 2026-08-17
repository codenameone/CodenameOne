package com.example.notes.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NoteStore {
    private final List<Note> notes = new ArrayList<Note>();

    public NoteStore seed() {
        notes.add(new Note("buy milk", Note.Priority.LOW));
        notes.add(new Note("ship the release", Note.Priority.HIGH));
        notes.add(new Note("write tests", Note.Priority.NORMAL));
        return this;
    }

    public List<Note> sorted() {
        List<Note> copy = new ArrayList<Note>(notes);
        Collections.sort(copy);
        return copy;
    }

    public int count() { return notes.size(); }
}
