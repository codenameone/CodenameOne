---
title: "Native Menus, File Dialogs and 3D Models"
slug: desktop-dialogs-menus-3d-models
url: /blog/desktop-dialogs-menus-3d-models/
date: '2026-10-28'
author: Shai Almog
description: "Codename One apps now get the operating system's own open and save dialogs, a real menu bar with submenus, check items and standard commands, and animated, textured 3D models. One API, placed the way each platform expects."
feed_html: "<img src=\"https://www.codenameone.com/blog/desktop-dialogs-menus-3d-models.jpg\" alt=\"A Codename One app with a native menu bar and file dialog\" /> Native open and save dialogs, a real menu bar with submenus and standard commands, and animated, textured 3D models."
series: ["release-2026-10-23"]
---

![A Codename One app with a native menu bar and file dialog](/blog/desktop-dialogs-menus-3d-models.jpg)

A desktop app gets judged in the first ten seconds. If the menu bar is wrong, Cmd+Q does nothing or the Save dialog looks like it came from a different decade, people notice, even if they can't say what bothers them. The [word processor](/blog/rewrite-everything-in-java-not-rust/) from last Friday's post needed all of that to hold up next to a native app, so those pieces are now part of Codename One. They work with the code you already write for a phone.

## The real open and save dialogs

`openFileChooser()` picks one file. Desktop apps need more: several filters, a suggested name, extra options in the dialog and a preview while the user browses. `showOpenDialog()` and `showSaveDialog()` cover that:

```java {class="cn1-no-try"}
Display.getInstance().showOpenDialog(new OpenFileRequest()
        .title("Import photos")
        .multiSelect(true)
        .addFilter(new FileTypeFilter("Images")
                .extensions("png", "jpg").mimeTypes("image/*"))
        .addOption(FileDialogOption.checkbox("strip", "Remove location data", true))
        .preview(path -> {
            try (InputStream in = FileSystemStorage.getInstance().openInputStream(path)) {
                return new ScaleImageLabel(Image.createImage(in));
            } catch (IOException err) {
                return new Label("No preview");
            }
        }),
    result -> {
        if (result.isCancelled()) {
            return;
        }
        boolean strip = "true".equals(result.getOptionValue("strip"));
        for (String path : result.getPaths()) {
            // import each file
        }
    });
```

{{< guide-block >}}
<figure style="margin:24px 0"><a href="/blog/release-2026-10-23/file-dialog-preview-option.jpg"><img width="1110" height="880" src="/blog/release-2026-10-23/file-dialog-preview-option.jpg" alt="An open dialog with an image preview, an Images filter and a Remove location data checkbox" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The request above in the simulator, with the preview and the option.</figcaption></figure>
{{< /guide-block >}}

Every platform opens and saves files. The extras appear where the platform has them, and the rest ignore them, so one request works everywhere:

| Platform | Open | Save |
| --- | --- | --- |
| macOS | `NSOpenPanel` | `NSSavePanel`, with format list, options and tag field |
| Windows | `IFileOpenDialog` | `IFileSaveDialog`, with options as dialog controls |
| Linux | `GtkFileChooser` | `GtkFileChooser` with overwrite confirmation |
| Android | The system document picker, several files, folders | Create document, with no storage permission |
| iOS | The document picker, several files, folders | Exported through the document picker |
| Browser | The browser's file picker | A download with the suggested name |

A save request writes its content after the user has picked the location, off the EDT, and it can see which format and options were chosen. Platform hints, such as the macOS tag field or the folder Android starts in, are one call each.

## A menu bar that follows each platform

A `Command` already knows its name, icon, and action. Tell it where it goes and it shows up in the native menu bar on macOS, Windows, Linux and in the browser build. Paths make submenus, and check and radio items work the way you'd expect:

```java {class="cn1-no-try"}
String[] zooms = {"50%", "100%", "200%"};
for (String zoom : zooms) {
    Command c = Command.create(zoom, null, e -> setZoom(zoom));
    c.setDesktopMenu("View/Zoom").setDesktopMenuGroup(1).setDesktopRadioGroup("zoom");
    c.setDesktopChecked("100%".equals(zoom));
    form.getToolbar().addCommandToOverflowMenu(c);
}
```

On a phone the same commands stay in the overflow menu, so there's nothing to branch on.

