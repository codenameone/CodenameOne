package com.codename1.flutter.material;

import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * Render element for {@link PopupMenuButton}: draws the trigger as a real button and OPENS
 * THE MENU when it is pressed.
 *
 * <p>It is a {@link ButtonRenderElement} rather than a tappable wrapper around the glyph,
 * and that is not incidental. An InkWell's gesture pane is an ordinary sibling in the flat
 * component list, so a scroll view's own pane — added later, sitting above — swallowed the
 * press before it arrived: the menu button drew correctly and did nothing, which is
 * indistinguishable from a missing handler. A CN1 Button receives its own events, exactly as
 * the neighbouring IconButtons in the same app bar already did.</p>
 *
 * <p>With neither {@code child} nor {@code icon} the trigger is Flutter's overflow glyph, so
 * a menu button written the ordinary way is visible at all.</p>
 */
public class PopupMenuButtonRenderElement extends ButtonRenderElement {

    private final Funcs.VoidFunc0 open = new Funcs.VoidFunc0() {
        @Override
        public void call() {
            // Read through widget() so a rebuilt configuration is honoured rather than the
            // one that happened to be current when this element was created.
            PopupMenus.show(PopupMenuButtonRenderElement.this, button());
        }
    };

    public PopupMenuButtonRenderElement(PopupMenuButton<?> widget) {
        super(widget);
    }

    private PopupMenuButton<?> button() {
        return (PopupMenuButton<?>) widget();
    }

    @Override
    protected Widget contentWidget() {
        return button().effectiveTrigger();
    }

    @Override
    protected Funcs.VoidFunc0 onPressed() {
        // A null handler would also DISABLE the button, which is what we want when the Dart
        // says the menu is disabled.
        return button().isEnabled() ? open : null;
    }
}
