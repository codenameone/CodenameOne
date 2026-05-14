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
| `<select>` | `Picker` | Use `Picker` (with `pickerType=Display.PICKER_TYPE_STRINGS`) — opens a native sheet on mobile. Avoid `ComboBox`; its touch UX is poor across platforms. |
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

CN1 CSS supports `@media (prefers-color-scheme: dark)` (see `references/css.md`) but **no** viewport-size queries. Branch in Java for form-factor and orientation:

```java
Display d = Display.getInstance();

if (d.isTablet()) {
    form.setLayout(new BorderLayout());
    form.add(BorderLayout.WEST, sideNav);
    form.add(BorderLayout.CENTER, detail);
} else {
    form.setLayout(BoxLayout.y());
    form.add(BoxLayout.encloseY(quickActions, detail));
}

// Portrait vs. landscape
if (d.isPortrait()) {
    hero.setHidden(false);
} else {
    hero.setHidden(true);     // hide tall hero in landscape
}

// React when the device rotates
form.addOrientationListener(evt -> rebuildLayout(form));
```

`BorderLayout` has a built-in trick that often removes the need to branch on orientation manually: pass `BorderLayout.CENTER_BEHAVIOR_TOTAL_BELOW` (or call `setLandscapeSwap(...)` on the layout) and the WEST/EAST regions swap when the device rotates to landscape — useful for keeping a side panel on the leading edge as the layout flips.

`form.addOrientationListener(evt -> ...)` fires whenever the screen rotates between portrait and landscape; rebuild your layout there for anything more involved than a simple component flip.

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
| `@media (max-width: 600px) { ... }` | No viewport queries — branch in Java on `Display.isTablet()` / `getDisplayWidth()`. |
| `@media (prefers-color-scheme: dark) { ... }` | **Supported** — see `references/css.md`. |
| `:hover` | Mobile has no hover; use `.pressed` state for press feedback |
| `transition: all 0.3s` | `Form.animateLayout(300)` after mutating layout |
| `transform: rotate(45deg)` | Override `paint(Graphics g)` and use `g.rotate(theta, x, y)` |
| `box-shadow` | `RoundBorder` with `setShadowOpacity`, or 9-patch with shadow baked in |
| `display: none` | `component.setHidden(true).setVisible(false)` then `parent.revalidate()` |
| CSS variables (`--primary-color`) | Theme constants `#Constants { primaryColor: #1d4ed8; }` read via `UIManager.getInstance().getThemeConstant(...)` |
| `filter: blur()` | None — pre-blur images at build time |
| `backdrop-filter` | None |
| `gradient` backgrounds | `Container` with a custom `Painter`; some CN1 versions support `cn1-derive-image` gradient borders |

## Porting Android (XML + Kotlin/Java) to Codename One

Codename One's component model is similar enough to Android's that you can usually translate screens one-to-one. As with HTML, the layout system is the part that differs most.

### Android view → CN1 component

