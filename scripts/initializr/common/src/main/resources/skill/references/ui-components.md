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
| `TextArea` | Multi-line input | `setGrowByContent(true)` grows the field to fit its text reliably *while editing* (not just after focus leaves). |
| `TextComponent` | Material-Design–style input — floating label, optional description / error message, optional action icon | The right default for new form inputs. See *TextComponent* below. |
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
| `BrowserComponent` | Embedded WebView | Use sparingly; ParparVM/iOS WebView has caveats. See *BrowserComponent appearance* below. |

**Note on `ComboBox`**: It exists but is **not recommended** in CN1. The dropdown rendering is awkward on touch screens and behaves inconsistently across platforms. Use `Picker` (set `pickerType` to `Display.PICKER_TYPE_STRINGS` for a string-list picker) — it opens a native sheet on iOS, a Material dialog on Android, and a normal popup in the simulator. `ComboBox` is kept only for legacy ports of Swing apps.

### BrowserComponent appearance (light/dark)

On iOS you can pin the WebView's appearance (the `prefers-color-scheme` the page sees and the native form-control rendering) instead of letting it follow the device:

```java
browser.setProperty(BrowserComponent.BROWSER_PROPERTY_INTERFACE_STYLE, "light"); // "light" | "dark" | "auto"
```

`"auto"` (the default) follows the device theme. Honored on iOS WKWebView; other platforms ignore it. Useful when your embedded HTML is only styled for one mode and you don't want the system flipping it.

### Package locations — don't trust autocomplete to find these

A few components live in package paths that don't match where you'd guess from the type name. Importing from the wrong package gives `cannot find symbol` and the IDE will helpfully offer to import the (deprecated or non-existent) sibling.

| Component | Package |
| --- | --- |
| `Label`, `Button`, `TextField`, `TextArea`, `Form`, `Container`, `TextComponent`, `Dialog`, `Tabs`, `CheckBox`, `RadioButton`, `Slider`, `List` | `com.codename1.ui` |
| `SpanLabel`, `SpanButton`, `MultiButton`, `MultiList`, `Switch`, `ScaleImageButton`, `ScaleImageLabel`, `ToastBar`, `InfiniteProgress`, `ImageViewer`, `FloatingActionButton`, `StickyHeaderContainer`, `Accordion` | `com.codename1.components` |
| `Picker` | `com.codename1.ui.spinner` |
| `InfiniteContainer` | `com.codename1.ui` (despite being a "component") |

When in doubt, `find CodenameOne/src -name "<ClassName>.java"` from the framework checkout — much faster than guessing which sub-package is current.

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

## `TextComponent` — the Material-Design input replacement

`com.codename1.ui.TextComponent` wraps a `TextField` (or `TextArea`) with a floating label, an optional description string under the field, an error message, and an optional action icon — the Material Design "outlined / filled text field" paradigm. Prefer it over bare `TextField` for new forms.

```java
import com.codename1.ui.TextComponent;
import com.codename1.ui.TextArea;
import com.codename1.ui.FontImage;

TextComponent email = new TextComponent()
        .label("Email")
        .descriptionMessage("We never share your address");
email.getField().setConstraint(TextArea.EMAILADDR);
email.getField().setSingleLineTextArea(true);

TextComponent search = new TextComponent()
        .label("Search")
        .action(FontImage.MATERIAL_SEARCH)
        .actionClick(e -> performSearch(search.getText()));

if (looksInvalid(email.getText())) {
    email.errorMessage("Enter a valid email address");
}

form.add(BorderLayout.CENTER, BoxLayout.encloseY(email, search));
```

Fluent builder methods: `label(String)`, `descriptionMessage(String)`, `errorMessage(String)` (set to `""` or `null` to clear), `action(char materialIcon)`, `actionClick(ActionListener)`, `actionAsButton(boolean)`, `onTopMode(boolean)`. The `onTopMode(true)` variant moves the label permanently above the field instead of floating into the field on focus; toggle the global default via the `textComponentOnTopBool` theme constant.

`getField()` returns the underlying `TextField` if you need to set keyboard constraints or hook value listeners; `getText()` / `setText(String)` work directly on the wrapper.

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

### `InteractionDialog` — a non-blocking, movable dialog

`com.codename1.components.InteractionDialog` is a dialog that does **not** block the EDT and lets the user keep interacting with the form behind it (toolbars, lists). Use it for floating panels, in-place editors, or a popover that should coexist with the screen.

```java
import com.codename1.components.InteractionDialog;

InteractionDialog dlg = new InteractionDialog("Filters");
dlg.setLayout(BoxLayout.y());
dlg.add(new Switch("Unread only"));
dlg.add(new Button("Apply") {{ addActionListener(e -> dlg.dispose()); }});
// position + size in pixels; show() leaves the rest of the form live
dlg.show(top, left, bottom, right);
```

**Stackable mode** (global opt-in): by default, disposing one `InteractionDialog` runs `removeAll()` on the shared layered pane and wipes any *other* interaction dialog still on screen. If your app shows two at once, enable stackable mode once at startup so siblings survive — later-shown dialogs simply layer on top, and the shared pane is cleared only when the last one closes:

```java
InteractionDialog.setStackable(true);     // app-wide; default is false (back-compatible)
boolean on = InteractionDialog.isStackable();
```

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

## Animation — pick the right tool for the scope

Codename One's animation story is built around the **AnimationManager** owned by each Form, plus a set of high-level idioms layered on top. CSS does not animate anything in CN1. Pick the tool that matches the scope of the change.

