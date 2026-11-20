---
title: "Brickyard: Launch Your Own Game Platform, With the Studio Inside the App"
slug: build-a-game-platform-with-luau-scripting
url: /blog/build-a-game-platform-with-luau-scripting/
date: '2026-11-26'
author: Shai Almog
description: "Brickyard is a user-generated game platform template: play 3D experiences with friends, build them in a studio inside the same app on a phone, tablet or desktop, script them in Luau, and publish. It reads Roblox place files, lets an AI agent build and play test through MCP, and runs on new portable GPU shaders and a 3D audio graph."
feed_html: "<img src=\"https://www.codenameone.com/blog/build-a-game-platform-with-luau-scripting.jpg\" alt=\"Brickyard game platform\" /> Play and build 3D games in one app, scripted in Luau, with Roblox place file import, an AI agent that play tests, portable shaders and spatial audio."
series: ["release-2026-11-20"]
---

![Brickyard game platform](/blog/build-a-game-platform-with-luau-scripting.jpg)

The biggest game platforms of the last decade aren't games. They're places where players build games for each other. The insight is that the studio and the game are the same product: the kid playing an obstacle course today is the one building the next one tomorrow, on the same device.

Brickyard is that platform as a Codename One template. One app to browse experiences, play them in 3D with friends, chat, and build them in a full studio, on a phone, a tablet or a desktop. Publish, earn, and run it on your own authoritative multiplayer server. It uses nearly everything we've built: the code editor, the AI tooling, the [gaming API](/blog/build-a-multiplayer-strategy-game-in-java/), and two new pieces from this week, portable shaders and 3D audio.

{{< guide-block >}}
<video controls playsinline preload="metadata" poster="/brickyard/brickyard-desktop-poster.jpg" style="width:100%;height:auto;display:block;margin:16px auto" aria-label="Playing and building in Brickyard on the desktop"><source src="/brickyard/brickyard-desktop.mp4" type="video/mp4"><a href="/brickyard/brickyard-desktop.mp4">Download the Brickyard video</a>.</video>
{{< /guide-block >}}

## Scripted in Luau, the language millions of young creators already write

Brickyard runs [Luau](https://luau.org/), the typed Lua dialect, on a pure-Java interpreter with full Luau syntax and the standard libraries. A kill brick in an obstacle course looks exactly like it does everywhere else Luau creators work:

```lua
for _, brick in ipairs(CollectionService:GetTagged("KillBrick")) do
    brick.Touched:Connect(function(hit)
        local h = hit.Parent and hit.Parent:FindFirstChildOfClass("Humanoid")
        if h then h.Health = 0 end
    end)
end
```

Coroutines are interpreter frames, not threads, so a thousand scripts waiting on events cost almost nothing. Every resume runs under an instruction budget. A creator's infinite loop becomes an error in the output panel, never a frozen game, which matters a lot when your creators are twelve.

That makes Brickyard compatible with the games creators already have. It reads and writes Roblox place files, both `.rbxlx` and the binary `.rbxl`, and exports `.rbxlx`. Import a place and you get a compatibility report listing the API its scripts use that the engine doesn't have yet, and which assets were replaced with placeholders. The engine is a clean room implementation, and nothing is fetched from Roblox.

## A real game studio, in the app

Explorer, properties, a Luau editor with syntax checks and completion, lighting presets, undo, play test and publish. On a phone, on a tablet, on a desktop. The same studio, the same files.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,300px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/brickyard-desktop-studio.png" alt="The Brickyard studio on the desktop with explorer, viewport and script" loading="lazy" style="width:100%;height:auto" /><figcaption>The studio on the desktop.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/brickyard-studio-tablet.png" alt="The Brickyard studio on a tablet" loading="lazy" style="width:100%;height:auto" /><figcaption>The same studio on a tablet.</figcaption></figure>
</div>
{{< /guide-block >}}

It ships with templates for the genres that fill these platforms: obby, survival, role-play town, tycoon, simulator, racing and a fighting arena. The economy is there too, a currency, a Plus subscription and a season pass, plus an admin console to run it all.

{{< guide-block >}}
<div class="release-screenshots" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,240px),1fr));gap:16px;align-items:start;margin:24px 0">
<figure style="margin:0"><img src="/developer-guide/img/brickyard-script-editor.png" alt="The Luau script editor on a phone" loading="lazy" style="width:100%;height:auto" /><figcaption>The Luau editor on a phone.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/brickyard-lighting.png" alt="Lighting presets in the studio" loading="lazy" style="width:100%;height:auto" /><figcaption>Lighting.</figcaption></figure>
<figure style="margin:0"><img src="/developer-guide/img/brickyard-playtest.png" alt="A multiplayer play test" loading="lazy" style="width:100%;height:auto" /><figcaption>Play test with several players.</figcaption></figure>
</div>
{{< /guide-block >}}

