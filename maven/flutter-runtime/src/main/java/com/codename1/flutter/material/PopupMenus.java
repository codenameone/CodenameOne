package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.FlutterRootLayout;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.widgets.Column;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BorderLayout;

import dart.core.DartList;
import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * Presents a {@link PopupMenuButton}'s menu and reports the selection — the part of
 * {@code showMenu} the button needs.
 *
 * <p>The entries come from the button's {@code itemBuilder}, which is a Dart callback
 * returning a list of {@link PopupMenuEntry}. Each selectable entry is wrapped in an
 * {@link InkWell} that dismisses the menu and fires {@code onSelected} with that entry's
 * value, so a menu behaves the way the Dart says it does rather than merely appearing.</p>
 *
 * <p>The menu itself is a CN1 dialog holding a Flutter subtree, the same arrangement
 * {@link Dialogs} uses for {@code showDialog} — modeless, so transpiled code continues
 * after the call exactly as it does in Dart.</p>
 */
public final class PopupMenus {

    private static com.codename1.ui.Dialog openMenu;
    private static Element openRoot;

    private PopupMenus() {
    }

    /**
     * Builds and shows {@code button}'s menu. Does nothing when the button is disabled or
     * has no itemBuilder.
     */
    public static void show(BuildContext context, PopupMenuButton<?> button) {
        if (button == null || !button.isEnabled() || button.getItemBuilder() == null) {
            return;
        }
        List<PopupMenuEntry<?>> entries = entriesOf(context, button);
        if (entries.isEmpty()) {
            return;
        }
        MenuWidget menu = new MenuWidget(button, entries);
        if (!Display.isInitialized()) {
            // headless: mount the tree so the bookkeeping is testable, with no dialog
            openRoot = FlutterUI.mount(menu, new RenderHost(), new BuildOwner());
            return;
        }
        dismiss();
        com.codename1.ui.Dialog d = new com.codename1.ui.Dialog(new BorderLayout());
        d.setDisposeWhenPointerOutOfBounds(true);
        Container c = FlutterUI.wrap(menu);
        d.add(BorderLayout.CENTER, c);
        openMenu = d;
        openRoot = ((FlutterRootLayout) c.getLayout()).host().rootElement();
        // Modeless, like showDialog: the caller keeps running, as the Dart expects.
        d.showPacked(BorderLayout.NORTH, false);
    }

    /** Closes the open menu, if any. */
    public static void dismiss() {
        if (openRoot != null) {
            FlutterUI.unmountTree(openRoot);
            openRoot = null;
        }
        if (openMenu != null) {
            openMenu.dispose();
            openMenu = null;
        }
    }

    /** Whether a menu is currently open — test and hot-restart hook. */
    public static boolean isOpen() {
        return openRoot != null;
    }

    /** Runs the itemBuilder and collects whatever entries it produced. */
    @SuppressWarnings("unchecked")
    private static List<PopupMenuEntry<?>> entriesOf(BuildContext context,
            PopupMenuButton<?> button) {
        List<PopupMenuEntry<?>> out = new ArrayList<PopupMenuEntry<?>>();
        Object built;
        try {
            built = button.getItemBuilder().call(context);
        } catch (Throwable t) {
            com.codename1.flutter.FlutterErrorReport.unimplemented("PopupMenuButton",
                    "itemBuilder failed: " + t);
            return out;
        }
        if (built instanceof Iterable) {
            for (Object o : (Iterable<Object>) built) {
                if (o instanceof PopupMenuEntry) {
                    out.add((PopupMenuEntry<?>) o);
                }
            }
        }
        return out;
    }

    /**
     * The menu surface: a Material card holding one row per entry.
     */
    static final class MenuWidget extends StatelessWidget {

        private final PopupMenuButton<?> button;
        private final List<PopupMenuEntry<?>> entries;

        MenuWidget(PopupMenuButton<?> button, List<PopupMenuEntry<?>> entries) {
            this.button = button;
            this.entries = entries;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Widget build(BuildContext context) {
            DartList<Widget> rows = new DartList<Widget>();
            for (int i = 0; i < entries.size(); i++) {
                final PopupMenuEntry<?> entry = entries.get(i);
                if (entry instanceof PopupMenuDivider) {
                    rows.add(new PopupMenuDivider());
                    continue;
                }
                if (!(entry instanceof PopupMenuItem)) {
                    continue;
                }
                final PopupMenuItem item = (PopupMenuItem) entry;
                InkWell row = new InkWell();
                row.child(item.getChild());
                row.onTap(new Funcs.VoidFunc0() {
                    @Override
                    public void call() {
                        select(item);
                    }
                });
                rows.add(row);
            }
            Column col = new Column();
            col.children(rows);
            col.mainAxisSize(com.codename1.flutter.MainAxisSize.min);
            Material surface = new Material();
            surface.child(col);
            return surface;
        }

        /** Dismiss first, then report — the order Flutter's menu route uses. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void select(PopupMenuItem item) {
            dismiss();
            Funcs.VoidFunc0 tap = item.getOnTap();
            if (tap != null) {
                tap.call();
            }
            Funcs.VoidFunc1 onSelected = ((PopupMenuButton) button).getOnSelected();
            if (onSelected != null && item.getValue() != null) {
                onSelected.call(item.getValue());
            }
        }
    }
}