### Layout-driven (the most common case): `Form.animateLayout(durationMs)`

Mutate visibility / size / `LayeredLayoutConstraint` / UIID, then call `animateLayout`. CN1 captures the before+after positions and tweens children across the duration.

```java
sidePanel.setHidden(!sidePanel.isHidden());
sidePanel.setVisible(!sidePanel.isVisible());
form.animateLayout(250);
```

Variants: `animateHierarchy(durationMs)` (the *whole subtree* re-flows, useful for deep mutations), `animateLayoutAndWait(durationMs)` (blocks the EDT until the tween finishes — only sensible during scripted demos).

For show/hide animations, panel slide-in, expand/collapse cards, sticky-header-on-scroll morphs — this is the right hammer.

### Container layout replacement: `Container#replace`, `Container#replaceAndWait`

```java
Container parent = card.getParent();
parent.replace(oldChild, newChild,
        CommonTransitions.createFade(200));     // swap with an animated transition
```

`replace(...)` swaps a child for a new one inside the same parent, optionally driving an in-place transition (slide, fade, etc.). It's the right idiom for "the card body morphs into a new layout" without rebuilding the parent. `replaceAndWait` is the blocking version.

### Hide and re-flow without animation: `revalidate()`

```java
cmp.setHidden(true).setVisible(false);
parent.revalidate();                            // instant re-layout, no tween
```

`revalidate()` recomputes the layout and repaints immediately — no animation, no waiting. Use it for instant mutations. Note: **`revalidate()` collides with most animations** running on the same parent — never call it from inside an `animate()` cycle or while `animateLayout` is in flight, or the tween snaps and the result looks broken. If you want both an immediate flush *and* a smooth animation later, call `revalidate()` first, then start the animation on the next cycle via `Display.callSerially`.

### Removing a child with a tween: `unlayout()`-style fade

To remove a component with a fade/slide-out, drop it from the parent, then `animateLayout`:

```java
parent.removeComponent(card);
parent.animateLayout(250);                       // remaining siblings re-flow to fill the gap
```

`animateUnlayout(int duration, int destOpacity, Runnable onComplete)` (on `Container`) explicitly animates a child *out* and runs the callback when the animation is done — useful when you want the removal itself to fade.

### Form-to-form transitions: `setTransitionInAnimator` / `setTransitionOutAnimator`

```java
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.animations.MorphTransition;

// Set on the form being LEFT (most common): controls how the current form animates OUT
// before the next one slides in.
currentForm.setTransitionOutAnimator(CommonTransitions.createSlide(
        CommonTransitions.SLIDE_HORIZONTAL, false, 250));
nextForm.show();

// Less common: dictate how the destination form animates IN, ignoring the source's
// transition-out.
nextForm.setTransitionInAnimator(CommonTransitions.createFade(200));
```

In most CN1 codebases the **default transition** is set globally via theme constants (`formTransitionIn`, `formTransitionOut`, `formTransitionInImage`, `formTransitionOutImage`) — the native theme typically defines a platform-appropriate default (iOS-style horizontal slide on iOS, Material slide on Android). You usually only call `setTransitionOutAnimator` / `setTransitionInAnimator` to **override** that default for a specific navigation step, not to define one from scratch.

`CommonTransitions` exposes slide / fade / cover / uncover / dialog / empty transitions. `MorphTransition.create(durationMs).morph(sourceCmp, targetCmp)` animates a specific source component into a specific destination component across forms — great for "tap a card to expand it into the full screen".

#### `MorphTransition.snapshotMode(boolean)`

Opt into snapshot-mode when the live-paint morph leaks off-viewport children (source inside a scrolling parent) or renders dynamic content (video, `BrowserComponent`):

```java
MorphTransition morph = MorphTransition.create(300).snapshotMode(true).morph("card");
nextForm.setTransitionInAnimator(morph);
nextForm.show();
```

#### Tabs animated indicator and modern pull-to-refresh

`Tabs` has a sliding underline indicator and `addPullToRefresh` has an arc-spinner — both on by default in the modern iOS / Android themes, so apps inherit them with no extra setup. Override via `Tabs.setAnimatedIndicator(boolean)` or the `tabsAnimatedIndicatorBool` / `pullToRefreshModernBool` theme constants when needed.

### Ongoing per-component animation: `Component.animate()` + `registerAnimated`

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

The right hammer for breathing dots, custom progress, gradient drift — anything that needs a per-frame update. The `Form.registerAnimated(Animation)` registration drives the EDT-side animation tick.

### Manual low-level control: `AnimationManager`

`Form.getAnimationManager()` exposes the queue that the higher-level helpers above all funnel through. Useful for chaining animations:

```java
form.getAnimationManager().addAnimation(
    new ComponentAnimation() {
        public boolean isInProgress() { return /* ... */; }
        public void updateState() { /* tick */ }
        public void flush() { /* skip-to-end */ }
    },
    () -> showNextStep()                        // runs after the animation completes
);
```

Reach for `AnimationManager` only when the higher-level helpers don't compose — e.g. you need to sequence three independent tweens and run a callback at the very end.

### Painters draw, they don't animate

A `Painter` is for **drawing** (custom backgrounds, decorations, shapes). It does not run on its own — the renderer calls it only when something else triggers a repaint. Don't try to drive an animation purely from a Painter: pair it with `animate()` (above) so state changes trigger the repaint cycle, *then* the Painter consumes the new state.

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
