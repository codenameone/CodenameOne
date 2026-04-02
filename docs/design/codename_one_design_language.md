# Generic Builder UI Design Guide

A practical, developer-oriented design guide derived from the original GUI builder mockups. This version is intentionally generic so it can be reused across company products, especially desktop-style admin tools, editors, inspectors, and builders.

## 1. Design intent

Use this design language for products that need:
- dense but readable controls
- left/right side panels
- property inspectors
- modal configuration dialogs
- selection-heavy workflows
- light and dark themes with the same structure

The visual character should feel:
- technical
- precise
- restrained
- slightly premium

## 2. Core rules

### Structure
- Prefer panel-based layouts: top toolbar, left navigation, center workspace, right inspector.
- Use borders and contrast to separate areas instead of heavy shadows.
- Keep controls aligned to a strong grid.

### Shape
- Use small rounded corners only.
- Avoid soft, playful, oversized radii.
- Default border width: 1px.
- Focus/selected border width: 2px.

### Accent usage
- **Blue** = selection, focus, active controls, links, sliders, handles.
- **Lime** = primary confirmation action only, usually **Done** or **Apply**.
- Do not use lime as a general accent.

### Density
- Compact is good.
- Cramped is bad.
- Use tight spacing inside controls, larger spacing between groups.

## 3. Fonts

Use a clean UI sans-serif. Recommended stack:

```css
font-family: Inter, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
```

Recommended weights:
- 400 for values and body text
- 500 for labels and secondary emphasis
- 600 for buttons, section headers, and important UI text

Recommended sizes:
- App title: 20px / 600
- Panel title: 14px / 600
- Section label: 12px / 600 / uppercase optional
- Field label: 12px / 500
- Input text: 14px / 400
- Helper text: 12px / 400
- Button text: 14px / 600

## 4. Color palette

Use semantic roles, but these concrete values are a good starting point.

### Light theme
```css
--bg-app: #F3F4F7;
--bg-panel: #FFFFFF;
--bg-subtle: #F7F8FB;
--bg-input: #FFFFFF;
--bg-preview: #FAFAFC;
--border: #D9DEE8;
--border-strong: #BFC7D6;
--text: #112247;
--text-muted: #7F8AA3;
--text-soft: #A0A8BA;
--accent: #2F6BFF;
--accent-soft: #E8F0FF;
--success: #B8D532;
--success-text: #FFFFFF;
--overlay: rgba(17, 34, 71, 0.18);
```

### Dark theme
```css
--bg-app: #071B4D;
--bg-panel: #102B66;
--bg-subtle: #163575;
--bg-input: #0E2A61;
--bg-preview: #112F70;
--border: #4C6EA8;
--border-strong: #7390C0;
--text: #F5F8FF;
--text-muted: #A8B8DA;
--text-soft: #7E93BC;
--accent: #4D86FF;
--accent-soft: rgba(77, 134, 255, 0.16);
--success: #B8D532;
--success-text: #FFFFFF;
--overlay: rgba(0, 0, 0, 0.35);
```

## 5. Spacing and sizing

Use this spacing scale:

```css
--space-1: 4px;
--space-2: 8px;
--space-3: 12px;
--space-4: 16px;
--space-5: 24px;
--space-6: 32px;

--radius-sm: 4px;
--radius-md: 6px;
--radius-lg: 8px;
```

Guidelines:
- Use 8px between related controls.
- Use 16px around groups.
- Use 24px between sections.
- Default control height: 40px.
- Dense control height: 32px.
- Icon button size: 40px square.

## 6. Icons and icon style

Use a single outline icon set. Good choices:
- **Phosphor**
- **Lucide**
- **Material Symbols Outlined**

Guidelines:
- Use outline icons, not mixed outline/fill styles.
- Use 1.5px to 2px stroke appearance.
- Keep icons geometric and literal.
- Default icon size: 18px to 20px.
- Inactive icons use muted text color.
- Active icons use the blue accent.
- Icon buttons should sit inside bordered square containers.

