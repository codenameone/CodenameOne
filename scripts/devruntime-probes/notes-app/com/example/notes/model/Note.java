package com.example.notes.model;

public class Note implements Comparable<Note> {
    public enum Priority { LOW, NORMAL, HIGH }

    private final String title;
    private final Priority priority;

    public Note(String title, Priority priority) {
        this.title = title;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public Priority getPriority() { return priority; }

    public int compareTo(Note o) { return o.priority.ordinal() - priority.ordinal(); }

    @Override
    public String toString() { return title + " [" + priority + "]"; }
}
