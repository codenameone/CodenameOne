package com.codename1.tools.skindesigner;

import com.codename1.components.OnOffSwitch;
import com.codename1.components.SpanLabel;
import com.codename1.components.ToastBar;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.io.Properties;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.system.Lifecycle;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.UITimer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.zipme.ZipEntry;
import net.sf.zipme.ZipOutputStream;

/**
 * Wizard-style skin designer.
 *
 * Step 0 — pick a device from {@link DeviceDatabase}.
 * Step 1 — pick a starting source (shape preset, uploaded image, or blank).
 * Step 2 — editor with live preview and a sidebar (Shape / Cutouts / Info).
 * Step 3 — done summary; the .skin file is written and a JS-port download
 *           dialog is triggered.
 */
public class SkinDesigner extends Lifecycle {
    private static final String PREF_STEP = "wiz.step";
    private static final String PREF_DEVICE = "wiz.deviceId";
    private static final String PREF_SOURCE = "wiz.source";
    private static final String PREF_HAS_IMAGE = "wiz.hasImage";
    private static final String IMAGE_STORAGE = "wiz.image.png";

    private static final int STEP_DEVICE = 0;
    private static final int STEP_SOURCE = 1;
    private static final int STEP_EDIT = 2;
    private static final int STEP_DONE = 3;

    private boolean websiteDarkMode;
    private Form form;
    private int step;
    private DeviceDatabase.Device device;
    private String source;
    private SkinModel skin = new SkinModel();
    private Image bodyImage;
    private Container bodyHolder;
    private Container header;
    private Container statusbar;
    private Label statusName;
    private Label statusSpec;
    private Container stepperRow;
    private Container wizardNav;

    /** Containers that should swap their UIID to {@code <uiid>Hover} while
     *  the cursor is over them. Empty on touch-only platforms (no hover). */
    private final List<Container> hoverCards = new ArrayList<>();
    private Container hoveredCard;

    @Override
    public void runApp() {
        CN.setProperty("platformHint.javascript.beforeUnloadMessage", null);
        websiteDarkMode = readThemeFromUrl();
        Display.getInstance().setDarkMode(websiteDarkMode);

        // Subclass so we can intercept hover events at the form level. CN1
        // doesn't have a CSS :hover state, and overriding pointerHover on a
        // child is unreliable when an overlay Button sits on top (the
        // overlay swallows the hover dispatch). Walking a registry on each
        // hover event lets us toggle the wrapper's UIID precisely once when
        // crossing card boundaries.
        form = new Form("Skin Designer", new BorderLayout()) {
            @Override
            public void pointerHover(int[] x, int[] y) {
                super.pointerHover(x, y);
                if (x != null && x.length > 0 && y != null && y.length > 0) {
                    updateHoverState(x[0], y[0]);
                }
            }
        };
        form.setUIID("SkinDesignerForm");
        form.setTitle("");
        // The Form's default Toolbar otherwise sits above our custom topbar
        // showing an empty title strip — hide it so the topbar is the only
        // header.
        form.getToolbar().setHidden(true);

        loadState();

        header = buildHeader();
        bodyHolder = new Container(new BorderLayout());
        bodyHolder.setUIID("SkinDesignerBody");
        statusbar = buildStatusbar();

        form.add(BorderLayout.NORTH, header);
        form.add(BorderLayout.CENTER, bodyHolder);
        form.add(BorderLayout.SOUTH, statusbar);

        renderStep();
        form.show();

        // Theme drift poll. Was 900ms, but each tick crosses the JS-port
        // bridge to read window.location.href; over an idle session it
        // accumulated enough JS-side work to lock the browser tab. 5 s
        // gives the user a near-immediate response without polling on
        // every animation frame.
        UITimer.timer(5000, true, form, () -> {
            boolean dark = readThemeFromUrl();
            if (dark != websiteDarkMode) {
                websiteDarkMode = dark;
                Display.getInstance().setDarkMode(dark);
                applyDarkRecursive(form);
                form.refreshTheme();
            }
        });
    }

    // ====================================================================
    //  State persistence
    // ====================================================================

    private void loadState() {
        step = Preferences.get(PREF_STEP, STEP_DEVICE);
        String id = Preferences.get(PREF_DEVICE, null);
        device = DeviceDatabase.findById(id);
        source = Preferences.get(PREF_SOURCE, null);
        skin.load();
        if (device != null && skin.name == null) {
            skin.resetForDevice(device);
        }
        if (Preferences.get(PREF_HAS_IMAGE, false) && Storage.getInstance().exists(IMAGE_STORAGE)) {
            try (InputStream is = Storage.getInstance().createInputStream(IMAGE_STORAGE)) {
                bodyImage = Image.createImage(is);
            } catch (IOException err) {
                Log.e(err);
            }
        }
        if (step >= STEP_SOURCE && device == null) {
            step = STEP_DEVICE;
        }
        if (step >= STEP_EDIT && source == null) {
            step = STEP_SOURCE;
        }
    }

    private void saveState() {
        Preferences.set(PREF_STEP, step);
        Preferences.set(PREF_DEVICE, device == null ? null : device.id);
        Preferences.set(PREF_SOURCE, source);
        Preferences.set(PREF_HAS_IMAGE, bodyImage != null);
        skin.save();
    }

    private void restart() {
        step = STEP_DEVICE;
        device = null;
        source = null;
        skin = new SkinModel();
        bodyImage = null;
        lastSkinBytes = null;
        lastSkinFile = null;
        Storage.getInstance().deleteStorageFile(IMAGE_STORAGE);
        Preferences.delete(PREF_STEP);
        Preferences.delete(PREF_DEVICE);
        Preferences.delete(PREF_SOURCE);
        Preferences.delete(PREF_HAS_IMAGE);
        SkinModel.clearPersisted();
        renderStep();
    }

    // ====================================================================
    //  Header / statusbar
    // ====================================================================

    private Container buildHeader() {
        Label logo = new Label();
        logo.setUIID("SkinDesignerBrandLogo");
        applyMaterialIcon(logo, FontImage.MATERIAL_PHONE_IPHONE);
        Label title = new Label("Skin Designer");
        title.setUIID("SkinDesignerBrandTitle");
        Container brand = BoxLayout.encloseX(logo, title);
        brand.setUIID("SkinDesignerBrand");

        stepperRow = new Container(new BoxLayout(BoxLayout.X_AXIS));
        stepperRow.setUIID("SkinDesignerStepper");
        rebuildStepper();

        wizardNav = new Container(new FlowLayout(Component.RIGHT));
        wizardNav.setUIID("SkinDesignerWizardNav");

        Container topbar = new Container(new BorderLayout());
        topbar.setUIID("SkinDesignerTopbar");
        topbar.add(BorderLayout.WEST, brand);
        topbar.add(BorderLayout.CENTER, FlowLayout.encloseCenter(stepperRow));
        topbar.add(BorderLayout.EAST, wizardNav);
        return topbar;
    }