### An AI agent that builds and play tests

Choose AI agent mode and the studio registers its tools with the app's built-in MCP server under the `brickyard_` prefix. Claude, Codex or any MCP client can then build the experience: edit the instance tree, write scripts and get the syntax errors back, apply templates, add effects and lighting, take screenshots and publish.

The tool that matters most is `playtest`. It plays a saved copy with a number of players for a number of seconds and answers with the output, the errors, the leaderboard stats and any unsupported API. The agent doesn't guess whether its obby works. It plays it, reads what broke, and fixes it. Every button in the studio goes through the same tools, so the edits show up live in the explorer while you watch.

{{< mermaid >}}
flowchart LR
    A[Claude or Codex] -->|MCP: brickyard_*| S[Studio tools]
    S --> T[Instance tree<br/>scripts, lighting]
    S --> P[playtest<br/>N players, N seconds]
    P -->|output, errors,<br/>leaderstats| A
    S --> PUB[Publish]
{{< /mermaid >}}

## Shaders you write once, in Java

Brickyard draws with the `gpu` package, which runs on Metal on iOS and macOS, Direct3D 11 on Windows, OpenGL ES on Android and WebGL in the browser. This week it got portable shader nodes: you build a shader as a typed expression graph in Java, and every port prints it as GLSL, Metal, or HLSL. No shader written three times, no platform you forgot to test.

It comes with a library you'd normally buy in an asset store. Ocean, lake and river water. Terrain and foliage. Painted metal, rust, wood, concrete, glass, ice and car paint. Cartoon shading. Lava and caustics. Skies for day, dusk, night, and overcast. And effects: dissolve, hologram, force field, beam, fire, smoke, portal and x-ray.

```java {class="cn1-no-try"}
ScenePresets.Preset look = ScenePresets.harbourDusk();
look.apply(device);
Material sea = new Material(Material.Type.PBR)
        .setShader(Water.ocean().setUniform(Water.WAVE_HEIGHT, 0.6f));
Material quay = new Material(Material.Type.PBR).setShader(Surface.concrete());
PostProcess post = look.getPost().setGodRays(0.5f).setFxaa(true);
```

![A gallery of portable shaders: rust, wood, ice, fabric, hologram and fire](/blog/release-2026-11-20/gpu-shader-gallery.png)

The post-processing stack has bloom, color grading, vignette, film grain, FXAA, chromatic aberration, god rays, ambient occlusion, depth of field, outlines and color lookup tables, plus your own full-screen passes built from the same nodes. Image-based lighting bakes from the sky on the CPU into an ordinary texture, so reflections work even on old OpenGL ES and WebGL devices that lack cube maps. GPU skinning animates characters, instancing draws a forest in one call, and glTF 2.0 models load directly.

## Sound that comes from somewhere

The new `com.codename1.media.audio` package is an audio graph modeled on the W3C Web Audio API. Sources, filters, delay, compression, convolution reverb, analyzers, and custom processors that run per block on the audio thread. The graph is rendered by portable code, so it sounds the same on every platform.

For games, the important node is `PannerNode`. It places a sound in 3D around the listener, with distance models and sound cones, and HRTF rendering for headphones, so footsteps can come from behind you:

```java {class="cn1-no-try"}
AudioContext audio = AudioContext.create();
PannerNode panner = audio.createPanner();
panner.setPanningModel(PanningModel.HRTF);
panner.getPositionX().setValue(3);
panner.getPositionZ().setValue(-1);
OscillatorNode tone = audio.createOscillator();
tone.connect(panner).connect(audio.getDestination());
tone.start();
```

An `OfflineAudioContext` renders the same graph to memory, so your unit test hears exactly what the device plays.

## Remix it

Brickyard is a template. Turn it into a coding school where students build games in Luau, a branded platform for a toy company, a training simulator, or a creator economy for one specific genre. The parts are all there, and the hard ones are tested on every commit.

That wraps up this week's series. Start from the beginning with {{< post-link path="/blog/why-my-kids-are-learning-to-program" text="why my kids are learning to program" >}}.
