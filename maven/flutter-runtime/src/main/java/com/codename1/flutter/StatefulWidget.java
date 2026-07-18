package com.codename1.flutter;

/**
 * A widget with mutable state. The framework creates the {@link State} object
 * when the widget is first inflated into an element and keeps it alive across
 * rebuilds as long as reconciliation reuses that element.
 */
public abstract class StatefulWidget extends Widget {

    public abstract State<? extends StatefulWidget> createState();

    @Override
    public Element createElement() {
        return new StatefulElement(this);
    }
}
