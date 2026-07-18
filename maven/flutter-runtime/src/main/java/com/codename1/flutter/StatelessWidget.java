package com.codename1.flutter;

/**
 * A widget that describes part of the UI purely as a function of its
 * configuration: {@link #build(BuildContext)} composes other widgets.
 */
public abstract class StatelessWidget extends Widget {

    public abstract Widget build(BuildContext context);

    @Override
    public Element createElement() {
        return new StatelessElement(this);
    }
}
