// A GPU peer can initialize before an embedded preview is attached. Keep it
// detached long enough for its first animation callback, then attach it normally.
export const gpuLifecycleScript = `
import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.util.UITimer;
import com.codename1.gpu.*;

class DetachedGpuView extends RenderView {
    public DetachedGpuView(Renderer renderer) { super(renderer); }
    public void prepare() { super.initComponent(); }
}
Form form = new Form("3D / GPU", new BorderLayout());
Camera camera = new Camera().setPerspective(60f, 0.1f, 100f)
        .setPosition(0f, 0f, 4f).setTarget(0f, 0f, 0f);
Mesh[] cube = new Mesh[1];
Material[] material = new Material[1];
float[] angle = {0f};
DetachedGpuView view = new DetachedGpuView(GpuScripting.renderer(camera,
    device -> {
        cube[0] = Primitives.cube(device, 1f);
        material[0] = new Material(Material.Type.PHONG).setColor(0xff3399ff);
    },
    device -> {
        angle[0] += 0.02f;
        device.clear(0xff10182a, true, true);
        device.setCamera(camera);
        device.draw(cube[0], material[0], Matrix4.rotation(angle[0], 0.4f, 1f, 0.2f));
    }));
view.setContinuous(true);
view.prepare();
form.show();
UITimer.timer(1500, false, Display.getInstance().getCurrent(), () -> {
    form.add(BorderLayout.CENTER, view);
    form.revalidate();
    System.out.println("[gpu-lifecycle] attached");
});
`;