{{< guide-block >}}
<figure style="margin:24px 0"><a href="/blog/release-2026-10-23/native-menu-submenu.jpg"><img width="1400" height="871" src="/blog/release-2026-10-23/native-menu-submenu.jpg" alt="The word processor's File menu with an Export submenu on macOS" loading="lazy" style="width:100%;height:auto" /></a><figcaption>The word processor's File menu on macOS.</figcaption></figure>
{{< /guide-block >}}

About, Settings and Quit live in different places on every desktop, under different names. Give the command a role and each platform puts it where its own apps do, with its own shortcut:

```java {class="cn1-no-try"}
Command settings = Command.createDesktopRole(Command.DESKTOP_ROLE_SETTINGS, e -> showSettings());
Command quit = Command.createDesktopRole(Command.DESKTOP_ROLE_QUIT, e -> quit());
```

| Role | macOS | Windows | Linux |
| --- | --- | --- | --- |
| About | Application menu, first | Help, last | Help, last |
| Settings | "Settings…" in the application menu, Cmd+, | "Options" under Tools | "Preferences" at the foot of Edit |
| Quit | "Quit" in the application menu, Cmd+Q | "Exit" at the foot of File | "Quit" at the foot of File, Ctrl+Q |

The File and Edit roles work the same way, so Redo is Cmd+Shift+Z on a Mac and Ctrl+Y on Windows without you looking it up. On macOS a role replaces the built-in item, so the menu never shows two Quit items.

{{< guide-block >}}
<figure style="margin:24px 0"><a href="/blog/release-2026-10-23/native-menu-app-roles.jpg"><img width="1000" height="600" src="/blog/release-2026-10-23/native-menu-app-roles.jpg" alt="The macOS application menu with About, Settings and Quit" loading="lazy" style="width:100%;height:auto" /></a><figcaption>Roles landing in the macOS application menu.</figcaption></figure>
{{< /guide-block >}}

One more small thing that makes an editor feel right: `form.setDocumentModified(true)` puts the dot in the close button on macOS and an asterisk before the title on Windows and Linux, and the Save command enables itself while there's something to save.

## Animated 3D models

The other big addition came from [Skirmish](/blog/build-a-multiplayer-strategy-game-in-java/). `GltfLoader` loads glTF 2.0 models, binary `.glb` or JSON `.gltf`, together with their textures, so a textured model renders with no extra setup. `AnimatedModel` brings them to life:

```java {class="cn1-no-try"}
GltfScene knight = GltfLoader.loadScene(in);
AnimatedModel unit = new AnimatedModel(device, knight);
unit.play("Walking_A", true);
unit.addTo(gameView);
// later
unit.crossFade("1H_Melee_Attack_Chop", 0.15f, false);
```

It plays the model's animation clips, cross-fades between them, deforms skinned meshes with their skeleton and carries rigid parts, like a sword in a hand, along with their bones. Skinning runs on the CPU, so it looks identical on every port and needs nothing from the shaders. Many instances can share one loaded scene: the vertex data, clips and textures are shared, and only the posed vertices are per instance. That's how a Skirmish battle fills the arena with troops.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,200px),1fr));gap:16px;align-items:center;margin:24px 0">
<figure style="margin:0"><a href="/developer-guide/img/gpu-model.png"><img width="358" height="276" src="/developer-guide/img/gpu-model.png" alt="The Khronos BoomBox glTF sample rendered with its own texture" loading="lazy" style="width:100%;height:auto" /></a><figcaption>A textured glTF model.</figcaption></figure>
<figure style="margin:0"><a href="/developer-guide/img/skirmish-battle.png"><img width="420" height="909" src="/developer-guide/img/skirmish-battle.png" alt="Animated characters in a Skirmish battle" loading="lazy" style="width:100%;height:auto;max-width:220px" /></a><figcaption>Animated characters in Skirmish.</figcaption></figure>
</div>
{{< /guide-block >}}

The [desktop integration guide](/developer-guide/desktop-integration/), the [file system chapter](/developer-guide/io/) and the [3D graphics chapter](/developer-guide/3d-graphics/) have the details.

Tomorrow, the last post of the week: {{< post-link path="/blog/private-calls-and-safer-backends" text="WebRTC Calls, Key Agreement and ORM Checks at Build Time" >}}.

{{< giscus >}}
