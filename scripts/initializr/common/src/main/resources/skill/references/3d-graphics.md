# Portable 3D / GPU Graphics Reference

`com.codename1.gpu` is a portable, **engine-managed-shader** 3D graphics surface. The headline: **you never write shader source.** You describe geometry (`Mesh` + `VertexFormat`) and surface appearance (`Material`) declaratively, and the engine generates the right shader for each platform (GLSL on OpenGL ES / WebGL, Metal Shading Language on iOS). The GPU surface is hosted by `RenderView`, an ordinary CN1 `Component`.

Use this for product viewers, data visualizations, custom 3D scenes, AR-style overlays — anything needing real GPU geometry. For 2D sprite games, prefer `com.codename1.gaming` (see `references/games.md`), which is built on top of this.

## The render loop

`new RenderView(Renderer)` drives four callbacks on the **GPU thread** (never touch CN1 UI from inside them). Allocate in `onInit`, configure projection in `onResize`, draw in `onFrame`:

```java
import com.codename1.gpu.*;
import com.codename1.ui.layouts.BorderLayout;

RenderView view = new RenderView(new Renderer() {
    private final Camera camera = new Camera();
    private Mesh cube;
    private Material material;

    public void onInit(GraphicsDevice device) {
        cube = Primitives.cube(device, 1.6f);
        material = new Material(Material.Type.PHONG).setColor(0xff3366ff).setShininess(24f);
        camera.setPerspective(45f, 0.1f, 100f)
              .setPosition(2.6f, 2.1f, 3.4f)
              .setTarget(0f, 0f, 0f);
        device.setLight(new Light().setDirection(-0.4f, -1f, -0.55f));
    }

    public void onResize(GraphicsDevice device, int w, int h) {
        camera.setAspect((float) w / Math.max(1, h));
        device.setViewport(0, 0, w, h);
    }

    public void onFrame(GraphicsDevice device) {
        device.clear(0xff101018, true, true);          // color, clearColor?, clearDepth?
        device.setCamera(camera);
        float[] model = Matrix4.rotation((float) Math.toRadians(25), 0.35f, 1f, 0.12f);
        device.draw(cube, material, model);            // null model = identity
    }

    public void onDispose(GraphicsDevice device) { }
});

if (view.isSupported()) {                               // false where there is no GPU backend
    view.setContinuous(true);                           // animate every frame; else call requestRender()
    form.add(BorderLayout.CENTER, view);
}
```

**Always gate on `view.isSupported()`.** When it's `false` the view shows a placeholder instead of crashing — branch to a 2D fallback if 3D is essential to the screen.

## Class map

| Role | Class | Key members |
| --- | --- | --- |
| Host component | `RenderView` | `new RenderView(Renderer)`, `isSupported()`, `setContinuous(boolean)`, `requestRender()`, `getRenderer()` |
| Your callbacks | `Renderer` (interface) | `onInit` / `onResize(w,h)` / `onFrame` / `onDispose`, all taking `GraphicsDevice` |
| Command surface | `GraphicsDevice` | `createVertexBuffer/IndexBuffer/Texture`, `clear`, `setViewport`, `setCamera`, `setLight`, `draw(mesh, material, modelMatrix)`, `dispose(...)`, `getCapabilities()` |
| Geometry | `Mesh` | `new Mesh(vb, primitiveType)` or `new Mesh(vb, ib, primitiveType)`; `isIndexed()` |
| Vertex data | `VertexBuffer` / `IndexBuffer` | `getData()` (write directly, then `setDirty()`), `setData(...)`; indices are 16-bit unsigned (max 65536 verts) |
| Layout | `VertexFormat` / `VertexAttribute` | presets `POSITION`, `POSITION_TEXCOORD`, `POSITION_NORMAL`, `POSITION_NORMAL_TEXCOORD`; `VertexAttribute.Usage` = POSITION/NORMAL/TEXCOORD/COLOR |
| Primitive kind | `PrimitiveType` | POINTS, LINES, LINE_STRIP, TRIANGLES, TRIANGLE_STRIP |
| Appearance | `Material` | `Type` UNLIT/LAMBERT/PHONG/SPRITE/SKYBOX; `setColor(argb)`, `setTexture(t)`, `setShininess(f)`, `setRenderState(rs)` |
| Pipeline state | `RenderState` | `opaque()` / `transparent()`; `setDepthTest`, `setDepthWrite`, `setBlendMode` (NONE/ALPHA/ADDITIVE), `setCullMode` (BACK/FRONT/NONE) |
| Lighting | `Light` | one directional light + ambient: `setDirection(x,y,z)`, `setColor(argb)`, `setAmbientColor(argb)` |
| View/projection | `Camera` | `setPerspective(fovDeg, near, far)` / `setOrthographic(...)`, `setAspect`, `setPosition`/`setTarget`/`setUp` |
| Math | `Matrix4` | `float[16]` helpers: `identity`, `multiply(a,b,dst)`, `perspective`, `ortho`, `lookAt`, `translation`, `scaling`, `rotation`, `invert`, `normalMatrix` |
| Built-in meshes | `Primitives` | `cube(device, size)`, `quad(device, size)` (both `POSITION_NORMAL_TEXCOORD`) |
| Model loading | `GltfLoader` | `load(device, bytes/stream)` → `Mesh`; `loadModel(...)` → `GltfModel` (`getMesh()` + `getBaseColorTexture()`) |
| Textures | `Texture` | `setWrap(CLAMP/REPEAT)`, `setFilter(NEAREST/LINEAR)`; create via `device.createTexture(image)` |
| Capabilities | `GpuCapabilities` | `getMaxTextureSize()`, `isShaderLevel3()`, `isDepthTextureSupported()`, `isIntIndicesSupported()`, `getRendererName()` |

