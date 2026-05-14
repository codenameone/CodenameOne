# HTML/CSS → Codename One Cheat Sheet

Designers and web developers think in HTML/CSS idioms — flexbox rows, hero sections, sticky headers, media queries. None of those translate literally, but every one of them has a CN1 idiom. Use this file as a fast lookup.

## HTML elements → CN1 components

| HTML | CN1 component | Notes |
| --- | --- | --- |
| `<div>` | `Container` | Wraps children; pick a layout. |
| `<span>` | `Label` | Single-line. |
| `<p>` / multi-line text | `SpanLabel` | Wraps automatically. |
| `<h1>`–`<h3>` | `Label` with a UIID like `H1`, `H2`, styled in CSS | |
| `<button>` | `Button` | |
| `<a href="...">` | `Button` styled as a link + `Display.execute(url)` | No native hyperlink component. |
| `<input type="text">` | `TextField` | |
| `<input type="password">` | `TextField` + `setConstraint(TextArea.PASSWORD)` | |
| `<input type="email">` | `TextField` + `setConstraint(TextArea.EMAILADDR)` | |
| `<textarea>` | `TextArea` | |
| `<select>` | `ComboBox` or `Picker` | `Picker` opens a native sheet on mobile. |
| `<input type="checkbox">` | `CheckBox` or `Switch` | |
| `<input type="radio">` | `RadioButton` in a `ButtonGroup` | |
| `<input type="range">` | `Slider` | |
| `<img>` | `Label` with image, or `URLImage` for remote | |
| `<ul>` / `<ol>` | `Container` with `BoxLayout.y()` of children | |
| `<form>` | `Container` + a `Submit` `Button` | No native form element. |
| `<dialog>` | `Dialog` | |
| `<iframe>` | `BrowserComponent` | |
| `<header>` / `<nav>` | `Toolbar` (already on every Form) | |
| `<footer>` | `BorderLayout.SOUTH` container | |

## CSS layout → CN1 layout

### Flexbox row → `BoxLayout.x()` or `FlowLayout`

```css
.row { display: flex; flex-direction: row; gap: 8px; }
```

```java
Container row = BoxLayout.encloseX(a, b, c);
row.getAllStyles().setMargin(0, 0, 2, 2);  // simulate "gap" via per-child margins
```

For `gap`, set padding/margin on the children or wrap each in a `Container` with margins.

### Flexbox column → `BoxLayout.y()`

```css
.col { display: flex; flex-direction: column; }
```

```java
Container col = BoxLayout.encloseY(header, body, footer);
```

### Flex `space-between` (header left + actions right)

```html
<div class="bar"><h2>Title</h2><button>Action</button></div>
```

```java
Container bar = new Container(new BorderLayout());
bar.add(BorderLayout.WEST, new Label("Title").setUIID("H2"));
bar.add(BorderLayout.EAST, new Button("Action"));
```

`BorderLayout` with WEST/EAST is the CN1 idiom for "stuff on each end".

### CSS Grid → `GridLayout` or `TableLayout`

```css
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
```

```java
Container grid = new Container(new GridLayout(rows, 3));
for (Item i : items) grid.add(renderCard(i));
```

If cells need different widths or row spans, use `TableLayout`.

### Hero section / full-screen banner

```css
.hero { height: 100vh; background: url(bg.jpg) center/cover; }
```

```java
Form f = new Form("", new LayeredLayout());
Label bg = new Label(); bg.setIcon(Image.createImage("/hero-bg.jpg"));
bg.setUIID("HeroBackground");
Label headline = new Label("Welcome");
headline.setUIID("HeroHeadline");
f.add(bg).add(headline);

// hero-headline rule in theme.css to position via padding
```

`LayeredLayout` stacks children on top of each other and lets you offset them with percent-based insets via `LayeredLayoutConstraint`.

### Card (rounded, shadow-ish)

```css
.card { border-radius: 12px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); padding: 16px; }
```

```css
/* theme.css */
Card {
    background-color: #ffffff;
    border-radius: 3mm;
    padding: 3mm;
    margin: 1mm;
    border: 1px solid #e2e8f0;     /* simulate light shadow with subtle border */
}
```

```java
Container card = new Container(BoxLayout.y());
card.setUIID("Card");
card.add(new Label("Title")).add(new SpanLabel("Body"));
```

CN1 has no real `box-shadow`. Options: a subtle border (above), a `RoundBorder` with `setShadowOpacity`, or a 9-patch image of a card with shadow baked in.

### Sticky header

```css
header { position: sticky; top: 0; }
```

Don't try to replicate `position: sticky`. Use `BorderLayout.NORTH` for the header and `BorderLayout.CENTER` (which is scrollable) for the body — the NORTH region stays put when the center scrolls.

### Centered content

```css
.center { display: flex; justify-content: center; align-items: center; }
```

```java
Container centered = FlowLayout.encloseCenter(new Label("Welcome"));
// or use a Form with FlowLayout configured to center vertically:
form.setLayout(new FlowLayout(Component.CENTER, Component.CENTER));
```

