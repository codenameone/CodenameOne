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
| `Picker` | Native picker (date/time/string list) | Reads as a button, opens a native sheet on mobile |
| `Switch` / `CheckBox` / `RadioButton` | Toggles | RadioButton requires a `ButtonGroup` |
| `Slider` | Range or progress | |
| `ComboBox` | Drop-down selector | |
| `List` / `MultiList` | Scrolling list of items | Prefer `InfiniteContainer` for paged data |
| `Container` | Generic group | Most screens are nested Containers |
| `Tabs` | Tabbed pane | Like Swing JTabbedPane |
| `Accordion` | Collapsible sections | |
| `InfiniteScrollAdapter` | Lazy-loaded pagination | Plug into a Container |
| `InfiniteProgress` | Activity spinner | |
| `Dialog` | Modal popup | `Dialog.show(...)` blocks current EDT pump |
| `Toolbar` | Top bar | Already on every Form |
| `BrowserComponent` | Embedded WebView | Use sparingly; ParparVM/iOS WebView has caveats |

## Toolbar — title, menu, commands

```java
Form f = new Form("Inbox", new BorderLayout());
Toolbar tb = f.getToolbar();
tb.setTitle("Inbox");

// Right-side button(s) in the title bar
tb.addCommandToRightBar("Compose", null, evt -> openComposeForm());

// Hamburger menu items (slide-out on Android, drawer on iOS)
tb.addCommandToSideMenu("Profile", null, evt -> openProfile());
tb.addCommandToSideMenu("Settings", null, evt -> openSettings());

// Bottom tabs (iOS-style)
tb.setHomeSearchEnabled(true);  // adds a search icon and floating search bar
```

Use `tb.addMaterialCommandToRightBar("", FontImage.MATERIAL_SEARCH, e -> ...)` to drop in a Material icon without a glyph font of your own.

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

## Images and icons

```java
// Bundled image (in common/src/main/resources/)
Image img = Image.createImage("/logo.png");

// Material icon as a font glyph (no asset file)
FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_FAVORITE, "Label", 6);
new Label(icon);

// Async load from network
URLImage urlImg = URLImage.createToStorage(
    placeholder, "logo-storage-key", "https://example.com/logo.png",
    URLImage.RESIZE_SCALE);
```

`FontImage` is the recommended way to add icons — it scales to any density and uses Material's font.

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

## Animation

```java
Component target = ...;
target.setX(0);
target.setY(0);
form.animateLayout(300);   // 300ms animated re-layout after you mutated constraints

// Or a specific property animation:
Motion m = Motion.createEaseInOutMotion(0, 100, 250);
m.start();
// poll in callSerially loop and call target.setX(...)
```

`Form.animateLayout(duration)` is the easiest tool — change layout constraints (insets in `LayeredLayout`, visibility, sizes), then call it.

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

## Patterns for the initializr-generated barebones template

The barebones starter is intentionally tiny — typically a `Form` with a centered `Label` and a `Button`. Build outwards from there:

1. Replace the centered label with a `BorderLayout` + `Toolbar` setup.
2. Add screens as separate `Form` classes or factory methods returning `Form`.
3. Style by editing `common/src/main/css/theme.css`. The initializr "Append Custom CSS" panel writes into the same file under a comment marker.

## When to reach for the GUI builder

CN1 has an optional GUI builder (`*.gui` XML) that generates view classes. It's powerful but the Java-first approach above is usually cleaner for new screens. If a project already uses GUI builder XML, edit those files and run `mvn -pl common generate-sources` to regenerate the Java.
