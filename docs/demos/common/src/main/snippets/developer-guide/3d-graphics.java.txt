// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::3d-graphics-java-001[]
RenderView view = new RenderView(new Renderer() {
    private final Camera camera = new Camera();
    private Mesh cube;
    private Material material;
    private float angle;

    public void onInit(GraphicsDevice device) {
        cube = Primitives.cube(device, 1.5f);
        material = new Material(Material.Type.PHONG)
                .setColor(0xff3366ff)
                .setShininess(24f);
        camera.setPerspective(45f, 0.1f, 100f)
              .setPosition(2.5f, 2f, 3.5f)
              .setTarget(0f, 0f, 0f);
        device.setLight(new Light().setDirection(-0.4f, -1f, -0.6f));
    }

    public void onResize(GraphicsDevice device, int w, int h) {
        camera.setAspect((float) w / Math.max(1, h));
        device.setViewport(0, 0, w, h);
    }

    public void onFrame(GraphicsDevice device) {
        angle += 0.02f;
        device.clear(0xff101018, true, true);
        device.setCamera(camera);
        device.draw(cube, material, Matrix4.rotation(angle, 0.3f, 1f, 0.1f));
    }

    public void onDispose(GraphicsDevice device) {
    }
});
view.setContinuous(true);            // animate; omit for on-demand rendering

Form hi = new Form("3D", new BorderLayout());
hi.add(BorderLayout.CENTER, view);
hi.show();
// end::3d-graphics-java-001[]

// tag::3d-graphics-java-002[]
Texture tex = device.createTexture(myImage);     // or createTexture(w, h, argb)
tex.setFilter(Texture.Filter.LINEAR).setWrap(Texture.Wrap.REPEAT);
Material m = new Material(Material.Type.UNLIT).setTexture(tex);
// end::3d-graphics-java-002[]

// tag::3d-graphics-java-003[]
VertexBuffer vb = device.createVertexBuffer(VertexFormat.POSITION_NORMAL_TEXCOORD, 4);
vb.setData(new float[] { /* px,py,pz, nx,ny,nz, u,v  per vertex ... */ });
IndexBuffer ib = device.createIndexBuffer(6);
ib.setData(new int[] { 0, 1, 2, 0, 2, 3 });
Mesh mesh = new Mesh(vb, ib, PrimitiveType.TRIANGLES);
// end::3d-graphics-java-003[]

// tag::3d-graphics-java-004[]
InputStream in = Display.getInstance().getResourceAsStream(getClass(), "/boombox.glb");
GltfLoader.GltfModel loaded = GltfLoader.loadModel(device, in);
Material material = new Material(Material.Type.PHONG).setShininess(16f);
if (loaded.getBaseColorTexture() != null) {
    material.setTexture(loaded.getBaseColorTexture());
}
Mesh model = loaded.getMesh();           // draw it like any other mesh
// end::3d-graphics-java-004[]

// tag::3d-graphics-java-005[]
if (CN.isGpuSupported()) {
    // GraphicsDevice.getCapabilities() exposes max texture size, shader level, etc.
}
// end::3d-graphics-java-005[]