### Media queries / responsive

CN1 CSS has no `@media`. Branch in Java:

```java
if (Display.getInstance().isTablet()) {
    form.setLayout(new BorderLayout());
    form.add(BorderLayout.WEST, sideNav);
    form.add(BorderLayout.CENTER, detail);
} else {
    form.setLayout(BoxLayout.y());
    form.add(BoxLayout.encloseY(quickActions, detail));
}
```

See `references/mobile-adaptability.md` for the full responsive pattern matrix.

## Common HTML/CSS recipes

### "Login form"

```html
<form>
    <input type="email" placeholder="Email">
    <input type="password" placeholder="Password">
    <button type="submit">Sign in</button>
    <a href="/forgot">Forgot password?</a>
</form>
```

```java
TextField email = new TextField();
email.setHint("Email");
email.setConstraint(TextArea.EMAILADDR);
email.setSingleLineTextArea(true);

TextField password = new TextField();
password.setHint("Password");
password.setConstraint(TextArea.PASSWORD);
password.setSingleLineTextArea(true);

Button signIn = new Button("Sign in");
signIn.setUIID("PrimaryCta");
signIn.addActionListener(e -> doSignIn(email.getText(), password.getText()));

Button forgot = new Button("Forgot password?");
forgot.setUIID("LinkButton");
forgot.addActionListener(e -> showForgot());

form.add(BorderLayout.CENTER, BoxLayout.encloseY(email, password, signIn, forgot));
```

```css
PrimaryCta {
    background-color: #1d4ed8;
    color: #ffffff;
    border: 1px solid #1d4ed8;
    border-radius: 3mm;
    padding: 2mm 4mm;
}
LinkButton {
    background-color: transparent;
    color: #1d4ed8;
    border: none;
    padding: 1mm;
}
```

### "Card list with chevrons" (settings screen)

```html
<ul class="settings">
    <li><span>Profile</span><span>›</span></li>
    <li><span>Notifications</span><span>›</span></li>
</ul>
```

```java
Container list = new Container(BoxLayout.y());
for (String label : labels) {
    Container row = new Container(new BorderLayout());
    row.setUIID("SettingsRow");
    row.add(BorderLayout.CENTER, new Label(label));
    row.add(BorderLayout.EAST, new Label(FontImage.createMaterial(FontImage.MATERIAL_CHEVRON_RIGHT, "Label", 4)));
    row.setLeadComponent(row);
    row.addPointerPressedListener(e -> openSetting(label));
    list.add(row);
}
list.setScrollableY(true);
```

### "Hero image + headline overlay"

```html
<section class="hero">
    <img src="bg.jpg">
    <h1>Welcome</h1>
</section>
```

```java
Container hero = new Container(new LayeredLayout());
hero.add(new Label(Image.createImage("/hero-bg.jpg").scaledWidth(Display.getInstance().getDisplayWidth())));
Label headline = new Label("Welcome");
headline.setUIID("HeroHeadline");
hero.add(headline);

LayeredLayout.LayeredLayoutConstraint c = ((LayeredLayout)hero.getLayout()).createConstraint();
c.setInsets("50% 0 auto auto").setReferenceComponentLeft(headline, 0.5f);
hero.getLayout().addLayoutComponent(c, headline, hero);
```

### "Toast / snackbar"

```js
showToast("Saved");
```

```java
ToastBar.showMessage("Saved", FontImage.MATERIAL_CHECK);
```

### "Confirm before delete"

```js
if (confirm("Delete this?")) delete();
```

```java
if (Dialog.show("Delete?", "Are you sure?", "Delete", "Cancel")) delete();
```

## What you cannot do (and what to do instead)

| Web feature | CN1 alternative |
| --- | --- |
| `position: absolute` with pixel coords | `LayeredLayout` with `LayeredLayoutConstraint` |
| `position: sticky` | `BorderLayout.NORTH` (sticks via container structure) |
| `@media (max-width: 600px) { ... }` | Branch in Java on `Display.isTablet()` / `getDisplayWidth()` |
| `:hover` | Mobile has no hover; use `.pressed` state for press feedback |
| `transition: all 0.3s` | `Form.animateLayout(300)` after mutating layout |
| `transform: rotate(45deg)` | Override `paint(Graphics g)` and use `g.rotate(theta, x, y)` |
| `box-shadow` | `RoundBorder` with `setShadowOpacity`, or 9-patch with shadow baked in |
| `display: none` | `component.setHidden(true).setVisible(false)` then `parent.revalidate()` |
| CSS variables (`--primary-color`) | Theme constants `#Constants { primaryColor: #1d4ed8; }` read via `UIManager.getInstance().getThemeConstant(...)` |
| `filter: blur()` | None — pre-blur images at build time |
| `backdrop-filter` | None |
| `gradient` backgrounds | `Container` with a custom `Painter`; some CN1 versions support `cn1-derive-image` gradient borders |
