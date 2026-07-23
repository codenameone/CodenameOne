# Evidence map

Source: `docs/website/content/blog/metal-and-skins.md`
Canonical: https://www.codenameone.com/blog/metal-and-skins/

## Thesis

Migrating a custom-rendered iOS UI to Metal while preserving visual verification

## Supported beats

- **How we think about quality:** Codename One is a small open source company. We are not a 200-engineer platform team with a dedicated SRE rotation and a separate QA org.
- **Sticky headers were half baked, and that was by design:** Last week's post introduced StickyHeaderContainer with an animated transition between section headers. Within a couple of days the issue tracker had #4849, the NONE and FADE transitions were not behaving correctly and the swap had visible jitter.
- **The SIMD bug, which was my mistake:** PR #4842 is a different story. The SIMD code on iOS uses alloca to put working buffers on the stack for speed.
- **Metal is here:** PR #4799 is the largest single change we have landed in months: a complete Metal rendering backend for iOS. It sits next to the existing OpenGL ES 2 path, behind a single build hint, with its own CI job and its own pixel-diff goldens.
- **The end of the skin downloader:** PR #4758 ships the Skin Designer as a JavaScript bundle, embedded into the website at /skindesigner/ the same way the Playground and Initializr are embedded. You can build a skin in the browser, save it, and use it in your simulator without installing anything.
- **How the wizard works:** The Skin Designer turns a device specification (resolution, PPI, fonts, safe-area insets, cutouts) into a .skin file that the JavaSE simulator can load. It runs in your browser. There is nothing to install. The wizard is intentionally opinionated.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/issues/4849
- https://github.com/codenameone/CodenameOne/pull/4842
- https://github.com/codenameone/CodenameOne/pull/4799
- https://github.com/codenameone/CodenameOne/pull/4758
- https://www.codenameone.com/developer-guide/img/skin-designer/skin-designer-stage-1-device.png
- https://www.codenameone.com/developer-guide/img/skin-designer/skin-designer-stage-2-source.png
- https://www.codenameone.com/developer-guide/img/skin-designer/skin-designer-stage-3-editor-shape.png
- https://www.codenameone.com/developer-guide/img/skin-designer/skin-designer-stage-3-editor-cutouts.png
- https://www.codenameone.com/developer-guide/img/skin-designer/skin-designer-stage-3-editor-info.png
- https://www.codenameone.com/developer-guide/img/skin-designer/skin-designer-stage-4-done.png
- https://github.com/codenameone/CodenameOne/blob/master/docs/developer-guide/Skin-Designer.asciidoc
- https://github.com/codenameone/CodenameOne
