# Evidence map

Source: `docs/website/content/blog/portable-3d-graphics-api.md`
Canonical: https://www.codenameone.com/blog/portable-3d-graphics-api/

## Thesis

Describing materials once while generating shaders for Metal, OpenGL, WebGL, and Direct3D

## Supported beats

- **A cube in one screen of code:** RenderView is a regular Codename One component. It participates in layout, sits next to buttons and labels, and the surrounding form keeps working exactly as before. This is a textured variant of the same code running on an iPhone through Metal.
- **Real models, not just primitives:** Primitives gives you cubes, spheres, and friends for getting started, but real content comes from 3D tools. The GltfLoader reads binary glTF (.glb), the de-facto standard interchange format that Blender and practically every modern 3D tool export.
- **What runs where:** The simulator's fallback deserves a note: it is a complete depth-buffered, perspective-correct, textured, lit rasterizer written in plain Java. When OpenGL isn't available, headless CI machines for instance, rendering keeps working and stays deterministic, which is how our screenshot test suite gates 3D output on every platform.
- **Two levels of API:** The hybrid design goes deeper than materials. Most applications stay on the high level: Mesh, Material, Camera, Light, and Primitives, with Matrix4 providing the usual transform helpers (perspective, look-at, rotation, scaling, multiplication) as plain float[16] arrays with no object churn per frame.
- **Designed to degrade:** Not every environment has a GPU backend. RenderView.isSupported() tells you whether real 3D is available, and where it isn't the view renders a placeholder rather than failing.
- **This is a foundation:** A portable 3D API is useful on its own, for product viewers, data visualization, and the occasional spinning logo. But the reason it exists is tomorrow's post: a game development API that builds sprites, scenes, physics, and sound on top of this layer.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/pull/5151
- https://github.com/codenameone/CodenameOne/issues
