# UI Components and Layouts Reference

Codename One's UI model is "Swing for mobile". You assemble `Component`s into `Container`s, attach a `Layout` to control arrangement, and put the root in a `Form`. The single EDT serializes UI work.

## Core hierarchy

```
Form  extends Container
  └── ContentPane (a Container)
        └── any number of Components/Containers (your screen)
  └── Toolbar (the top bar, side menu, optional tabs)
```

Every Codename One `Form` has a `Toolbar` and a `ContentPane`. The toolbar handles the title, back navigation, side menu, and command buttons. You add screen content to the content pane (via `form.add(...)` which delegates to it).

## Layouts (Component arrangement)

Picking the right layout makes 90% of the work straightforward. Treat layouts like Swing layout managers — set one, then add children with constraints if needed.

| Layout | When to use | Example |
| --- | --- | --- |
| `BoxLayout.y()` / `BoxLayout.x()` | Stack vertically or horizontally. Default for forms. | `Container c = BoxLayout.encloseY(label, field, button);` |
| `BorderLayout` | One CENTER region plus optional NORTH/SOUTH/EAST/WEST. Great for screens with a header + body + footer. | `form.setLayout(new BorderLayout()); form.add(BorderLayout.CENTER, list);` |
| `FlowLayout` | Children flow horizontally, wrap when out of width. | `new Container(new FlowLayout(Component.CENTER));` |
| `GridLayout` | N rows × M columns, all cells the same size. | `new Container(new GridLayout(2, 3));` |
| `TableLayout` | Spreadsheet-like: per-cell width/height/span. Use when GridLayout is too rigid. | `TableLayout tl = new TableLayout(rows, cols);` |
| `LayeredLayout` | Stack components on top of each other with percent-based insets. The "responsive" layout. | See `mobile-adaptability.md`. |
| `GridBagLayout` | Swing-style absolute control. Avoid unless porting. | — |

### Convenience static helpers

`BoxLayout.encloseX(...)`, `BoxLayout.encloseY(...)`, `BorderLayout.center(...)`, `FlowLayout.encloseCenter(...)` create a container and add children in one call:

```java
Container row = BoxLayout.encloseX(new Label("Total"), new SpanLabel("$12.99"));
Container col = BoxLayout.encloseY(new Label("Name"), nameField, saveBtn);
form.add(BorderLayout.CENTER, col);
```

`SpanLabel` is a multi-line, wrap-friendly label — prefer it to `Label` for any string longer than a couple of words.

## Common components

| Component | Purpose | Notes |
| --- | --- | --- |
| `Label` | Single-line text or an image | Set UIID to style via CSS |
| `SpanLabel` | Multi-line wrapped text | Use for descriptions/copy |
| `Button` | Tappable button | `.pressed` UIID variant for press state |
| `TextField` | Single-line input | Use `TextField.setUIID("InitializrField")` to apply CSS |
| `TextArea` | Multi-line input | |
| `Picker` | Native picker (date/time/string/list) | Reads as a button, opens a native sheet on mobile. **Use this instead of `ComboBox`.** |
| `Switch` / `CheckBox` / `RadioButton` | Toggles | RadioButton requires a `ButtonGroup` |
| `Slider` | Range or progress | |
| `List` / `MultiList` | Scrolling list of items | Prefer `InfiniteContainer` for paged data |
| `Container` | Generic group | Most screens are nested Containers. See *Container UIID* caveat below. |
| `Tabs` | Tabbed pane | Like Swing JTabbedPane |
| `Accordion` | Collapsible sections | |
| `InfiniteScrollAdapter` | Lazy-loaded pagination | Plug into a Container |
| `InfiniteProgress` | Activity spinner | |
| `Dialog` | Modal popup | `Dialog.show(...)` blocks current EDT pump |
| `Toolbar` | Top bar | Already on every Form |
| `BrowserComponent` | Embedded WebView | Use sparingly; ParparVM/iOS WebView has caveats |

**Note on `ComboBox`**: It exists but is **not recommended** in CN1. The dropdown rendering is awkward on touch screens and behaves inconsistently across platforms. Use `Picker` (set `pickerType` to `Display.PICKER_TYPE_STRINGS` for a string-list picker) — it opens a native sheet on iOS, a Material dialog on Android, and a normal popup in the simulator. `ComboBox` is kept only for legacy ports of Swing apps.