Prefer `device.createVertexBuffer(...)` / `createIndexBuffer(...)` / `createTexture(...)` over the raw constructors — they return device-owned, SIMD-aligned resources the device disposes for you.

## Loading a glTF model

`GltfLoader` reads `.glb` (binary) and `.gltf` (JSON). It pulls POSITION (required), NORMAL (computed if absent), and TEXCOORD_0 into the standard `POSITION_NORMAL_TEXCOORD` layout; materials beyond base-color texture, skinning, and animation are ignored.

```java
public void onInit(GraphicsDevice device) {
    InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/boombox.glb");
    GltfLoader.GltfModel loaded = GltfLoader.loadModel(device, in);
    mesh = loaded.getMesh();
    material = new Material(Material.Type.PHONG).setShininess(16f);
    Texture tex = loaded.getBaseColorTexture();
    if (tex != null) material.setTexture(tex);

    camera.setPerspective(45f, 0.1f, 100f).setPosition(1.9f, 1.5f, 2.6f).setTarget(0,0,0);
    device.setLight(new Light().setDirection(-0.4f, -0.7f, -0.6f));
}
```

Bundle the `.glb` at the top of `common/src/main/resources/` (flat namespace — see `references/java-api-subset.md`).

## Building custom geometry

```java
VertexBuffer vb = device.createVertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, vertexCount);
float[] data = vb.getData();        // interleaved per-vertex floats; write in place
// ... fill data ...
vb.setDirty();                      // mark for re-upload after writing
IndexBuffer ib = device.createIndexBuffer(indexCount);
ib.setData(intIndices);             // validated to 0..65535
Mesh mesh = new Mesh(vb, ib, PrimitiveType.TRIANGLES);
```

For transforms, compose `Matrix4` helpers (column-major `float[16]`); the destination of `multiply(a, b, dst)` must not alias `a` or `b`.

## Platform support

| Platform | Backend |
| --- | --- |
| Android | OpenGL ES 2+ (`GLSurfaceView`) |
| iOS | Metal (`MTKView`) |
| JavaScript / web | WebGL 1.0+ |
| JavaSE simulator | Real OpenGL via JOGL when present; pure-Java software rasterizer fallback otherwise |

Backends differ in capability — query `device.getCapabilities()` before using advanced features: `isShaderLevel3()` (GLSL ES 3 / WebGL2), `isDepthTextureSupported()` (shadow maps), `isIntIndicesSupported()` (32-bit indices; WebGL 1 is 16-bit only), `getMaxTextureSize()`. No build hint or extra dependency is required; the API is part of `codenameone-core`.

## What NOT to do

- Don't touch CN1 UI (`Form`, components, `callSerially` targets) from a `Renderer` callback — they run on the GPU thread, not the EDT.
- Don't skip `isSupported()` — assume some target has no GPU and provide a fallback.
- Don't write GLSL/Metal by hand — express it through `Material` + `VertexFormat`; there is no hook for raw shader source.
- Don't exceed 65536 vertices per `IndexBuffer` unless `isIntIndicesSupported()` is true.
- Don't allocate meshes/textures in `onFrame` — create them once in `onInit` and reuse.
- Don't forget `setDirty()` after writing into a buffer's backing array, or the GPU keeps the stale copy.