| Android `View` | CN1 component | Notes |
| --- | --- | --- |
| `LinearLayout` (vertical) | `BoxLayout.y()` | |
| `LinearLayout` (horizontal) | `BoxLayout.x()` | |
| `RelativeLayout` / `ConstraintLayout` | `LayeredLayout` with `LayeredLayoutConstraint` | Use percent or mm insets and `setReferenceComponent*` for "below this view". |
| `FrameLayout` | `LayeredLayout` | Same stacking semantics. |
| `GridLayout` (Android's, not CSS) | `GridLayout` | |
| `RecyclerView` | `InfiniteContainer` | The pagination/recycling story maps directly. |
| `ScrollView` (vertical) | `Container.setScrollableY(true)` | Wrap your content. |
| `HorizontalScrollView` | `Container.setScrollableX(true)` | |
| `NestedScrollView` | **Don't nest scrollables in CN1** — see scrolling rules in `references/ui-components.md`. |
| `Toolbar` / `ActionBar` | `Form.getToolbar()` | Already on every Form. |
| `BottomNavigationView` | `Toolbar.addCommandToBottomBar(...)` or `Tabs` at the bottom. | |
| `NavigationView` (drawer) | `Toolbar.addCommandToSideMenu(...)` | |
| `TextView` | `Label` (single line) / `SpanLabel` (wrapped) | |
| `EditText` | `TextField` (single line) / `TextArea` (multi-line) | Set `constraint` for the keyboard type. |
| `Button` / `MaterialButton` | `Button` | |
| `ImageView` | `Label` with image, or `URLImage` for remote | |
| `Switch` / `CheckBox` / `RadioButton` | `Switch` / `CheckBox` / `RadioButton` | RadioButton needs `ButtonGroup`. |
| `Spinner` | `Picker` (string list) | Do **not** use `ComboBox`. |
| `ProgressBar` | `Slider` (set max) or `InfiniteProgress` (spinner) | |
| `Dialog` / `AlertDialog` | `Dialog.show(...)` | `Toast` → `ToastBar.showMessage(...)`. |
| `Fragment` | A factory method returning a configured `Container`, attached to a Form. CN1 has no Fragment lifecycle — keep state in regular Java objects. |
| `Activity` | A separate `Form` class (or factory). Navigation = `nextForm.show()`. |
| `Intent` (in-app) | Direct method call to the next Form's factory. |
| `Intent` (external — phone/email/url) | `Display.execute("tel:..." / "mailto:..." / url)`. |
| `RecyclerView.Adapter` | Implement `InfiniteContainer.fetchComponents(int, int)` or pass a list to `MultiList`. |

### Android XML → CN1 Java

CN1 has no XML layout for screens (the GUI builder uses its own format). Translate Android XML directly to Java:

```xml
<!-- Android: res/layout/profile.xml -->
<LinearLayout android:orientation="vertical" android:padding="16dp">
    <TextView android:text="@string/name" style="@style/Headline" />
    <EditText android:hint="@string/name_hint" />
    <Button android:text="@string/save" style="@style/PrimaryButton" />
</LinearLayout>
```

```java
// CN1
Container col = new Container(BoxLayout.y());
col.setUIID("ProfileCard");           // padding/margin lives in CSS

Label headline = new Label(L10n.get("name"));
headline.setUIID("Headline");

TextField nameField = new TextField();
nameField.setHint(L10n.get("name_hint"));

Button save = new Button(L10n.get("save"));
save.setUIID("PrimaryCta");

col.add(headline).add(nameField).add(save);
```

```css
/* Android themes ~= CN1 CSS rules. */
ProfileCard { padding: 2mm; }
Headline { font-family: "native:MainBold"; font-size: 4mm; color: #0f172a; }
PrimaryCta { background-color: #1d4ed8; color: #ffffff; border-radius: 3mm; padding: 2mm 4mm; }
```

### Android idioms that DO NOT translate

| Android | Why it doesn't translate | What to do |
| --- | --- | --- |
| `Context` everywhere | CN1 has no `Context`. | Use `Display.getInstance()` or static singletons. |
| `findViewById(R.id.x)` | No XML view inflation. | Hold component references as fields after constructing. |
| `Handler.post(...)` | No `Handler`. | `Display.callSerially(...)`. |
| `LiveData`, `ViewModel` (Architecture Components) | None of the Jetpack stack is available. | `com.codename1.properties.*` for property binding, or plain observer fields. |
| `Room`, `LiveData<List<X>>` | No Room. | `com.codename1.db.Database` (SQLite) + manual model layer. |
| `SharedPreferences` | Different API. | `com.codename1.io.Preferences` (drop-in semantically). |
| `Picasso` / `Glide` for image loading | Not available. | `URLImage.createToStorage(...)` or `MultiImage`. |
| `Retrofit` / `OkHttp` | Not in the JDK subset. | `Rest.get/post(...)` — see `references/java-api-subset.md`. |
| `Coroutines` / `RxJava` | No coroutines runtime; no RxJava. | `Display.startThread(...)` + `Display.callSerially(...)` for async; chain callbacks. |
| `R.string.xxx` | No resources system. | `UIManager.getInstance().localize("xxx", "default")` reading from `messages.properties` bundles. |
| `Permission` manifest entries | Different mechanism. | `Display.requestPermission(...)` at runtime + `codename1.arg.android.xPermissions` build hint. |
| `Activity onCreate/onResume` | No Activity lifecycle. | Override `Lifecycle.init/start/stop` (app-level) and `Form.show()` (per-screen). |

### Android resources

| Android | CN1 |
| --- | --- |
| `res/values/strings.xml` | `common/src/main/l10n/messages.properties` (and per-locale `messages_de.properties`, etc.) |
| `res/drawable/foo.png` | `common/src/main/resources/foo.png` (flat namespace — see `references/java-api-subset.md`) |
| `res/values/colors.xml` | Theme constants in `theme.css` under `#Constants { ... }` |
| `res/font/x.ttf` | `common/src/main/css/fonts/x.ttf`, declared via `@font-face` in `theme.css` |