## Container is structural — don't style its UIID

`Container` is the layout glue between visible components, **not** a styled component itself. The default `Container` UIID must remain transparent with **0 padding / 0 margin / no border**.

- If you need a visible "box" (a card, a banner, a section, a row with a background), give the wrapper its own UIID and style that instead:
  ```java
  Container card = new Container(BoxLayout.y());
  card.setUIID("Card");          // <-- not Container
  ```
- Restyling the base `Container` UIID in `theme.css` causes nested layouts (which are very common in CN1) to compound the styling, producing double padding, doubled backgrounds, and stretched borders.
- If a generated project or a cn1lib has already restyled `Container` away from the defaults, restore it before doing anything else.

The same caveat applies (more mildly) to `ContentPane` — only change it when you specifically want to recolor the entire form background and you understand the cascading impact.

## Sticky headers — `StickyHeaderContainer`

CN1 has `com.codename1.components.StickyHeaderContainer`. It wraps a scrolling content pane and pins one or more header containers to the top: while you scroll, the header stays glued in place and can morph (color shift, height change, fade) using a transition you supply.

```java
import com.codename1.components.StickyHeaderContainer;

Container scrolling = BoxLayout.encloseY(card1, card2, card3, /* ... */);
scrolling.setScrollableY(true);

Container header = BoxLayout.encloseY(new Label("My Profile"));
header.setUIID("StickyHeader");

StickyHeaderContainer sticky = new StickyHeaderContainer(header, scrolling);
form.add(BorderLayout.CENTER, sticky);
```

For animated effects (e.g. fade the header background as the user scrolls), look at `StickyHeaderContainer` overloads that take a transition and at the bundled `StickyHeaderFadeTransitionScreenshotTest` / `StickyHeaderSlideTransitionScreenshotTest` examples in the CN1 source tree.

## Hero images — use the Toolbar background

The common "hero image at the top of a screen, content scrolls underneath, header shrinks as you scroll" pattern is built into the CN1 `Toolbar`. The right path is **not** a manual `LayeredLayout` of background+title — instead, give the Toolbar a background image (or themed UIID) and let it handle the shrink-on-scroll behavior itself.

```css
Toolbar {
    background-image: url('/hero.jpg');
    background-position: center;
    color: #ffffff;
}
Title {
    color: #ffffff;
    font-family: "native:MainBold";
    font-size: 5mm;
}
```

```java
Form f = new Form("Welcome", new BorderLayout());
Toolbar tb = f.getToolbar();
tb.setTitleCentered(true);
// Toolbar already paints the hero image as its background — no custom layered layout needed.

Container body = BoxLayout.encloseY(/* scrollable content */);
body.setScrollableY(true);
f.add(BorderLayout.CENTER, body);
```

If you need the hero to **shrink** as the user scrolls (the classic iOS "large title" effect), pair the Toolbar with `Toolbar.setScrollOffSize(...)` and a `ScrollListener` on `body` — the Toolbar already knows how to interpolate its own background height in response. Avoid hand-rolling this with `LayeredLayout`; it's substantially more code and lacks the platform-native behavior the Toolbar provides.

## Toolbar — title, menu, commands

```java
Form f = new Form("Inbox", new BorderLayout());
Toolbar tb = f.getToolbar();
tb.setTitle("Inbox");

// Right-bar / left-bar commands with Material icons — terse and native-looking.
tb.addMaterialCommandToRightBar("Compose", FontImage.MATERIAL_EDIT, e -> openComposeForm());
tb.addMaterialCommandToRightBar("Search",  FontImage.MATERIAL_SEARCH, e -> openSearch());

// Hamburger side menu (slide-out on Android, drawer on iOS).
tb.addMaterialCommandToSideMenu("Profile",  FontImage.MATERIAL_PERSON,   e -> openProfile());
tb.addMaterialCommandToSideMenu("Settings", FontImage.MATERIAL_SETTINGS, e -> openSettings());

// Built-in search bar
tb.setHomeSearchEnabled(true);
```

