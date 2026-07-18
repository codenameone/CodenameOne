package com.codename1.flutter;

/**
 * Element for a {@link StatelessWidget}: rebuilding calls the widget's build
 * method and reconciles the single resulting child.
 */
public class StatelessElement extends ComposedElement {

    public StatelessElement(StatelessWidget widget) {
        super(widget);
    }

    @Override
    protected Widget build() {
        return ((StatelessWidget) widget).build(this);
    }
}
