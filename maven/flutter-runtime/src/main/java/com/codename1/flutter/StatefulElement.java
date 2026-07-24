package com.codename1.flutter;

/**
 * Element for a {@link StatefulWidget}. Owns the {@link State} instance:
 * created (and {@code initState} run) on mount, retargeted with
 * {@code didUpdateWidget} when a new widget of the same type/key arrives,
 * disposed on unmount.
 */
public class StatefulElement extends ComposedElement {

    private final State<? extends StatefulWidget> state;

    public StatefulElement(StatefulWidget widget) {
        super(widget);
        this.state = widget.createState();
        this.state.attach(this, widget);
    }

    public State<? extends StatefulWidget> state() {
        return state;
    }

    @Override
    protected void firstBuild() {
        state.initState();
        // Flutter runs didChangeDependencies right after initState and before the first
        // build; widgets that create controllers there (e.g. a PageController sized from
        // MediaQuery) rely on it having run before build reads them.
        state.didChangeDependencies();
        super.firstBuild();
    }

    @Override
    public void update(Widget newWidget) {
        StatefulWidget oldWidget = (StatefulWidget) widget;
        widget = newWidget;
        state.updateWidget((StatefulWidget) newWidget);
        state.invokeDidUpdateWidget(oldWidget);
        dirty = true;
        performRebuild();
    }

    @Override
    public void unmount() {
        super.unmount();
        state.dispose();
        state.detach();
    }

    @Override
    protected Widget build() {
        return state.build(this);
    }
}
