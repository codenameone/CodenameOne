package com.codename1.flutter;

import dart.runtime.Funcs;

/**
 * Convenience base for render elements holding a single (possibly null)
 * child widget from their configuration.
 */
public abstract class SingleChildRenderElement extends RenderElement {

    private Element child;

    protected SingleChildRenderElement(Widget widget) {
        super(widget);
    }

    /**
     * The child widget from the current configuration (may be null).
     */
    protected abstract Widget childWidget();

    @Override
    protected void syncChildren() {
        child = updateChild(child, childWidget(), 0);
    }

    public Element childElement() {
        return child;
    }

    /**
     * The render element of the child, descending through composition.
     */
    protected RenderElement renderChild() {
        return findRenderElement(child);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (child != null) {
            visitor.call(child);
        }
    }
}
