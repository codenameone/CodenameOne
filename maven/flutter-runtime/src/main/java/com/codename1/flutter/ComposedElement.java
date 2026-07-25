package com.codename1.flutter;

import dart.runtime.Funcs;

/**
 * Base class for elements that compose exactly one child by calling a build
 * method (Flutter's ComponentElement): {@link StatelessElement} and
 * {@link StatefulElement}.
 */
public abstract class ComposedElement extends Element {

    private Element child;

    protected ComposedElement(Widget widget) {
        super(widget);
    }

    public Element child() {
        return child;
    }

    @Override
    public void mount(Element parent, int slot) {
        super.mount(parent, slot);
        firstBuild();
    }

    protected void firstBuild() {
        dirty = true;
        performRebuild();
    }

    @Override
    public void update(Widget newWidget) {
        super.update(newWidget);
        dirty = true;
        performRebuild();
    }

    @Override
    protected void performRebuild() {
        dirty = false;
        // Name the widget being built, so a failure inside it (a Dart `!` on
        // something that turned out null, most often) reports where it
        // happened. The transpiled build methods are inlined into the
        // framework's frame on some backends, so the stack trace alone shows
        // nothing but this class's own recursion.
        String previous = dart.runtime.DartRuntime.diagnosticContext();
        dart.runtime.DartRuntime.diagnosticContext(
                "building " + (widget == null ? "null" : widget.getClass().getName()));
        Widget built;
        try {
            built = build();
        } finally {
            dart.runtime.DartRuntime.diagnosticContext(previous);
        }
        child = updateChild(child, built, 0);
    }

    /**
     * Calls the widget's (or state's) build method.
     */
    protected abstract Widget build();

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (child != null) {
            visitor.call(child);
        }
    }
}