Common icon patterns:
- toolbar: undo, redo, copy, paste, delete
- panel headers: menu, more, collapse, expand
- inspectors: alignment, border, color, visibility, preview
- tree/palette: folder, component, layers, plus

## 7. Component rules

### Panels
- Use flat surfaces with visible borders.
- Header row should be visually stronger than body.
- Support scrolling without changing panel padding.

### Inputs
- White or theme-matched fill.
- Visible 1px border.
- Optional embedded unit selector on the right.
- Focus = blue border or blue ring.

### Buttons
- Primary button: lime fill, white text.
- Secondary button: neutral surface with border.
- Icon button: bordered square with accent on active.

### Section headers
- Small, strong, compact.
- Use uppercase only when it improves scanning.

### Modals
- Centered card.
- Large internal padding.
- Clear footer actions.
- Use preview regions when choosing a visual property.

### Selection state
- Blue border.
- Blue icon.
- Optional pale blue background.
- In builder views, use visible handles and guides.

## 8. Implementation tokens

Use these token names in code:

```css
--color-bg-app
--color-bg-panel
--color-bg-subtle
--color-bg-input
--color-border
--color-border-strong
--color-text
--color-text-muted
--color-text-soft
--color-accent
--color-accent-soft
--color-success
--color-success-text
--space-1
--space-2
--space-3
--space-4
--space-5
--space-6
--radius-sm
--radius-md
--radius-lg
```

## 9. Practical do/don't

Do:
- keep layouts panel-driven
- use blue consistently for interaction
- reserve lime for the primary confirmation action
- rely on borders and spacing for structure
- make dark mode navy-based, not black

Don't:
- use large soft shadows
- use many bright colors
- mix different icon families
- make corners too round
- overload the UI with gradients

## 10. Sample HTML/CSS

This sample includes a built-in light/dark toggle so you can test both themes quickly. It uses a `data-theme` attribute on `<html>` and a single button to switch modes.

