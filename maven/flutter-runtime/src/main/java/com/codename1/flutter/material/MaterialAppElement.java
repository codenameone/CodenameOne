package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.ui.Form;

import java.util.Map;

/**
 * Element for {@link MaterialApp}: installs the app's EFFECTIVE theme (per
 * themeMode/darkTheme) into the CN1 UIManager <b>before</b> the subtree
 * mounts, so every component created below picks the themed Flutter* styles
 * up; on every widget update the effective theme is recomputed and, when it
 * changed (a themeMode/theme/darkTheme switch across rebuilds), the overlay
 * is re-installed, the Form's styles are refreshed and the whole element
 * subtree re-applies its programmatic styling
 * ({@link Element#themeChanged()}).
 */
public class MaterialAppElement extends StatelessElement {

    /** The prop table last installed, for change detection. */
    private Map<String, Object> installedProps;

    public MaterialAppElement(MaterialApp widget) {
        super(widget);
    }

    private MaterialApp app() {
        return (MaterialApp) widget();
    }

    @Override
    public void mount(Element parent, int slot) {
        // Install before super.mount: the children inflate (and create their
        // CN1 components) during the first build inside super.mount.
        RenderHost h = parent != null ? parent.host() : host();
        installEffectiveTheme(app().effectiveTheme(), h);
        super.mount(parent, slot);
    }

    @Override
    public void update(Widget newWidget) {
        ThemeData eff = ((MaterialApp) newWidget).effectiveTheme();
        boolean changed = !ThemeDataAdapter.themeProps(eff).equals(installedProps);
        if (changed) {
            installEffectiveTheme(eff, host());
        }
        super.update(newWidget);
        if (changed) {
            // Reused widget instances skip Element.update, so force every
            // render element to re-apply its (theme-derived) programmatic
            // styling and re-measure.
            themeChanged();
            if (host() != null) {
                host().revalidate();
            }
        }
    }

    private void installEffectiveTheme(ThemeData eff, RenderHost h) {
        installedProps = ThemeDataAdapter.themeProps(eff);
        ThemeDataAdapter.install(eff);
        Form f = h == null ? null : h.form();
        if (f != null) {
            // Re-derive the existing components' UIID styles from the new
            // overlay, then style the Form itself per-instance.
            try {
                f.refreshTheme();
            } catch (Throwable ignore) {
                // headless
            }
            ThemeDataAdapter.applyToForm(f, eff);
        }
    }
}