    private void rebuildStepper() {
        if (stepperRow == null) {
            return;
        }
        stepperRow.removeAll();
        String[] labels = { "Device", "Start from", "Editor", "Save" };
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) {
                stepperRow.add(buildStepperSep());
            }
            String suffix = (i == step) ? "Active" : (i < step ? "Done" : "Pending");
            // Force a square size so cn1-pill-border draws the badge as a true
            // circle, with the digit centered by the Label's own alignment.
            Label badge = new Label(String.valueOf(i + 1));
            badge.setUIID("SkinDesignerStepperNum" + suffix);
            int diameter = CN.convertToPixels(4.6f);
            badge.setPreferredSize(new com.codename1.ui.geom.Dimension(diameter, diameter));
            if (i < step) {
                badge.setText("");
                applyMaterialIcon(badge, FontImage.MATERIAL_CHECK,
                        2.6f);
            }
            Label text = new Label(labels[i]);
            text.setUIID("SkinDesignerStepperLabel" + suffix);
            Container item = BoxLayout.encloseX(badge, text);
            item.setUIID("SkinDesignerStepperItem" + suffix);
            stepperRow.add(item);
        }
        stepperRow.revalidate();
    }

    /** Em-dash between stepper items - far simpler than a colored block,
     *  and BoxLayout can't grow a single character into a thick bar.
     *  Uses a unicode escape because Java sources are ASCII-only. */
    private Component buildStepperSep() {
        Label sep = new Label("\u2014");
        sep.setUIID("SkinDesignerStepperSep");
        return sep;
    }

    private void rebuildWizardNav() {
        if (wizardNav == null) {
            return;
        }
        wizardNav.removeAll();
        if (step > STEP_DEVICE && step < STEP_DONE) {
            Button reset = new Button("Start over");
            reset.setUIID("SkinDesignerGhostButton");
            reset.addActionListener(e -> restart());
            wizardNav.add(reset);
        }
        wizardNav.revalidate();
    }

    private Container buildStatusbar() {
        statusName = new Label("Ready");
        statusName.setUIID("SkinDesignerStatusName");
        statusSpec = new Label("");
        statusSpec.setUIID("SkinDesignerStatusSpec");
        Container bar = new Container(new BorderLayout());
        bar.setUIID("SkinDesignerStatusbar");
        bar.add(BorderLayout.WEST, statusName);
        bar.add(BorderLayout.EAST, statusSpec);
        return bar;
    }

    private void updateStatusbar() {
        if (device == null) {
            statusName.setText("Ready");
            statusSpec.setText("");
        } else {
            statusName.setText(skin.name + ".skin");
            statusSpec.setText(device.resolutionW + " × " + device.resolutionH
                    + " · " + device.ppi + " ppi · Saved locally");
        }
        statusName.getParent().revalidate();
    }

    // ====================================================================
    //  Step rendering
    // ====================================================================

    private void renderStep() {
        if (skin == null) {
            skin = new SkinModel();
        }
        if (device != null && (skin.name == null || skin.name.isEmpty())) {
            skin.resetForDevice(device);
        }
        clearHoverState();
        bodyHolder.removeAll();
        switch (step) {
            case STEP_DEVICE:
                bodyHolder.add(BorderLayout.CENTER, buildDeviceStep());
                break;
            case STEP_SOURCE:
                bodyHolder.add(BorderLayout.CENTER, buildSourceStep());
                break;
            case STEP_EDIT:
                bodyHolder.add(BorderLayout.CENTER, buildEditorStep());
                break;
            case STEP_DONE:
                bodyHolder.add(BorderLayout.CENTER, buildDoneStep());
                break;
        }
        rebuildStepper();
        rebuildWizardNav();
        updateStatusbar();
        applyDarkRecursive(form);
        bodyHolder.revalidate();
        saveState();
    }

    private void goToStep(int s) {
        step = Math.max(STEP_DEVICE, Math.min(STEP_DONE, s));
        renderStep();
    }

    // ====================================================================
    //  Step 0 — pick device
    // ====================================================================

    /** Cap on cards rendered at once. The full DB has 5000+ entries; rendering
     *  them all on the EDT freezes the UI for several seconds. Users narrow
     *  via search/filter to find what's missing. Kept low so even slow
     *  platforms stay responsive on a filter switch. */
    private static final int DEVICE_GRID_LIMIT = 60;

    private Label deviceCountLabel;
    private UITimer searchDebounce;

    private Container buildDeviceStep() {
        Container root = new Container(new BorderLayout());
        root.setUIID("SkinDesignerStepRoot");

        Container heading = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        heading.setUIID("SkinDesignerStepHead");
        Label h1 = new Label("Which device is this skin for?");
        h1.setUIID("SkinDesignerH1");
        // SpanLabel wraps cleanly without inheriting TextArea's default
        // underline/border styling (which was reading as a thick divider).
        SpanLabel sub = new SpanLabel(
                "Pick your device. We'll prefill resolution, PPI, safe-area insets, "
                        + "and fonts so you can focus on the skin shape.");
        sub.setUIID("SkinDesignerSubBlock");
        sub.setTextUIID("SkinDesignerSub");
        heading.add(h1);
        heading.add(sub);

        TextField search = new TextField("", "Search devices…", 24, TextField.ANY);
        search.setUIID("SkinDesignerSearchField");
        // Magnifier glass material icon as the hint icon. Force the icon
        // style's bg transparent so the baked image doesn't carry the
        // search field's solid white block (visible after dark-mode swap).
        Style searchIconStyle = new Style(search.getStyle());
        searchIconStyle.setBgTransparency(0);
        search.setHintIcon(FontImage.createMaterial(
                FontImage.MATERIAL_SEARCH, searchIconStyle, 3.4f));

        String[] filterIds = { "all", "phone", "tablet", "fold" };
        String[] filterLabels = { "All", "Phones", "Tablets", "Foldables" };
        final String[] activeFilter = { "all" };
        Button[] filters = new Button[filterIds.length];
        Container filterRow = new Container(new FlowLayout(Component.CENTER));
        filterRow.setUIID("SkinDesignerFilterRow");

        Container grid = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        grid.setUIID("SkinDesignerDeviceGrid");
        grid.setScrollableY(true);

        deviceCountLabel = new Label("");
        deviceCountLabel.setUIID("SkinDesignerEmptyHint");

        Runnable refresh = () -> rebuildDeviceGrid(grid, search.getText(), activeFilter[0]);

        // Debounce search keystrokes so we don't rebuild 5k cards on each char.
        Runnable debouncedRefresh = () -> {
            if (searchDebounce != null) {
                searchDebounce.cancel();
            }
            searchDebounce = UITimer.timer(180, false, form, refresh);
        };

        for (int i = 0; i < filterIds.length; i++) {
            final String id = filterIds[i];
            Button b = new Button(filterLabels[i]);
            b.setUIID(themedUiid(
                    "all".equals(id) ? "SkinDesignerFilterTagActive" : "SkinDesignerFilterTag",
                    websiteDarkMode));
            b.addActionListener(e -> {
                activeFilter[0] = id;
                // Compose the dark suffix when needed, otherwise the chips
                // would all flash to their light-mode UIID on every click.
                for (int j = 0; j < filters.length; j++) {
                    String base = filterIds[j].equals(id)
                            ? "SkinDesignerFilterTagActive"
                            : "SkinDesignerFilterTag";
                    filters[j].setUIID(themedUiid(base, websiteDarkMode));
                }
                // Repaint the chip change immediately, then defer the (slow)
                // grid rebuild to the next event-loop tick so the press
                // visual lands first.
                filterRow.revalidate();
                CN.callSerially(refresh);
            });
            filters[i] = b;
            filterRow.add(b);
        }

        search.addActionListener(e -> refresh.run());
        search.addDataChangedListener((type, index) -> debouncedRefresh.run());

        Container topInner = BoxLayout.encloseY(heading, FlowLayout.encloseCenter(search), filterRow,
                FlowLayout.encloseCenter(deviceCountLabel));
        topInner.setUIID("SkinDesignerStepHeadInner");

        // Footer with Continue button
        Button cont = new Button("Continue");
        cont.setUIID("SkinDesignerPrimaryButton");
        applyMaterialIcon(cont, FontImage.MATERIAL_CHEVRON_RIGHT);
        cont.setTextPosition(Component.LEFT);
        cont.setEnabled(device != null);
        cont.addActionListener(e -> {
            if (device != null) {
                goToStep(STEP_SOURCE);
            }
        });
        Container footer = FlowLayout.encloseRight(cont);
        footer.setUIID("SkinDesignerFooter");

        rebuildDeviceGrid(grid, "", "all");

        Container scroll = new Container(new BorderLayout());
        scroll.setUIID("SkinDesignerStepScroll");
        scroll.add(BorderLayout.NORTH, topInner);
        scroll.add(BorderLayout.CENTER, grid);

        // Handles used by selectDevice() to re-style cards in place and to
        // toggle Continue without doing a full step rebuild (which would
        // jump the scroll position).
        bodyHolder.putClientProperty("deviceContinue", cont);
        bodyHolder.putClientProperty("deviceGrid", grid);

        root.add(BorderLayout.CENTER, scroll);
        root.add(BorderLayout.SOUTH, footer);
        return root;
    }

    private void rebuildDeviceGrid(Container grid, String query, String filter) {
        grid.removeAll();

        // First pass: collect matching devices (DB is already sorted year-desc).
        List<DeviceDatabase.Device> matched = new ArrayList<>();
        int totalMatched = 0;
        for (DeviceDatabase.Device d : DeviceDatabase.all()) {
            if (!d.matchesFormFilter(filter)) continue;
            if (!d.matchesQuery(query)) continue;
            totalMatched++;
            if (matched.size() < DEVICE_GRID_LIMIT) {
                matched.add(d);
            }
        }

        if (deviceCountLabel != null) {
            if (totalMatched == 0) {
                deviceCountLabel.setText("No devices match");
            } else if (totalMatched <= matched.size()) {
                deviceCountLabel.setText(totalMatched + " device" + (totalMatched == 1 ? "" : "s"));
            } else {
                deviceCountLabel.setText("Showing " + matched.size() + " of " + totalMatched
                        + " — type to narrow");
            }
        }

        if (matched.isEmpty()) {
            grid.revalidate();
            return;
        }

        // Group by brand, preserving order (already year-desc within DB).
        Map<String, List<DeviceDatabase.Device>> grouped = new LinkedHashMap<>();
        for (DeviceDatabase.Device d : matched) {
            List<DeviceDatabase.Device> bucket = grouped.get(d.brand);
            if (bucket == null) {
                bucket = new ArrayList<>();
                grouped.put(d.brand, bucket);
            }
            bucket.add(d);
        }

        int columns = Math.max(1, gridColumns());
        for (Map.Entry<String, List<DeviceDatabase.Device>> entry : grouped.entrySet()) {
            Label brand = new Label(entry.getKey() + " · " + entry.getValue().size());
            brand.setUIID("SkinDesignerGroupLabel");
            grid.add(brand);
            Container row = new Container(new GridLayout(1, columns));
            row.setUIID("SkinDesignerCardRow");
            int col = 0;
            for (DeviceDatabase.Device d : entry.getValue()) {
                if (col >= columns) {
                    grid.add(row);
                    row = new Container(new GridLayout(1, columns));
                    row.setUIID("SkinDesignerCardRow");
                    col = 0;
                }
                row.add(buildDeviceCard(d));
                col++;
            }
            while (col < columns) {
                row.add(new Label(" "));
                col++;
            }
            grid.add(row);
        }
        // Filter rebuilds happen outside renderStep, so applyDarkRecursive
        // wasn't running on the freshly-built cards — leaving them in
        // light-mode UIIDs even when the rest of the form was dark.
        applyDarkRecursive(grid);
        grid.revalidate();
    }

    private int gridColumns() {
        int w = Display.getInstance().getDisplayWidth();
        int cardW = CN.convertToPixels(50);
        return Math.max(1, w / cardW);
    }

    private Container buildDeviceCard(DeviceDatabase.Device d) {
        Container card = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        boolean selected = device != null && device.id.equals(d.id);
        card.setUIID(selected ? "SkinDesignerDeviceCardSelected" : "SkinDesignerDeviceCard");

        Label osMark = new Label();
        osMark.setUIID("SkinDesignerOsMark");
        // Use the brand glyphs (apple/android) rather than the silhouette
        // phone icons, which read as identical at this size.
        applyMaterialIcon(osMark, "ios".equals(d.platformName)
                ? FontImage.MATERIAL_APPLE
                : FontImage.MATERIAL_ANDROID);
        Label name = new Label(d.name);
        name.setUIID("SkinDesignerDeviceName");
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.WEST, osMark);
        top.add(BorderLayout.CENTER, name);
        Label check = new Label();
        check.setUIID("SkinDesignerDeviceCheck");
        applyMaterialIcon(check, FontImage.MATERIAL_CHECK);
        // Always add the slot — toggle visibility on selection — so the layout
        // doesn't shift between selected/unselected states.
        check.setVisible(selected);
        top.add(BorderLayout.EAST, check);
        card.add(top);

        Label spec = new Label(d.resolutionW + "×" + d.resolutionH
                + "  ·  " + d.ppi + "ppi  ·  " + d.screenSize + "\"");
        spec.setUIID("SkinDesignerDeviceSpec");
        card.add(spec);

        Container wrapper = makeClickable(card, e -> selectDevice(d));
        // Keep a back-pointer so we can re-style without rebuilding the grid
        // (which would lose the user's scroll position).
        wrapper.putClientProperty("deviceId", d.id);
        wrapper.putClientProperty("card", card);
        wrapper.putClientProperty("check", check);
        registerHover(wrapper);
        return wrapper;
    }

    /**
     * Selecting a device used to call {@link #renderStep()}, which rebuilt the
     * entire body and lost the user's scroll position — that produced the
     * "jumping" the user reported. Now we just toggle the previously- and
     * newly-selected cards' UIIDs in place and refresh the Continue button.
     */
    private void selectDevice(DeviceDatabase.Device d) {
        device = d;
        skin.resetForDevice(d);
        Button cont = (Button) bodyHolder.getClientProperty("deviceContinue");
        if (cont != null) {
            cont.setEnabled(true);
        }
        Container grid = (Container) bodyHolder.getClientProperty("deviceGrid");
        if (grid != null) {
            applySelectionToGrid(grid, d.id);
        }
        updateStatusbar();
        saveState();
    }

    private void applySelectionToGrid(Container container, String selectedId) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            Component c = container.getComponentAt(i);
            Object id = c.getClientProperty("deviceId");
            if (id instanceof String) {
                Component check = (Component) c.getClientProperty("check");
                boolean sel = id.equals(selectedId);
                // makeClickable moved the visible UIID to the wrapper (`c`),
                // so toggle there. Compose the dark suffix when needed and
                // update baseUIID so hover swaps onto the correct variant.
                String baseLight = sel ? "SkinDesignerDeviceCardSelected" : "SkinDesignerDeviceCard";
                String base = websiteDarkMode ? baseLight + "Dark" : baseLight;
                c.putClientProperty("baseUIID", base);
                String visible = (c == hoveredCard) ? hoverVariantOf(base) : base;
                if (!visible.equals(c.getUIID())) {
                    c.setUIID(visible);
                }
                if (check != null) {
                    check.setVisible(sel);
                }
            } else if (c instanceof Container) {
                applySelectionToGrid((Container) c, selectedId);
            }
        }
        container.repaint();
    }

    /**
     * Wraps a card's content in a {@link LayeredLayout} container with a
     * transparent {@link Button} overlay on top. The overlay is a real
     * Button so it participates in CN1's scroll-vs-tap detection inside
     * scrollable parents, where {@code addPointerReleasedListener} on a
     * plain Container was getting swallowed by the scroll layer.
     *
     * The overlay also picks up Button's standard pressed/disabled visual
     * states for free.
     */
    private static Container makeClickable(Container content, com.codename1.ui.events.ActionListener listener) {
        // Preserve the content's UIID so the wrapper looks like the card.
        String uiid = content.getUIID();
        Container layered = new Container(new LayeredLayout());
        layered.setUIID(uiid);
        // Reset content's UIID so its background/border don't double up on
        // top of the wrapper.
        content.setUIID("Container");
        layered.add(content);

        Button overlay = new Button("");
        overlay.setUIID("SkinDesignerCardOverlay");
        overlay.addActionListener(listener);
        layered.add(overlay);
        return layered;
    }

    /**
     * Registers a clickable card so the form's pointerHover override toggles
     * its UIID to its hover variant while the cursor is over it. We resolve
     * the base UIID lazily on first hover so we capture the
     * already-dark-mode-applied UIID rather than the build-time light one
     * (registerHover runs before applyDarkRecursive).
     */
    private void registerHover(Container card) {
        hoverCards.add(card);
    }

    /**
     * Called from the subclassed Form's pointerHover. Walks {@link #hoverCards}
     * once per hover event, swaps UIIDs on whichever card the cursor entered
     * or left.
     */
    private void updateHoverState(int x, int y) {
        if (hoverCards.isEmpty()) {
            return;
        }
        Container target = null;
        for (int i = 0; i < hoverCards.size(); i++) {
            Container c = hoverCards.get(i);
            if (c.getParent() != null && c.contains(x, y)) {
                target = c;
                break;
            }
        }
        if (target == hoveredCard) {
            return;
        }
        if (hoveredCard != null) {
            String base = (String) hoveredCard.getClientProperty("baseUIID");
            if (base != null) {
                hoveredCard.setUIID(base);
                hoveredCard.repaint();
            }
        }
        hoveredCard = target;
        if (hoveredCard != null) {
            // Capture base lazily — whatever UIID the card has right now is
            // already dark-mode-adjusted by applyDarkRecursive.
            String base = (String) hoveredCard.getClientProperty("baseUIID");
            if (base == null) {
                base = hoveredCard.getUIID();
                hoveredCard.putClientProperty("baseUIID", base);
            }
            hoveredCard.setUIID(hoverVariantOf(base));
            hoveredCard.repaint();
        }
    }

    /**
     * Light:  SkinDesignerSourceCard      -> SkinDesignerSourceCardHover
     * Dark:   SkinDesignerSourceCardDark  -> SkinDesignerSourceCardHoverDark
     * Selected light: ...CardSelected     -> ...CardSelectedHover
     * Selected dark:  ...CardSelectedDark -> ...CardSelectedHoverDark
     */
    private static String hoverVariantOf(String base) {
        if (base.endsWith("Dark")) {
            return base.substring(0, base.length() - 4) + "HoverDark";
        }
        return base + "Hover";
    }

    /** Drop hover registrations whenever we rebuild a step so we don't leak
     *  stale references to detached containers. */
    private void clearHoverState() {
        hoverCards.clear();
        hoveredCard = null;
    }

    // ====================================================================
    //  Step 1 — pick source
    // ====================================================================

    private Container buildSourceStep() {
        Container root = new Container(new BorderLayout());
        root.setUIID("SkinDesignerStepRoot");

        Label h1 = new Label("How would you like to start?");
        h1.setUIID("SkinDesignerH1");
        // Render "Build a skin for <bold device name>." with the device name bold.
        Label subPrefix = new Label("Build a skin for ");
        subPrefix.setUIID("SkinDesignerSub");
        Label subDevice = new Label(device.name);
        subDevice.setUIID("SkinDesignerSubBold");
        Label subSuffix = new Label(".");
        subSuffix.setUIID("SkinDesignerSub");
        Container subRow = new Container(new FlowLayout(Component.CENTER));
        subRow.add(subPrefix);
        subRow.add(subDevice);
        subRow.add(subSuffix);
        Container heading = BoxLayout.encloseY(h1, subRow);
        heading.setUIID("SkinDesignerStepHead");

        Container cards = new Container(new GridLayout(1, 3));
        cards.setUIID("SkinDesignerSourceRow");
        cards.add(buildSourceCard("Pick a shape",
                "Start from a common phone silhouette and tweak dimensions and cutouts from there.",
                FontImage.MATERIAL_PHONE_IPHONE,
                SkinModel.SOURCE_SHAPE));
        cards.add(buildSourceCard("Upload an image",
                "Use a render or photo of the device. Position the screen rectangle and mark cutouts on top.",
                FontImage.MATERIAL_IMAGE,
                SkinModel.SOURCE_IMAGE));
        cards.add(buildSourceCard("Blank rectangle",
                "Plain outline with the screen filling the whole skin. Useful when you don't need any cutouts.",
                FontImage.MATERIAL_CROP_DIN,
                SkinModel.SOURCE_BLANK));

        Container body = BoxLayout.encloseY(heading, cards);
        body.setScrollableY(true);
        body.setUIID("SkinDesignerStepBody");

        Button back = new Button("Back");
        back.setUIID("SkinDesignerSecondaryButton");
        applyMaterialIcon(back, FontImage.MATERIAL_CHEVRON_LEFT);
        back.addActionListener(e -> goToStep(STEP_DEVICE));
        Container footer = FlowLayout.encloseIn(back);
        footer.setUIID("SkinDesignerFooter");

        root.add(BorderLayout.CENTER, body);
        root.add(BorderLayout.SOUTH, footer);
        return root;
    }

    private Container buildSourceCard(String title, String desc, char icon, String sourceId) {
        Container card = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        card.setUIID("SkinDesignerSourceCard");

        Label illustration = new Label();
        illustration.setUIID("SkinDesignerSourceIll");
        applyMaterialIcon(illustration, icon, 8.0f);
        card.add(illustration);

        Label h3 = new Label(title);
        h3.setUIID("SkinDesignerH3");
        card.add(h3);

        SpanLabel p = new SpanLabel(desc);
        p.setUIID("SkinDesignerSourcePBlock");
        p.setTextUIID("SkinDesignerSourceP");
        card.add(p);

        Container wrapper = makeClickable(card, e -> {
            source = sourceId;
            if (SkinModel.SOURCE_BLANK.equals(sourceId)) {
                skin.cutouts.clear();
                skin.bezel = 0;
                skin.cornerR = 4;
                skin.homeIndicator = false;
            } else if (SkinModel.SOURCE_IMAGE.equals(sourceId)) {
                if (bodyImage == null) {
                    pickImage(() -> goToStep(STEP_EDIT));
                    return;
                }
            }
            goToStep(STEP_EDIT);
        });
        registerHover(wrapper);
        return wrapper;
    }

    // ====================================================================
    //  Step 2 — editor
    // ====================================================================

    private DevicePreview livePreview;
    private Container cutoutListHolder;
    private int selectedCutout = -1;

    private Container buildEditorStep() {
        Container root = new Container(new BorderLayout());
        root.setUIID("SkinDesignerStepRoot");

        livePreview = new DevicePreview(false);
        livePreview.setSkin(skin);
        if (SkinModel.SOURCE_IMAGE.equals(source) && bodyImage != null) {
            livePreview.setBodyImage(bodyImage);
        }

        Container stage = new Container(new LayeredLayout());
        stage.setUIID("SkinDesignerStage");
        Container previewWrap = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        previewWrap.add(livePreview);
        stage.add(previewWrap);

        Container chips = new Container(new BoxLayout(BoxLayout.X_AXIS));
        chips.setUIID("SkinDesignerStageChips");
        chips.add(infoChip("Screen", device.resolutionW + "×" + device.resolutionH));
        chips.add(infoChip("Cutouts", String.valueOf(skin.cutouts.size())));
        Container chipsLayer = new Container(new FlowLayout(Component.LEFT, Component.TOP));
        chipsLayer.add(chips);
        stage.add(chipsLayer);

        Container sidebar = buildSidebar();

        // Use a 65/35 split via a container with computed widths
        Container split = new Container(new BorderLayout());
        split.add(BorderLayout.CENTER, stage);
        split.add(BorderLayout.EAST, sidebar);
        sidebar.setPreferredW(CN.convertToPixels(60));

        root.add(BorderLayout.CENTER, split);
        return root;
    }

    private Container infoChip(String label, String value) {
        Label k = new Label(label);
        k.setUIID("SkinDesignerChipKey");
        Label v = new Label(value);
        v.setUIID("SkinDesignerChipValue");
        Container chip = BoxLayout.encloseX(k, v);
        chip.setUIID("SkinDesignerInfoChip");
        return chip;
    }

    private final String[] sidebarTabIds = { "shape", "cutouts", "info" };
    private String activeSidebarTab = "shape";
    private Container sidebarBody;

    private Container buildSidebar() {
        Container sidebar = new Container(new BorderLayout());
        sidebar.setUIID("SkinDesignerSidebar");

        Container tabs = new Container(new GridLayout(1, sidebarTabIds.length));
        tabs.setUIID("SkinDesignerSidebarTabs");
        Button[] tabBtns = new Button[sidebarTabIds.length];
        String[] tabLabels = { "Shape", "Cutouts", "Info" };
        for (int i = 0; i < sidebarTabIds.length; i++) {
            final String id = sidebarTabIds[i];
            Button t = new Button(tabLabels[i]);
            t.setUIID(id.equals(activeSidebarTab) ? "SkinDesignerSidebarTabActive" : "SkinDesignerSidebarTab");
            t.addActionListener(e -> {
                activeSidebarTab = id;
                for (int j = 0; j < tabBtns.length; j++) {
                    tabBtns[j].setUIID(sidebarTabIds[j].equals(id)
                            ? "SkinDesignerSidebarTabActive"
                            : "SkinDesignerSidebarTab");
                }
                rebuildSidebarBody();
            });
            tabBtns[i] = t;
            tabs.add(t);
        }

        sidebarBody = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        sidebarBody.setUIID("SkinDesignerSidebarBody");
        sidebarBody.setScrollableY(true);
        rebuildSidebarBody();

        Button back = new Button("Back");
        back.setUIID("SkinDesignerSecondaryButton");
        applyMaterialIcon(back, FontImage.MATERIAL_CHEVRON_LEFT);
        back.addActionListener(e -> goToStep(STEP_SOURCE));

        Button finish = new Button("Finish");
        finish.setUIID("SkinDesignerPrimaryButton");
        applyMaterialIcon(finish, FontImage.MATERIAL_CHEVRON_RIGHT);
        finish.setTextPosition(Component.LEFT);
        finish.addActionListener(e -> {
            // Build + save + trigger the download from this direct click
            // handler so the browser sees a user-gesture chain. If we wait
            // for buildDoneStep() to do it, the browser may have lost the
            // gesture context by the time Display.execute fires (especially
            // after the heavy skin-image generation).
            saveSkinFromUserGesture();
            goToStep(STEP_DONE);
        });

        Container foot = new Container(new BorderLayout());
        foot.setUIID("SkinDesignerSidebarFoot");
        foot.add(BorderLayout.WEST, back);
        foot.add(BorderLayout.EAST, finish);

        sidebar.add(BorderLayout.NORTH, tabs);
        sidebar.add(BorderLayout.CENTER, sidebarBody);
        sidebar.add(BorderLayout.SOUTH, foot);
        return sidebar;
    }

    private void rebuildSidebarBody() {
        sidebarBody.removeAll();
        if ("shape".equals(activeSidebarTab)) {
            buildShapeTab(sidebarBody);
        } else if ("cutouts".equals(activeSidebarTab)) {
            buildCutoutsTab(sidebarBody);
        } else {
            buildInfoTab(sidebarBody);
        }
        sidebarBody.revalidate();
        applyDarkRecursive(sidebarBody);
    }

    private void buildShapeTab(Container parent) {
        if (SkinModel.SOURCE_SHAPE.equals(source)) {
            parent.add(sectionLabel("PRESET"));
            parent.add(buildPresetGrid());
        } else if (SkinModel.SOURCE_IMAGE.equals(source)) {
            Label help = new Label("Upload a device image. The screen rectangle and cutouts will be positioned on top.");
            help.setUIID("SkinDesignerHelpBlock");
            parent.add(help);
            Button upload = new Button(bodyImage != null ? "Replace image" : "Upload image");
            upload.setUIID("SkinDesignerSecondaryButton");
            applyMaterialIcon(upload, FontImage.MATERIAL_FILE_UPLOAD);
            upload.addActionListener(e -> pickImage(() -> {
                livePreview.setBodyImage(bodyImage);
                rebuildSidebarBody();
            }));
            parent.add(upload);
        }

        parent.add(sectionLabel("DIMENSIONS"));
        parent.add(numericPair("Corner radius", "px", skin.cornerR, v -> {
            skin.cornerR = Math.max(0, v);
            livePreview.repaint();
            saveState();
        }, "Bezel", "px", skin.bezel, v -> {
            skin.bezel = Math.max(0, v);
            livePreview.repaint();
            saveState();
        }));

        parent.add(boolPair("Home indicator", skin.homeIndicator, v -> {
            skin.homeIndicator = v;
            livePreview.repaint();
            saveState();
        }));
    }

    private Container buildPresetGrid() {
        // Mirrors SHAPE_PRESETS in Editor.jsx
        String[][] presets = {
                { "rr", "Rounded rect" },
                { "notch", "Notch" },
                { "island", "Dynamic Island" },
                { "hole", "Punch-hole" },
                { "holeCorner", "Corner hole" },
                { "classic", "Classic (home)" },
        };
        Container row = new Container(new GridLayout(2, 3));
        row.setUIID("SkinDesignerPresetGrid");
        for (String[] preset : presets) {
            row.add(buildPresetTile(preset[0], preset[1]));
        }
        return row;
    }

    private Container buildPresetTile(String id, String label) {
        Container tile = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        tile.setUIID(id.equals(skin.presetId) ? "SkinDesignerPresetSelected" : "SkinDesignerPreset");
        PresetIcon icon = new PresetIcon(id);
        icon.setUIID("SkinDesignerPresetIcon");
        tile.add(icon);
        Label lbl = new Label(label);
        lbl.setUIID("SkinDesignerPresetLabel");
        tile.add(lbl);
        return makeClickable(tile, e -> {
            applyPreset(id);
            rebuildSidebarBody();
            livePreview.setSkin(skin);
            livePreview.repaint();
            saveState();
        });
    }

    private void applyPreset(String id) {
        skin.presetId = id;
        skin.cutouts = new ArrayList<>();
        switch (id) {
            case "rr":
                skin.cornerR = 40; skin.bezel = 40; skin.homeIndicator = true;
                break;
            case "notch":
                skin.cornerR = 44; skin.bezel = 28; skin.homeIndicator = true;
                skin.cutouts.add(new SkinModel.Cutout(SkinModel.CUTOUT_NOTCH, 180, 30, 0, 0, "Notch"));
                break;
            case "island":
                skin.cornerR = 48; skin.bezel = 22; skin.homeIndicator = true;
                skin.cutouts.add(new SkinModel.Cutout(SkinModel.CUTOUT_ISLAND, 120, 35, 0, 14, "Dynamic Island"));
                break;
            case "hole":
                skin.cornerR = 36; skin.bezel = 24; skin.homeIndicator = true;
                skin.cutouts.add(new SkinModel.Cutout(SkinModel.CUTOUT_HOLE, 28, 28, 0, 20, "Camera"));
                break;
            case "holeCorner":
                skin.cornerR = 32; skin.bezel = 22; skin.homeIndicator = true;
                skin.cutouts.add(new SkinModel.Cutout(SkinModel.CUTOUT_HOLE, 26, 26, -100, 18, "Camera"));
                break;
            case "classic":
                skin.cornerR = 20; skin.bezel = 64; skin.homeIndicator = false;
                break;
        }
    }

    private void buildCutoutsTab(Container parent) {
        Label help = new Label("Cutouts (notch, island, camera hole) hide rendering and ignore touch input. Positions are relative to the top-center of the screen.");
        help.setUIID("SkinDesignerHelpBlock");
        parent.add(help);

        parent.add(sectionLabel("CUTOUTS (" + skin.cutouts.size() + ")"));
        cutoutListHolder = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        rebuildCutoutList();
        parent.add(cutoutListHolder);

        Container addRow = new Container(new GridLayout(1, 3));
        addRow.add(addCutoutBtn("Notch", SkinModel.CUTOUT_NOTCH));
        addRow.add(addCutoutBtn("Island", SkinModel.CUTOUT_ISLAND));
        addRow.add(addCutoutBtn("Hole", SkinModel.CUTOUT_HOLE));
        parent.add(addRow);
    }

    private void rebuildCutoutList() {
        cutoutListHolder.removeAll();
        if (skin.cutouts.isEmpty()) {
            Label none = new Label("None. Add one below.");
            none.setUIID("SkinDesignerEmptyHint");
            cutoutListHolder.add(none);
        }
        for (int i = 0; i < skin.cutouts.size(); i++) {
            cutoutListHolder.add(buildCutoutRow(i));
        }
        if (selectedCutout >= 0 && selectedCutout < skin.cutouts.size()) {
            cutoutListHolder.add(buildCutoutEditor(selectedCutout));
        }
        cutoutListHolder.revalidate();
        applyDarkRecursive(cutoutListHolder);
    }

    private Container buildCutoutRow(int idx) {
        SkinModel.Cutout c = skin.cutouts.get(idx);
        boolean sel = idx == selectedCutout;
        Container row = new Container(new BorderLayout());
        row.setUIID(sel ? "SkinDesignerCutoutRowSelected" : "SkinDesignerCutoutRow");

        Label sw = new Label(" ");
        sw.setUIID("SkinDesignerCutoutSwatch");
        Label name = new Label(c.name);
        name.setUIID("SkinDesignerCutoutName");
        Label type = new Label(c.type);
        type.setUIID("SkinDesignerCutoutType");

        // Selectable left half (swatch + name + type chip) — clickable as one unit
        Container selectArea = new Container(new BorderLayout());
        selectArea.setUIID("Container");
        selectArea.add(BorderLayout.WEST, BoxLayout.encloseX(sw, name));
        selectArea.add(BorderLayout.EAST, type);
        // makeClickable returns a LayeredLayout WRAPPER that contains
        // selectArea + an overlay Button. We must add THAT wrapper to the
        // row, not selectArea itself — re-parenting selectArea would orphan
        // the overlay and the row would no longer respond to taps. (This
        // was the source of the exception the user saw on the Cutouts tab.)
        Container clickable = makeClickable(selectArea, e -> {
            selectedCutout = (selectedCutout == idx) ? -1 : idx;
            rebuildCutoutList();
        });

        // The X button stays an independent Button so it can fire on its own
        Button rm = new Button();
        rm.setUIID("SkinDesignerIconButton");
        applyMaterialIcon(rm, FontImage.MATERIAL_CLOSE);
        rm.addActionListener(e -> {
            skin.cutouts.remove(idx);
            if (selectedCutout == idx) selectedCutout = -1;
            else if (selectedCutout > idx) selectedCutout--;
            saveState();
            rebuildSidebarBody();
            livePreview.repaint();
        });

        row.add(BorderLayout.CENTER, clickable);
        row.add(BorderLayout.EAST, rm);
        return row;
    }

    private Container buildCutoutEditor(int idx) {
        SkinModel.Cutout c = skin.cutouts.get(idx);
        Container editor = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        editor.setUIID("SkinDesignerCutoutEditor");
        editor.add(numericPair("Width", "px", c.w, v -> { c.w = Math.max(1, v); livePreview.repaint(); saveState(); },
                                "Height", "px", c.h, v -> { c.h = Math.max(1, v); livePreview.repaint(); saveState(); }));
        editor.add(numericPair("Offset X", "px", c.x, v -> { c.x = v; livePreview.repaint(); saveState(); },
                                "Offset Y", "px", c.y, v -> { c.y = v; livePreview.repaint(); saveState(); }));
        return editor;
    }

    private Button addCutoutBtn(String label, String type) {
        Button b = new Button(label);
        b.setUIID("SkinDesignerSecondaryButton");
        applyMaterialIcon(b, FontImage.MATERIAL_ADD);
        b.addActionListener(e -> {
            SkinModel.Cutout nc;
            switch (type) {
                case SkinModel.CUTOUT_NOTCH:
                    nc = new SkinModel.Cutout(type, 180, 30, 0, 0, "Notch"); break;
                case SkinModel.CUTOUT_ISLAND:
                    nc = new SkinModel.Cutout(type, 120, 35, 0, 14, "Dynamic Island"); break;
                default:
                    nc = new SkinModel.Cutout(SkinModel.CUTOUT_HOLE, 28, 28, 0, 20, "Camera"); break;
            }
            skin.cutouts.add(nc);
            selectedCutout = skin.cutouts.size() - 1;
            saveState();
            rebuildSidebarBody();
            livePreview.repaint();
        });
        return b;
    }

    private void buildInfoTab(Container parent) {
        parent.add(sectionLabel("SKIN"));
        TextField nameField = new TextField(skin.name, "Name", 32, TextField.ANY);
        nameField.setUIID("SkinDesignerField");
        nameField.addDataChangedListener((type, idx) -> {
            skin.name = nameField.getText();
            updateStatusbar();
            saveState();
        });
        parent.add(fieldRow("Name", nameField));

        parent.add(sectionLabel("DEVICE (from library)"));
        parent.add(numericPair("Width", "px", device.resolutionW, null,
                "Height", "px", device.resolutionH, null));
        // CN1 wants pixel ratio as pixels per millimeter — derive from PPI.
        int ppmm100 = (int) Math.round((device.ppi / 25.4) * 100);
        parent.add(numericPair("PPI", "", device.ppi, null,
                "Px/mm × 100", "", ppmm100, null));

        parent.add(sectionLabel("SAFE AREA"));
        parent.add(numericPair("Top", "px", skin.safeTop, v -> { skin.safeTop = Math.max(0, v); saveState(); },
                "Bottom", "px", skin.safeBottom, v -> { skin.safeBottom = Math.max(0, v); saveState(); }));
    }

    private Label sectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.setUIID("SkinDesignerSectionLabel");
        return lbl;
    }

    private Container fieldRow(String labelText, Component field) {
        Label l = new Label(labelText);
        l.setUIID("SkinDesignerFieldLabel");
        Container c = BoxLayout.encloseY(l, field);
        c.setUIID("SkinDesignerFieldRow");
        return c;
    }

    private Container boolPair(String label, boolean value, BooleanSetter setter) {
        Label l = new Label(label);
        l.setUIID("SkinDesignerFieldLabel");
        OnOffSwitch sw = new OnOffSwitch();
        sw.setUIID("SkinDesignerField");
        sw.setValue(value);
        if (setter != null) {
            sw.addActionListener(e -> setter.set(sw.isValue()));
        }
        Container c = new Container(new BorderLayout());
        c.setUIID("SkinDesignerFieldRow");
        c.add(BorderLayout.CENTER, l);
        c.add(BorderLayout.EAST, sw);
        return c;
    }

    private Container numericPair(String label1, String unit1, int value1, IntSetter setter1,
                                  String label2, String unit2, int value2, IntSetter setter2) {
        Container row = new Container(new GridLayout(1, 2));
        row.setUIID("SkinDesignerFieldGrid");
        row.add(numericField(label1, unit1, value1, setter1));
        row.add(numericField(label2, unit2, value2, setter2));
        return row;
    }

    private Container numericField(String label, String unit, int value, IntSetter setter) {
        Label l = new Label(unit == null || unit.isEmpty() ? label : label + " (" + unit + ")");
        l.setUIID("SkinDesignerFieldLabel");
        TextField tf = new TextField(String.valueOf(value), label, 6, TextField.NUMERIC);
        tf.setUIID(setter == null ? "SkinDesignerFieldReadonly" : "SkinDesignerField");
        tf.setEditable(setter != null);
        if (setter != null) {
            tf.addDataChangedListener((type, idx) -> {
                String t = tf.getText();
                int v;
                try {
                    v = Integer.parseInt(t.length() == 0 ? "0" : t);
                } catch (NumberFormatException nfe) {
                    return;
                }
                setter.set(v);
            });
        }
        Container c = BoxLayout.encloseY(l, tf);
        c.setUIID("SkinDesignerFieldRow");
        return c;
    }

    interface IntSetter { void set(int v); }
    interface BooleanSetter { void set(boolean v); }

    // ====================================================================
    //  Step 3 — done summary + save
    // ====================================================================

    /** Holds the most-recently-generated .skin bytes + filename so the
     *  Save button on the done step can re-trigger the download. */
    private byte[] lastSkinBytes;
    private String lastSkinFile;

    /**
     * Generates the .skin bytes and persists them to FileSystemStorage so
     * the done step's Download button can fire {@link Display#execute(String)}
     * on the cached file instantly (no slow regeneration inside the click
     * handler). Does NOT itself call execute() — the Finish click handler
     * runs createSkinBytes() which can take seconds, and by the time we'd
     * call execute() the browser's user-gesture window is gone and the
     * download is silently blocked. The done step's primary "Download skin"
     * Button is the reliable trigger.
     */
    private void saveSkinFromUserGesture() {
        byte[] data = createSkinBytes();
        if (data == null) {
            // ToastBar already shown by createSkinBytes() on failure.
            return;
        }
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String outPath = fs.getAppHomePath() + sanitize(skin.name) + ".skin";
        try (OutputStream os = fs.openOutputStream(outPath)) {
            os.write(data);
            lastSkinBytes = data;
            lastSkinFile = outPath;
        } catch (IOException err) {
            Log.e(err);
            ToastBar.showErrorMessage("Error saving skin: " + err);
        }
    }

    private Container buildDoneStep() {
        boolean savedOk = lastSkinBytes != null && lastSkinFile != null;

        Container root = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        root.setUIID("SkinDesignerDoneRoot");

        // A subtler check: smaller, finer-stroked icon. The lime ring around
        // it lives on the SkinDesignerDoneCheck UIID's background.
        Label check = new Label();
        check.setUIID("SkinDesignerDoneCheck");
        applyMaterialIcon(check, FontImage.MATERIAL_CHECK, 5.0f);
        Container checkWrap = FlowLayout.encloseCenter(check);
        root.add(checkWrap);

        Label h1 = new Label(savedOk ? "Skin saved" : "Skin ready");
        h1.setUIID("SkinDesignerH1");
        root.add(FlowLayout.encloseCenter(h1));

        SpanLabel msg = new SpanLabel(savedOk
                ? "Your skin file has been generated. Click \"Download skin\" "
                        + "below to save it (your browser may have blocked the "
                        + "auto-download)."
                : "Click \"Download skin\" to generate and save your .skin file.");
        msg.setUIID("SkinDesignerSubBlock");
        msg.setTextUIID("SkinDesignerSub");
        root.add(msg);

        Container summary = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        summary.setUIID("SkinDesignerSummary");
        summary.add(summaryRow("Name", skin.name));
        summary.add(summaryRow("Device", device.name));
        summary.add(summaryRow("Resolution", device.resolutionW + " × " + device.resolutionH));
        summary.add(summaryRow("Cutouts", String.valueOf(skin.cutouts.size())));
        summary.add(summaryRow("Source", source));
        root.add(FlowLayout.encloseCenter(summary));

        // Big primary "Download skin" button — the only reliably user-gesture
        // path on browsers that blocked the auto-trigger from Finish.
        Button download = new Button("Download skin");
        download.setUIID("SkinDesignerPrimaryButton");
        applyMaterialIcon(download, FontImage.MATERIAL_FILE_DOWNLOAD);
        download.addActionListener(e -> {
            // If we already have generated bytes, just re-fire the download
            // (also from a direct gesture). Otherwise generate now.
            if (lastSkinFile != null) {
                Display.getInstance().execute(lastSkinFile);
            } else {
                saveSkinFromUserGesture();
            }
        });

        Button back = new Button("Back to editor");
        back.setUIID("SkinDesignerSecondaryButton");
        applyMaterialIcon(back, FontImage.MATERIAL_CHEVRON_LEFT);
        back.addActionListener(e -> goToStep(STEP_EDIT));

        Button restartBtn = new Button("Make another skin");
        restartBtn.setUIID("SkinDesignerSecondaryButton");
        applyMaterialIcon(restartBtn, FontImage.MATERIAL_REFRESH);
        restartBtn.addActionListener(e -> restart());

        Container actions = new Container(new FlowLayout(Component.CENTER));
        actions.add(back);
        actions.add(download);
        actions.add(restartBtn);
        root.add(actions);

        Container scroll = new Container(new BorderLayout());
        scroll.setScrollableY(true);
        scroll.add(BorderLayout.CENTER, root);
        return scroll;
    }

    private Container summaryRow(String k, String v) {
        Label kl = new Label(k);
        kl.setUIID("SkinDesignerSummaryKey");
        Label vl = new Label(v);
        vl.setUIID("SkinDesignerSummaryValue");
        Container row = new Container(new BorderLayout());
        row.setUIID("SkinDesignerSummaryRow");
        row.add(BorderLayout.WEST, kl);
        row.add(BorderLayout.EAST, vl);
        return row;
    }

    // ====================================================================
    //  Image upload
    // ====================================================================

    private void pickImage(Runnable onDone) {
        Display.getInstance().openGallery((ee) -> {
            if (ee != null && ee.getSource() != null) {
                try {
                    String fileName = (String) ee.getSource();
                    bodyImage = Image.createImage(fileName);
                    Util.copy(FileSystemStorage.getInstance().openInputStream(fileName),
                            Storage.getInstance().createOutputStream(IMAGE_STORAGE));
                    saveState();
                } catch (IOException err) {
                    Log.e(err);
                    ToastBar.showErrorMessage("Error loading image: " + err);
                }
            }
            if (onDone != null) onDone.run();
        }, Display.GALLERY_IMAGE);
    }

    // ====================================================================
    //  Skin file generation
    // ====================================================================

    private byte[] createSkinBytes() {
        try {
            Image portrait = generatePortraitImage();
            Image landscape = rotate90(portrait);
            Image overlayPortrait = generateOverlay(portrait.getWidth(), portrait.getHeight(),
                    skinBezelInPx(portrait.getWidth(), portrait.getHeight()));
            Image overlayLandscape = rotate90(overlayPortrait);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                writeEntry(zos, "skin.png", imageBytes(portrait));
                writeEntry(zos, "skin_l.png", imageBytes(landscape));
                writeEntry(zos, "skin_map.png", imageBytes(overlayPortrait));
                writeEntry(zos, "skin_map_l.png", imageBytes(overlayLandscape));

                String themeFile = pickNativeThemeFile(device);
                InputStream is = Display.getInstance().getResourceAsStream(getClass(), "/" + themeFile);
                if (is != null) {
                    zos.putNextEntry(new ZipEntry(themeFile));
                    Util.copyNoClose(is, zos, 8192);
                }

                Properties props = buildProperties(portrait.getWidth(), portrait.getHeight());
                zos.putNextEntry(new ZipEntry("skin.properties"));
                props.store(zos, "Created by the Codename One skin designer");
            }
            return bos.toByteArray();
        } catch (IOException err) {
            Log.e(err);
            ToastBar.showErrorMessage("Error generating skin: " + err);
            return null;
        }
    }

    private void writeEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
    }

    private byte[] imageBytes(Image img) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ImageIO.getImageIO().save(img, bo, ImageIO.FORMAT_PNG, 1);
        return bo.toByteArray();
    }

    private int skinBezelInPx(int totalW, int totalH) {
        // Use the SAME formula as generatePortraitImage so safe-area
        // coordinates align with where the screen actually starts in the
        // generated skin image. Previously we mixed two different scaling
        // strategies — buildProperties wrote safePortraitY against
        // bezelPx_a but the image was generated with bezelPx_b — leaving
        // the safe area off by ~10px on high-res devices.
        return Math.round(skin.bezel * ((float) device.resolutionW / DevicePreview.VB_W));
    }

    private int skinCornerInPx(int totalW) {
        return Math.round(((float) skin.cornerR / DevicePreview.VB_W) * totalW);
    }

    /**
     * Produces a portrait skin image whose dimensions are
     * {@code device.resolution + 2 * bezelPx}. The screen rectangle (an area
     * with alpha=0) starts at (bezelPx, bezelPx) and spans the full device
     * resolution.
     */
    /**
     * How tall the top frame extension needs to be (in physical pixels) to
     * fit notch cutouts above the screen rect. Notches are physical
     * hardware cutouts (iPhone X / 11 / 12 / 13 style) and live in the
     * device frame.
     *
     * Islands and punch-holes are NOT counted here — they're software/in-
     * display features (iPhone 14 Pro+ Dynamic Island, Android camera
     * holes), so they're drawn floating inside the screen rect with the
     * iOS status bar / safe area pushing content below them.
     */
    private int computeTopCutoutPx(float scale) {
        int maxExtentVB = 0;
        for (SkinModel.Cutout c : skin.cutouts) {
            if (!SkinModel.CUTOUT_NOTCH.equals(c.type)) {
                continue;
            }
            if (c.h > maxExtentVB) {
                maxExtentVB = c.h;
            }
        }
        return Math.round(maxExtentVB * scale);
    }

    private Image generatePortraitImage() {
        if (SkinModel.SOURCE_IMAGE.equals(source) && bodyImage != null) {
            return generateImageBased();
        }
        float framePxScale = ((float) device.resolutionW) / DevicePreview.VB_W;
        int bezelPx = Math.round(skin.bezel * framePxScale);
        int cornerPx = Math.round(skin.cornerR * framePxScale);
        int screenCornerPx = Math.max(0, cornerPx - Math.round(8 * framePxScale));
        int topCutoutPx = computeTopCutoutPx(framePxScale);
        int totalW = device.resolutionW + bezelPx * 2;
        int totalH = device.resolutionH + bezelPx * 2 + topCutoutPx;
        int screenY = bezelPx + topCutoutPx;

        Image base = Image.createImage(totalW, totalH, 0xff121822);
        Graphics g = base.getGraphics();
        g.setAntiAliased(true);
        g.setColor(0x2a2f3a);
        int inset = Math.max(1, totalW / 200);
        g.fillRoundRect(inset, inset, totalW - inset * 2, totalH - inset * 2,
                Math.max(0, (cornerPx - inset) * 2), Math.max(0, (cornerPx - inset) * 2));

        int[] data = base.getRGB();
        applyRoundRectAlphaMask(data, totalW, totalH, 0, 0, totalW, totalH, cornerPx);

        // Carve the screen with rounded corners — the frame material left at
        // the corners gives the visible rounded screen edge.
        carveRoundedScreenRect(data, totalW, totalH, bezelPx, screenY,
                device.resolutionW, device.resolutionH, screenCornerPx);

        // Notches hang from the top frame extension into the gap above the
        // screen rect (physical hardware cutout, iPhone X / 11 / 12 / 13).
        applyTopFrameCutouts(data, totalW, totalH, bezelPx, screenY,
                device.resolutionW, framePxScale);
        // Islands and punch-holes float inside the screen rect — Dynamic
        // Island and Android camera holes are software-reserved space the
        // OS status bar paints around, not physical cutouts.
        applyInScreenCutouts(data, totalW, totalH, bezelPx, screenY,
                device.resolutionW, framePxScale);

        return Image.createImage(data, totalW, totalH);
    }

    private Image generateImageBased() {
        float framePxScale = ((float) device.resolutionW) / DevicePreview.VB_W;
        int bezelPx = Math.round(skin.bezel * framePxScale);
        int cornerPx = Math.round(skin.cornerR * framePxScale);
        int screenCornerPx = Math.max(0, cornerPx - Math.round(8 * framePxScale));
        int topCutoutPx = computeTopCutoutPx(framePxScale);
        int totalW = device.resolutionW + bezelPx * 2;
        int totalH = device.resolutionH + bezelPx * 2 + topCutoutPx;
        int screenY = bezelPx + topCutoutPx;

        Image canvas = Image.createImage(totalW, totalH, 0);
        Graphics g = canvas.getGraphics();
        g.setAntiAliased(true);
        Image scaled = bodyImage.scaledLargerRatio(totalW, totalH);
        int dx = (totalW - scaled.getWidth()) / 2;
        int dy = (totalH - scaled.getHeight()) / 2;
        g.drawImage(scaled, dx, dy);

        int[] data = canvas.getRGB();
        applyRoundRectAlphaMask(data, totalW, totalH, 0, 0, totalW, totalH, cornerPx);
        carveRoundedScreenRect(data, totalW, totalH, bezelPx, screenY,
                device.resolutionW, device.resolutionH, screenCornerPx);
        applyTopFrameCutouts(data, totalW, totalH, bezelPx, screenY,
                device.resolutionW, framePxScale);
        applyInScreenCutouts(data, totalW, totalH, bezelPx, screenY,
                device.resolutionW, framePxScale);
        return Image.createImage(data, totalW, totalH);
    }

    private Image generateOverlay(int totalW, int totalH, int bezelPx) {
        // The screen rect in skin_map.png matches where it lives in skin.png:
        // shifted DOWN by the top-frame cutout extension so the simulator
        // sees the screen starting below the cutout.
        float framePxScale = ((float) device.resolutionW) / DevicePreview.VB_W;
        int topCutoutPx = computeTopCutoutPx(framePxScale);
        int screenY = bezelPx + topCutoutPx;
        Image overlay = Image.createImage(totalW, totalH, 0);
        Graphics g = overlay.getGraphics();
        g.setColor(0x000000);
        g.fillRect(bezelPx, screenY, device.resolutionW, device.resolutionH);
        return overlay;
    }

    /** Carves a rectangular block transparent (alpha=0). Used by the
     *  rounded-corner variant after it decides which pixels lie inside
     *  the rounded shape. */
    @SuppressWarnings("unused")
    private void carveScreenRect(int[] data, int w, int h, int x, int y, int rw, int rh) {
        int x2 = Math.min(w, x + rw);
        int y2 = Math.min(h, y + rh);
        for (int yy = Math.max(0, y); yy < y2; yy++) {
            int row = yy * w;
            for (int xx = Math.max(0, x); xx < x2; xx++) {
                data[row + xx] = 0;
            }
        }
    }

    /** Like {@link #carveScreenRect} but only carves the inside of a
     *  rounded rect — the four corner pixels remain frame material so the
     *  rendered screen looks rounded inside the bezel. */
    private void carveRoundedScreenRect(int[] data, int w, int h,
                                        int x, int y, int rw, int rh, int radius) {
        int r = Math.max(0, Math.min(radius, Math.min(rw, rh) / 2));
        int r2 = r * r;
        int rightInner = x + rw - r - 1;
        int bottomInner = y + rh - r - 1;
        int x2 = Math.min(w, x + rw);
        int y2 = Math.min(h, y + rh);
        for (int yy = Math.max(0, y); yy < y2; yy++) {
            int row = yy * w;
            for (int xx = Math.max(0, x); xx < x2; xx++) {
                if (r == 0) {
                    data[row + xx] = 0;
                    continue;
                }
                int relX = xx - x;
                int relY = yy - y;
                int dx, dy;
                if (relX < r && relY < r) {
                    dx = r - relX; dy = r - relY;
                } else if (xx > rightInner && relY < r) {
                    dx = xx - rightInner; dy = r - relY;
                } else if (relX < r && yy > bottomInner) {
                    dx = r - relX; dy = yy - bottomInner;
                } else if (xx > rightInner && yy > bottomInner) {
                    dx = xx - rightInner; dy = yy - bottomInner;
                } else {
                    data[row + xx] = 0;
                    continue;
                }
                if (dx * dx + dy * dy <= r2) {
                    data[row + xx] = 0;
                }
            }
        }
    }

    /**
     * Renders notch cutouts (physical hardware cutouts) hanging from the
     * top frame extension down to the screen top. Used for iPhone X / 11 /
     * 12 / 13 style devices where the notch occupies a U-shaped chunk of
     * the device frame above the display.
     */
    private void applyTopFrameCutouts(int[] data, int w, int h,
                                      int bezelPx, int screenY, int sw, float scale) {
        int cx = bezelPx + sw / 2;
        for (SkinModel.Cutout c : skin.cutouts) {
            if (!SkinModel.CUTOUT_NOTCH.equals(c.type)) {
                continue;
            }
            int cw = Math.round(c.w * scale);
            int ch = Math.round(c.h * scale);
            int ox = cx + Math.round(c.x * scale);
            int oy = screenY - ch;
            int x0 = ox - cw / 2;
            fillRect(data, w, h, x0, oy, cw, ch);
        }
    }

    /**
     * Renders software cutouts (Dynamic Island, punch-hole cameras) as
     * opaque pills/circles floating *inside* the screen rect. The iOS
     * theme reserves the safe-area top for the status bar, and these
     * shapes appear painted on top of that area in the skin overlay.
     */
    private void applyInScreenCutouts(int[] data, int w, int h,
                                      int bezelPx, int screenY, int sw, float scale) {
        int cx = bezelPx + sw / 2;
        for (SkinModel.Cutout c : skin.cutouts) {
            if (SkinModel.CUTOUT_NOTCH.equals(c.type)) {
                continue;
            }
            int cw = Math.round(c.w * scale);
            int ch = Math.round(c.h * scale);
            int ox = cx + Math.round(c.x * scale);
            int oy = screenY + Math.round(c.y * scale);
            if (SkinModel.CUTOUT_ISLAND.equals(c.type)) {
                int x0 = ox - cw / 2;
                fillRoundedRect(data, w, h, x0, oy, cw, ch, ch / 2);
            } else if (SkinModel.CUTOUT_HOLE.equals(c.type)) {
                int r = cw / 2;
                fillCircle(data, w, h, ox, oy + ch / 2, r);
            }
        }
    }

    private void applyRoundRectAlphaMask(int[] data, int w, int h,
                                         int x, int y, int rw, int rh, int radius) {
        if (radius <= 0) return;
        int r = Math.min(radius, Math.min(rw, rh) / 2);
        int r2 = r * r;
        int[][] corners = {
                { x + r, y + r, -1, -1 },
                { x + rw - r - 1, y + r, +1, -1 },
                { x + r, y + rh - r - 1, -1, +1 },
                { x + rw - r - 1, y + rh - r - 1, +1, +1 },
        };
        for (int[] c : corners) {
            int cx = c[0], cy = c[1], dx = c[2], dy = c[3];
            for (int yy = 0; yy < r; yy++) {
                for (int xx = 0; xx < r; xx++) {
                    int px = cx + dx * xx;
                    int py = cy + dy * yy;
                    if (px < 0 || py < 0 || px >= w || py >= h) continue;
                    int distSq = xx * xx + yy * yy;
                    if (distSq > r2) {
                        data[py * w + px] = 0;
                    }
                }
            }
        }
    }

    // applyCutouts (the old in-screen renderer) replaced by
    // applyTopFrameCutouts which positions cutouts above the screen.

    private void fillRect(int[] data, int w, int h, int x, int y, int rw, int rh) {
        int x2 = Math.min(w, x + rw);
        int y2 = Math.min(h, y + rh);
        for (int yy = Math.max(0, y); yy < y2; yy++) {
            for (int xx = Math.max(0, x); xx < x2; xx++) {
                data[yy * w + xx] = 0xff000000;
            }
        }
    }

    private void fillCircle(int[] data, int w, int h, int cx, int cy, int r) {
        int r2 = r * r;
        for (int yy = Math.max(0, cy - r); yy < Math.min(h, cy + r + 1); yy++) {
            int dy = yy - cy;
            for (int xx = Math.max(0, cx - r); xx < Math.min(w, cx + r + 1); xx++) {
                int dx = xx - cx;
                if (dx * dx + dy * dy <= r2) {
                    data[yy * w + xx] = 0xff000000;
                }
            }
        }
    }

    private void fillRoundedRect(int[] data, int w, int h, int x, int y, int rw, int rh, int r) {
        int x2 = Math.min(w, x + rw);
        int y2 = Math.min(h, y + rh);
        for (int yy = Math.max(0, y); yy < y2; yy++) {
            for (int xx = Math.max(0, x); xx < x2; xx++) {
                int dx = xx - x;
                int dy = yy - y;
                int rrw = rw - 1;
                int rrh = rh - 1;
                int cdx, cdy;
                if (dx < r && dy < r) { cdx = r - dx; cdy = r - dy; }
                else if (dx > rrw - r && dy < r) { cdx = dx - (rrw - r); cdy = r - dy; }
                else if (dx < r && dy > rrh - r) { cdx = r - dx; cdy = dy - (rrh - r); }
                else if (dx > rrw - r && dy > rrh - r) { cdx = dx - (rrw - r); cdy = dy - (rrh - r); }
                else { data[yy * w + xx] = 0xff000000; continue; }
                if (cdx * cdx + cdy * cdy <= r * r) {
                    data[yy * w + xx] = 0xff000000;
                }
            }
        }
    }

    private Image rotate90(Image src) {
        return src.rotate90Degrees(true);
    }

    private Properties buildProperties(int totalW, int totalH) {
        Properties p = new Properties();
        p.put("touch", "true");
        p.put("platformName", device.platformName);
        p.put("tablet", String.valueOf(device.tablet));
        p.put("systemFontFamily", device.systemFont);
        p.put("proportionalFontFamily", device.proportionalFont);
        p.put("monospaceFontFamily", device.monoFont);
        // Don't write smallFontSize/mediumFontSize/largeFontSize. The
        // simulator (JavaSEPort line ~2870) auto-derives correct sizes
        // from pixelMilliRatio when those are absent:
        //     med = round(2.6 * ppmm), sm = 2 * ppmm, la = 3.3 * ppmm
        // Our DeviceDatabase entries store iOS-style point values
        // (12/15/22), and writing those overrides the simulator's
        // physical-pixel computation — on a 460 ppi device that's a
        // sub-millimeter font and the UI text is unreadable.
        // Also write `ppi` so the simulator computes pixelMilliRatio = ppi/25.4
        // (it prefers `ppi` over `pixelRatio`).
        p.put("ppi", String.valueOf(device.ppi));
        // Keep pixelRatio (= pixels per millimeter) as a fallback for older
        // skin loaders that didn't read `ppi`.
        p.put("pixelRatio", String.valueOf(device.ppi / 25.4));
        p.put("overrideNames", overrideNames(device));

        int bezelPx = skinBezelInPx(totalW, totalH);
        // roundScreen=true switches the simulator from the skin_map-driven
        // path (which clips UI to non-skin pixels) to the display-rect path
        // where the skin is painted *over* the rendered UI (JavaSEPort
        // line ~1622). That overlay rendering is what makes Dynamic Island
        // and other in-screen cutouts appear "floating" on top of the
        // status bar / app content rather than carved out of it.
        p.put("roundScreen", "true");
        float framePxScale = ((float) device.resolutionW) / DevicePreview.VB_W;
        int topCutoutPxForDisplay = computeTopCutoutPx(framePxScale);
        p.put("displayX", String.valueOf(bezelPx));
        p.put("displayY", String.valueOf(bezelPx + topCutoutPxForDisplay));
        p.put("displayWidth", String.valueOf(device.resolutionW));
        p.put("displayHeight", String.valueOf(device.resolutionH));
        // Notch cutouts live in the top frame extension above the screen,
        // so they don't eat into safeTop. But islands and punch-holes are
        // drawn floating *inside* the screen and the iOS status bar
        // reserves space for them — extend safeTop to cover their bottom
        // edge so app content lands below.
        int inScreenCutoutBottomVB = 0;
        for (SkinModel.Cutout c : skin.cutouts) {
            if (SkinModel.CUTOUT_NOTCH.equals(c.type)) {
                continue;
            }
            int extentVB = c.y + c.h;
            if (extentVB > inScreenCutoutBottomVB) {
                inScreenCutoutBottomVB = extentVB;
            }
        }
        int effectiveSafeTopVB = Math.max(skin.safeTop, inScreenCutoutBottomVB);
        float vbToPx = (float) device.resolutionW / DevicePreview.VB_W;
        int safeTopPx = Math.round(effectiveSafeTopVB * vbToPx);
        int safeBottomPx = Math.round(skin.safeBottom * vbToPx);

        // Safe area is consumed by Container.snapToSafeAreaInternal, which
        // treats the rect's X/Y as inset margins from the *display* origin
        // (not the skin image). So write 0,Y,W,H in display-relative coords:
        // origin (0,0) is the screen top-left, Y is the top inset, etc.
        int safeW = device.resolutionW;
        int safeH = Math.max(1, device.resolutionH - safeTopPx - safeBottomPx);
        p.put("safePortraitX", "0");
        p.put("safePortraitY", String.valueOf(safeTopPx));
        p.put("safePortraitWidth", String.valueOf(safeW));
        p.put("safePortraitHeight", String.valueOf(safeH));

        // Landscape is portrait rotated 90° clockwise. The display now has
        // width = portraitH and height = portraitW. The portrait top inset
        // becomes the landscape *left* inset; the portrait bottom inset
        // becomes the landscape right inset.
        int landSafeW = Math.max(1, device.resolutionH - safeTopPx - safeBottomPx);
        int landSafeH = device.resolutionW;
        p.put("safeLandscapeX", String.valueOf(safeTopPx));
        p.put("safeLandscapeY", "0");
        p.put("safeLandscapeWidth", String.valueOf(landSafeW));
        p.put("safeLandscapeHeight", String.valueOf(landSafeH));

        // bezelPx is unused now that the safe-area coords are display-relative,
        // but skin generation still needs it elsewhere.
        if (bezelPx < 0) { /* keep var alive */ }
        return p;
    }

    private String overrideNames(DeviceDatabase.Device d) {
        String form = d.tablet ? "tablet" : "phone";
        String os;
        String last;
        if ("ios".equals(d.platformName)) {
            os = "ios";
            last = d.tablet ? "ipad" : "iphone";
        } else if ("and".equals(d.platformName)) {
            os = "android";
            last = d.tablet ? "android-tablet" : "android-phone";
        } else {
            os = "windows";
            last = "desktop";
        }
        return form + "," + os + "," + last;
    }

    private String pickNativeThemeFile(DeviceDatabase.Device d) {
        if ("ios".equals(d.platformName)) return "iOS7Theme.res";
        if ("and".equals(d.platformName)) return "android_holo_light.res";
        return "winTheme.res";
    }

    private static String sanitize(String name) {
        if (name == null || name.isEmpty()) return "skin";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isAsciiAlphanumeric(c) || c == '-' || c == '_') {
                sb.append(c);
            } else if (c == ' ') {
                sb.append('-');
            }
        }
        if (sb.length() == 0) sb.append("skin");
        return sb.toString();
    }

    private static boolean isAsciiAlphanumeric(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    // ====================================================================
    //  Theming helpers (light/dark via URL ?theme=)
    // ====================================================================

    private boolean readThemeFromUrl() {
        String href = CN.getProperty("browser.window.location.href", "");
        String theme = queryParam(href, "theme");
        if ("dark".equalsIgnoreCase(theme)) return true;
        if ("light".equalsIgnoreCase(theme)) return false;
        return Display.getInstance().isDarkMode();
    }

    private String queryParam(String href, String name) {
        if (href == null || href.length() == 0) return null;
        int q = href.indexOf('?');
        if (q < 0 || q == href.length() - 1) return null;
        String query = href.substring(q + 1);
        int hash = query.indexOf('#');
        if (hash >= 0) query = query.substring(0, hash);
        String prefix = name + "=";
        for (String pair : Util.split(query, "&")) {
            if (pair.startsWith(prefix) && pair.length() > prefix.length()) {
                return pair.substring(prefix.length());
            }
        }
        return null;
    }

    private void applyDarkRecursive(Component c) {
        String uiid = c.getUIID();
        if (uiid != null) {
            String themed = themedUiid(uiid, websiteDarkMode);
            if (!uiid.equals(themed)) {
                c.setUIID(themed);
            }
        }
        // Hover-card baseUIID may have been captured under the previous
        // theme; recompute against the now-current theme so the hover
        // swap restores to the right variant.
        Object base = c.getClientProperty("baseUIID");
        if (base instanceof String) {
            String rebased = themedUiid((String) base, websiteDarkMode);
            if (!rebased.equals(base)) {
                c.putClientProperty("baseUIID", rebased);
            }
        }
        // Material icons bake the host's bg color into the image. After we
        // swap UIID light->dark, the icon's background is still the old
        // theme's. Re-bake against the now-current style so the icon's
        // transparent area matches the surrounding button.
        Object iconChar = c.getClientProperty("materialIcon");
        if (iconChar instanceof Character && c instanceof Label) {
            applyMaterialIcon((Label) c, (Character) iconChar);
        }
        if (c instanceof Container) {
            Container container = (Container) c;
            for (int i = 0; i < container.getComponentCount(); i++) {
                applyDarkRecursive(container.getComponentAt(i));
            }
        }
    }

    /**
     * Sets a material icon on the label and remembers the char so
     * {@link #applyDarkRecursive(Component)} can re-bake it after a theme
     * switch. Forces a transparent style background so the icon image
     * never carries a solid colored block (which produced a "white square
     * behind the arrow" in dark mode where the button was light at the
     * time of icon creation).
     */
    private static void applyMaterialIcon(Label l, char icon) {
        l.putClientProperty("materialIcon", icon);
        Style s = new Style(l.getUnselectedStyle());
        s.setBgTransparency(0);
        l.setIcon(FontImage.createMaterial(icon, s, 4f));
    }

    /** Variant with explicit size in mm. */
    private static void applyMaterialIcon(Label l, char icon, float sizeMM) {
        l.putClientProperty("materialIcon", icon);
        Style s = new Style(l.getUnselectedStyle());
        s.setBgTransparency(0);
        l.setIcon(FontImage.createMaterial(icon, s, sizeMM));
    }

    private String themedUiid(String uiid, boolean dark) {
        if (uiid == null || uiid.isEmpty()) return uiid;
        if (!uiid.startsWith("SkinDesigner")) return uiid;
        if (dark) {
            return uiid.endsWith("Dark") ? uiid : uiid + "Dark";
        }
        return uiid.endsWith("Dark") ? uiid.substring(0, uiid.length() - 4) : uiid;
    }

    // ====================================================================
    //  Preset icons
    // ====================================================================

    /** Tiny inline icon for the shape-preset tiles. */
    static final class PresetIcon extends Component {
        private final String id;
        PresetIcon(String id) { this.id = id; }
        @Override
        protected com.codename1.ui.geom.Dimension calcPreferredSize() {
            int s = CN.convertToPixels(8);
            return new com.codename1.ui.geom.Dimension(s, (int) (s * 1.5f));
        }
        @Override
        public void paint(Graphics g) {
            int w = getWidth(), h = getHeight();
            int pad = Math.max(1, w / 12);
            g.setColor(0x7F8AA3);
            int rx = getX() + pad, ry = getY() + pad;
            int rw = w - pad * 2, rh = h - pad * 2;
            int corner = "classic".equals(id) ? rw / 8 : rw / 4;
            g.drawRoundRect(rx, ry, rw, rh, corner * 2, corner * 2);
            g.setColor(0x112247);
            switch (id) {
                case "notch": {
                    int nw = rw / 3, nh = rh / 14;
                    g.fillRoundRect(rx + (rw - nw) / 2, ry + 2, nw, nh, nh, nh);
                    break;
                }
                case "island": {
                    int iw = rw / 3, ih = rh / 18;
                    g.fillRoundRect(rx + (rw - iw) / 2, ry + ih, iw, ih, ih, ih);
                    break;
                }
                case "hole": {
                    int r = rw / 12;
                    g.fillArc(rx + rw / 2 - r, ry + r * 2, r * 2, r * 2, 0, 360);
                    break;
                }
                case "holeCorner": {
                    int r = rw / 14;
                    g.fillArc(rx + r * 2, ry + r * 2, r * 2, r * 2, 0, 360);
                    break;
                }
                case "classic": {
                    int r = rw / 18;
                    g.drawArc(rx + rw / 2 - r, ry + rh - r * 3, r * 2, r * 2, 0, 360);
                    break;
                }
                default:
                    break;
            }
        }
    }

}