For commands without a Material icon (text-only labels or custom images) use `addCommandToRightBar(String, Image, ActionListener)` and friends.

## Form lifecycle and navigation

```java
Form prev = Display.getInstance().getCurrent();
nextForm.setBackCommand("Back", null, evt -> prev.showBack());
// or simply:
nextForm.getToolbar().setBackCommand("Back", evt -> prev.showBack());
nextForm.show();
```

`form.show()` replaces the current form. `form.showBack()` is the same but plays the "back" transition. `Form.previous()` returns the previous form on the navigation stack when `setBackCommand` was used.

## Dialogs

```java
// Yes/No
if (Dialog.show("Delete?", "Are you sure?", "Delete", "Cancel")) {
    delete();
}

// Custom content
Dialog d = new Dialog("Custom");
d.setLayout(BoxLayout.y());
d.add(new Label("Enter a code:"));
d.add(new TextField());
d.add(new Button("OK") {{ addActionListener(e -> d.dispose()); }});
d.show();

// Lightweight toast (non-modal)
ToastBar.showMessage("Saved!", FontImage.MATERIAL_CHECK);
```

For confirmation flows prefer `Dialog.show(...)` overloads — they map to native sheets on iOS. `ToastBar` is the only built-in non-blocking notification.

## Styling: UIIDs over inline styles

A component's **UIID** is its CSS selector. `Button` is the default UIID for `new Button(...)`. Change it to target a CSS rule:

```java
Button cta = new Button("Submit");
cta.setUIID("PrimaryCta");
```

```css
PrimaryCta {
    background-color: #1d4ed8;
    color: #ffffff;
    border-radius: 3mm;
    padding: 2mm 4mm;
}
```

Inline styling via `cta.getAllStyles().setBgColor(0x1d4ed8)` works but is brittle (it bypasses theming). Prefer CSS. See `references/css.md`.

## Margin and padding from Java — mind the units

The `getAllStyles().setMargin(...)` / `setPadding(...)` setters take **device pixels** by default. That's almost never what you want — pixel sizes don't scale with display density. Use the overload that takes a unit:

```java
import com.codename1.ui.plaf.Style;

// WRONG — interprets values as raw device pixels, looks tiny on hi-DPI:
comp.getAllStyles().setMargin(2, 2, 2, 2);

// RIGHT — explicit unit. Style.UNIT_TYPE_DIPS means "mm" in CN1 (density-independent).
comp.getAllStyles().setMarginUnit(Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS,
                                  Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS);
comp.getAllStyles().setMargin(2f, 2f, 2f, 2f);     // 2mm on each side

// Same pattern for padding:
comp.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS,
                                   Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS);
comp.getAllStyles().setPadding(2f, 2f, 4f, 4f);
```

Available units:

| Constant | Meaning |
| --- | --- |
| `Style.UNIT_TYPE_PIXELS` | Raw device pixels (the *bad default*). |
| `Style.UNIT_TYPE_DIPS` | Millimeters (density-independent). Use this. |
| `Style.UNIT_TYPE_SCREEN_PERCENTAGE` | Percentage of the form's smaller dimension. |
| `Style.UNIT_TYPE_VH` / `UNIT_TYPE_VW` | Percentage of screen height/width. |

In CSS you don't think about this — `padding: 2mm 4mm;` is unambiguous. In Java, **always** call `setMarginUnit` / `setPaddingUnit` before `setMargin` / `setPadding` unless you really want device pixels.

## Images and icons

```java
// Bundled image (top-level under common/src/main/resources/ — see java-api-subset.md
// for the flat-namespace constraint).
Image img = Image.createImage("/logo.png");

// Async load from network with a cached, scaled fallback.
URLImage urlImg = URLImage.createToStorage(
    placeholder, "logo-storage-key", "https://example.com/logo.png",
    URLImage.RESIZE_SCALE);
```

### Material icons — prefer the built-in convenience APIs

`FontImage` is the underlying API for icon-as-glyph, but most components expose a one-line wrapper that's much shorter than the raw `FontImage.createMaterial(...)` form.