```html
<!DOCTYPE html>
<html lang="en" data-theme="dark">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Builder UI Sample</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
  <style>
    :root {
      --space-1: 4px;
      --space-2: 8px;
      --space-3: 12px;
      --space-4: 16px;
      --space-5: 24px;
      --space-6: 32px;
      --radius-sm: 4px;
      --radius-md: 6px;
      --radius-lg: 8px;
      --shadow-none: none;
    }

    html[data-theme="light"] {
      --bg-app: #F3F4F7;
      --bg-panel: #FFFFFF;
      --bg-subtle: #F7F8FB;
      --bg-input: #FFFFFF;
      --bg-preview: #FAFAFC;
      --border: #D9DEE8;
      --border-strong: #BFC7D6;
      --text: #112247;
      --text-muted: #7F8AA3;
      --text-soft: #A0A8BA;
      --accent: #2F6BFF;
      --accent-soft: #E8F0FF;
      --success: #B8D532;
      --success-text: #FFFFFF;
      --overlay: rgba(17, 34, 71, 0.18);
    }

    html[data-theme="dark"] {
      --bg-app: #071B4D;
      --bg-panel: #102B66;
      --bg-subtle: #163575;
      --bg-input: #0E2A61;
      --bg-preview: #112F70;
      --border: #4C6EA8;
      --border-strong: #7390C0;
      --text: #F5F8FF;
      --text-muted: #A8B8DA;
      --text-soft: #7E93BC;
      --accent: #4D86FF;
      --accent-soft: rgba(77, 134, 255, 0.16);
      --success: #B8D532;
      --success-text: #FFFFFF;
      --overlay: rgba(0, 0, 0, 0.35);
    }

    * { box-sizing: border-box; }

    html, body {
      margin: 0;
      height: 100%;
    }

    body {
      font-family: Inter, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      background: var(--bg-app);
      color: var(--text);
    }

    button, input {
      font: inherit;
    }

    .app {
      display: grid;
      grid-template-columns: 280px 1fr 320px;
      grid-template-rows: 56px 1fr;
      height: 100vh;
    }

    .toolbar {
      grid-column: 1 / -1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 var(--space-4);
      background: var(--bg-panel);
      border-bottom: 1px solid var(--border);
    }

    .toolbar-group {
      display: flex;
      gap: var(--space-2);
      align-items: center;
    }

    .title {
      font-size: 20px;
      font-weight: 600;
    }

    .icon-btn, .tool-btn {
      height: 40px;
      min-width: 40px;
      padding: 0 var(--space-3);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      background: var(--bg-panel);
      color: var(--text-muted);
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: var(--space-2);
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
    }

    .icon-btn.active, .tool-btn.active {
      color: var(--accent);
      border-color: var(--accent);
      background: var(--accent-soft);
    }

    .panel {
      background: var(--bg-panel);
      border-right: 1px solid var(--border);
      overflow: auto;
    }

    .panel.right {
      border-right: 0;
      border-left: 1px solid var(--border);
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--space-4);
      border-bottom: 1px solid var(--border);
      font-size: 14px;
      font-weight: 600;
    }

    .panel-body {
      padding: var(--space-4);
    }

    .section {
      margin-bottom: var(--space-5);
    }

    .section-title {
      margin: 0 0 var(--space-3);
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.04em;
      color: var(--text-muted);
    }

    .field {
      margin-bottom: var(--space-3);
    }

    .label {
      display: block;
      margin-bottom: var(--space-2);
      font-size: 12px;
      font-weight: 500;
      color: var(--text-muted);
    }

    .input-row {
      display: flex;
      gap: var(--space-2);
    }

    .input, .unit {
      height: 40px;
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      background: var(--bg-input);
      color: var(--text);
      padding: 0 var(--space-3);
      font-size: 14px;
      display: flex;
      align-items: center;
    }

    .input {
      flex: 1;
    }

    .unit {
      width: 64px;
      justify-content: center;
      color: var(--text-muted);
    }

    .color-row {
      display: flex;
      gap: var(--space-2);
      align-items: center;
    }

    .swatch {
      width: 40px;
      height: 40px;
      border-radius: var(--radius-md);
      border: 1px solid var(--border);
      background: #ADD12E;
      flex: 0 0 auto;
    }

    .workspace {
      padding: var(--space-5);
      overflow: auto;
    }

    .canvas {
      height: calc(100vh - 56px - 48px);
      border: 1px solid var(--border-strong);
      border-radius: var(--radius-lg);
      background: var(--bg-subtle);
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .smart-bar {
      position: absolute;
      top: var(--space-4);
      display: flex;
      gap: var(--space-2);
    }

    .component {
      width: 220px;
      padding: var(--space-6) var(--space-5);
      border: 2px solid var(--accent);
      border-radius: var(--radius-lg);
      background: var(--bg-panel);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: var(--space-4);
      box-shadow: var(--shadow-none);
    }

    .primary-btn {
      height: 44px;
      padding: 0 24px;
      border: 1px solid transparent;
      border-radius: var(--radius-md);
      background: var(--success);
      color: var(--success-text);
      font-size: 14px;
      font-weight: 600;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
    }

    .meta {
      text-align: center;
      color: var(--text-muted);
      font-size: 12px;
      line-height: 1.4;
    }

    .tree-item {
      padding: 10px 12px;
      border-radius: var(--radius-md);
      color: var(--text);
      font-size: 14px;
      display: flex;
      align-items: center;
      gap: var(--space-2);
      border: 1px solid transparent;
    }

    .tree-item.active {
      background: var(--accent-soft);
      color: var(--accent);
      border-color: var(--accent);
    }

    .divider {
      height: 1px;
      background: var(--border);
      margin: var(--space-4) 0;
    }
  </style>
</head>
<body>
  <div class="app">
    <header class="toolbar">
      <div class="toolbar-group">
        <button class="icon-btn active">≡</button>
        <button class="icon-btn">↶</button>
        <button class="icon-btn">↷</button>
        <button class="icon-btn">⧉</button>
      </div>
      <div class="title">Builder UI</div>
      <div class="toolbar-group">
        <button class="icon-btn" id="themeToggle" aria-label="Toggle theme">☀︎ / ☾</button>
        <button class="icon-btn">⬆</button>
        <button class="icon-btn active">💾</button>
      </div>
    </header>

    <aside class="panel">
      <div class="panel-header">
        <span>COMPONENT TREE</span>
        <span>⋮</span>
      </div>
      <div class="panel-body">
        <div class="tree-item active">▾ Button</div>
        <div class="tree-item">▸ Container</div>
        <div class="tree-item">▸ Dialog</div>
        <div class="divider"></div>
        <div class="section-title">ACTIONS</div>
        <div class="input-row">
          <button class="icon-btn">+</button>
          <button class="icon-btn">−</button>
          <button class="icon-btn">□</button>
        </div>
      </div>
    </aside>

    <main class="workspace">
      <div class="canvas">
        <div class="smart-bar">
          <button class="tool-btn active">Smart Insets</button>
          <button class="tool-btn">Auto Snap</button>
        </div>
        <div class="component">
          <button class="primary-btn">Primary Button</button>
          <div class="meta">
            border.png<br>
            20 × 19
          </div>
        </div>
      </div>
    </main>

    <aside class="panel right">
      <div class="panel-header">
        <span>PROPERTY INSPECTOR</span>
        <span>⋮</span>
      </div>
      <div class="panel-body">
        <div class="section">
          <h3 class="section-title">TYPOGRAPHY</h3>
          <div class="field">
            <label class="label">Font Name</label>
            <div class="input">Inter Semibold</div>
          </div>
          <div class="field">
            <label class="label">Font Size</label>
            <div class="input-row">
              <div class="input">12</div>
              <div class="unit">mm</div>
            </div>
          </div>
        </div>

        <div class="section">
          <h3 class="section-title">BACKGROUND</h3>
          <div class="field">
            <label class="label">BG Type</label>
            <div class="input">gradient_linear_vertical</div>
          </div>
          <div class="field">
            <label class="label">FG Color</label>
            <div class="color-row">
              <div class="input">#ADD12E</div>
              <div class="swatch"></div>
            </div>
          </div>
          <div class="field">
            <label class="label">BG Color</label>
            <div class="color-row">
              <div class="input">#FFFFFF</div>
              <div class="swatch" style="background:#FFFFFF"></div>
            </div>
          </div>
        </div>

        <div class="section">
          <h3 class="section-title">LAYOUT</h3>
          <div class="field">
            <label class="label">Border</label>
            <div class="input-row">
              <div class="input">2</div>
              <div class="unit">mm</div>
            </div>
          </div>
        </div>
      </div>
    </aside>
  </div>

  <script>
    const root = document.documentElement;
    const toggle = document.getElementById('themeToggle');

    toggle.addEventListener('click', () => {
      const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      root.setAttribute('data-theme', next);
    });
  </script>
</body>
</html>
```

### Theme toggle pattern

Use this pattern in production too:
- Put theme tokens on `html[data-theme="light"]` and `html[data-theme="dark"]`
- Keep component CSS identical across both themes
- Only swap semantic color variables
- Persist the theme with `localStorage` if needed

## 11. Developer note

To implement this properly in production:
- put the colors behind semantic tokens
- keep light and dark on the same spacing and component rules
- create shared CSS classes or design tokens for panels, inputs, icon buttons, and section headers
- test both themes with inspector-heavy screens, not just simple forms