| Goal | Right call |
| --- | --- |
| Material icon on a `Label` | `Label l = new Label(); l.setMaterialIcon(FontImage.MATERIAL_FAVORITE);` |
| `Label` with text + icon | `Label l = new Label("Favorite"); l.setMaterialIcon(FontImage.MATERIAL_FAVORITE);` |
| `Button` icon-only | `Button b = new Button(FontImage.MATERIAL_DELETE);` |
| `Button` text + icon | `Button b = new Button("Delete", FontImage.MATERIAL_DELETE);` |
| `Toolbar` right-bar command | `f.getToolbar().addMaterialCommandToRightBar("Search", FontImage.MATERIAL_SEARCH, e -> ...);` |
| `Toolbar` side menu command | `f.getToolbar().addMaterialCommandToSideMenu("Profile", FontImage.MATERIAL_PERSON, e -> ...);` |
| Floating action button | `FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD); fab.bindFabToContainer(form.getContentPane()); fab.addActionListener(e -> add());` |

The verbose `new Label(FontImage.createMaterial(FontImage.MATERIAL_FAVORITE, "Label", 6))` form works but you almost never need it — the convenience APIs above take a `char` directly and use the right UIID and sizing for the component.

The full Material icon catalog is exposed as `char` constants on `FontImage` — autocomplete on `FontImage.MATERIAL_` to discover them.

## Lists and infinite scrolling

For large datasets, use `InfiniteContainer` instead of `List`:

```java
InfiniteContainer ic = new InfiniteContainer(20) {
    @Override
    public Component[] fetchComponents(int index, int amount) {
        // Called on a background thread. Return null when no more items.
        List<Item> page = api.fetchPage(index, amount);
        Component[] out = new Component[page.size()];
        for (int i = 0; i < page.size(); i++) {
            out[i] = renderItem(page.get(i));
        }
        return out;
    }
};
form.add(BorderLayout.CENTER, ic);
```

`InfiniteContainer` handles loading spinners, pagination, and recycling — much better than `List` for paged APIs.

## Scrolling — one axis only

CN1's scrolling model is **single-axis per container**. Two-axis scrolling is not supported, and nested scrollables produce broken gestures (the parent and child fight over touch events).

- Always use `setScrollableX(boolean)` or `setScrollableY(boolean)` — **never** `setScrollable(boolean)` (which is a legacy convenience that turns *both* on and will fail at runtime on mobile builds).
- **Never nest a scrollable inside another scrollable.** If you have a scrolling list inside a scrolling form, one of them must be non-scrolling. The usual fix is to make the inner container `setScrollableY(false)` and let the outer (the Form) handle scroll.
- **A `Form` is scrollable Y by default** — unless its content pane uses a `BorderLayout`, in which case auto-scroll is disabled (because `BorderLayout` has a fixed `CENTER` region). If you put a `BoxLayout.y()` form full of items and they exceed the screen, the form will scroll automatically.

```java
// Vertically scrolling list — most common pattern.
Container list = new Container(BoxLayout.y());
list.setScrollableY(true);
list.add(...);
form.add(BorderLayout.CENTER, list);

// Horizontally swipe-able row of cards.
Container row = new Container(BoxLayout.x());
row.setScrollableX(true);
row.setScrollVisible(false);     // hide the scrollbar on swipe
```

If the design truly requires what looks like two-axis scrolling (e.g. a wide table), build it as a horizontally-scrolling outer container whose only child is a vertically-scrolling list of fixed-width rows — i.e. flatten one of the axes into rows of identical layout.

## Floating Action Button + the Form's LayeredPane

For overlays that need to sit on top of the regular content (the canonical example is a circular **+** FAB anchored bottom-right), CN1 gives you two tools:

- **`FloatingActionButton`** — a circular Material-style button with a built-in shadow and a `bindFabToContainer` helper that pins it to the corner of any container.
- **`Form.getLayeredPane()`** — every Form ships with a top-level `LayeredPane` that overlays the entire form. Add components here to stack them above everything else (think tooltips, custom popovers, decorations).

```java
import com.codename1.components.FloatingActionButton;

FloatingActionButton fab = FloatingActionButton.createFAB(FontImage.MATERIAL_ADD);
fab.addActionListener(e -> openNewItem());
fab.bindFabToContainer(form.getContentPane());   // pins it bottom-right inside the body
```

For non-FAB overlays (a heads-up notification banner, an in-app tutorial highlight) use the layered pane directly:

```java
LayeredLayout ll = new LayeredLayout();
form.getLayeredPane().setLayout(ll);

Container banner = BoxLayout.encloseY(new SpanLabel("You're offline. Reconnecting…"));
banner.setUIID("OfflineBanner");
form.getLayeredPane().add(banner);

LayeredLayout.LayeredLayoutConstraint c = ll.createConstraint();
c.setInsets("auto auto 0 0").setReferenceComponentTop(banner, 1f);
ll.addLayoutComponent(c, banner, form.getLayeredPane());
form.revalidate();
```

`LayeredPane` is the replacement for "I need `position: absolute` over the whole screen" CSS thinking.

## Animation — the three right tools

Codename One's animation story is built around layout-driven tweens and transition objects, **not** CSS. The hierarchy of approaches:

### 1. `Form.animateLayout(durationMs)` — for layout-driven animations

Mutate visibility / size / `LayeredLayoutConstraint` / UIID, then call `animateLayout`. CN1 captures the before+after positions and tweens children across the duration.

```java
sidePanel.setHidden(!sidePanel.isHidden());
sidePanel.setVisible(!sidePanel.isVisible());
form.animateLayout(250);
```

Use this for show/hide animations, panel slide-in, expand/collapse cards, sticky-header-on-scroll morphs.

### 2. Form-to-form transitions — for whole-screen navigation

```java
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.MorphTransition;

nextForm.setTransitionInAnimator(
    CommonTransitions.createSlide(CommonTransitions.SLIDE_HORIZONTAL, true, 250));
nextForm.setTransitionOutAnimator(
    CommonTransitions.createFade(200));
nextForm.show();
```

`CommonTransitions` exposes slide / fade / cover / uncover / dialog / empty transitions. `MorphTransition` (`MorphTransition.create(durationMs).morph(from, to)`) is for animating a specific source component into a specific destination component across forms — great for "tap a card to expand it into the full screen".

### 3. `Component.animate()` + `setAnimation(true)` — for ongoing per-component animation

Override `animate()` on a Component, return `true` while the animation should keep firing, and register with the form:

```java
class PulseDot extends Container {
    private final Motion m = Motion.createEaseInOutMotion(0, 255, 600);

    @Override protected void initComponent() {
        super.initComponent();
        m.start();
        getComponentForm().registerAnimated(this);
    }

    @Override public boolean animate() {
        getAllStyles().setBgTransparency(m.getValue());
        if (m.isFinished()) m.start();
        return true;
    }
}
```

This is the right hammer for breathing dots, custom progress, gradient drift — anything that needs a per-frame update. Don't try to do this from a `Painter`: painters don't have a built-in re-paint cycle, so they only repaint when something else triggers it.

`Painter` is for **drawing** (custom backgrounds, decorations); `animate()` is for **state changes that drive painting**.

## Keyboard handling (text input)

`TextField` opens the native keyboard automatically when focused. To customize:

```java
TextField tf = new TextField();
tf.setHint("Email");
tf.setInputMode(TextArea.EMAILADDR);
tf.setConstraint(TextArea.EMAILADDR);
tf.setSingleLineTextArea(true);
```

Use `TextArea.NUMERIC`, `TextArea.PASSWORD`, `TextArea.URL`, etc., for the right keyboard variant on mobile.

## Patterns for the barebones starter

The generated barebones starter is intentionally tiny — typically a `Form` with a centered `Label` and a `Button`. Build outwards from there:

1. Replace the centered label with a `BorderLayout` + `Toolbar` setup.
2. Add screens as separate `Form` classes or factory methods returning `Form`.
3. To recolor the whole app for your brand, override the accent-color rules in `common/src/main/css/theme.css` — see *Modern theme accent colors* in `references/css.md`.

## When to reach for the GUI builder

CN1 has an optional GUI builder (`*.gui` XML) that generates view classes. It's powerful but the Java-first approach above is usually cleaner for new screens. If a project already uses GUI builder XML, edit those files and run `mvn -pl common generate-sources` to regenerate the Java.
