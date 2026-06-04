package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GltfLoader;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Light;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.RenderView;
import com.codename1.gpu.Renderer;
import com.codename1.gpu.Texture;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.UITimer;
import com.codename1.util.Base64;

/// End-to-end screenshot test for loading a real authored model through the
/// portable 3D mesh-loading API (`GltfLoader`). Rather than a built in
/// primitive, this renders a textured torus (320 triangles with smooth normals
/// and seamless UVs) loaded from an embedded binary glTF (`.glb`) asset, lit with
/// a Phong material and a checkerboard texture. The model bytes are embedded as
/// base64 so the test has no external asset dependency and stays deterministic
/// across every platform; decoding them yields a valid `.glb` that any glTF
/// viewer could open. The torus is drawn at a fixed orientation for a stable
/// capture.
public class Gpu3DModelScreenshotTest extends BaseTest {
    private RenderView view;

    @Override
    public boolean runTest() {
        Form form = createForm("3D Model", new BorderLayout(), "Gpu3DModel");
        view = new RenderView(new Renderer() {
            private final Camera camera = new Camera();
            private Mesh model;
            private Material material;

            public void onInit(GraphicsDevice device) {
                model = GltfLoader.load(device, Base64.decode(TORUS_GLB_BASE64.getBytes()));
                Texture tex = device.createTexture(64, 64, checker());
                tex.setFilter(Texture.Filter.NEAREST);
                // The torus UVs tile (0..6 around the ring, 0..2 around the
                // tube), so the texture must repeat rather than clamp.
                tex.setWrap(Texture.Wrap.REPEAT);
                material = new Material(Material.Type.PHONG)
                        .setTexture(tex)
                        .setShininess(20f);
                camera.setPerspective(45f, 0.1f, 100f)
                        .setPosition(3.0f, 2.6f, 4.0f)
                        .setTarget(0f, 0f, 0f);
                device.setLight(new Light().setDirection(-0.4f, -1f, -0.55f));
            }

            public void onResize(GraphicsDevice device, int width, int height) {
                camera.setAspect((float) width / Math.max(1, height));
                device.setViewport(0, 0, width, height);
            }

            public void onFrame(GraphicsDevice device) {
                device.clear(0xff101018, true, true);
                device.setCamera(camera);
                float[] m = Matrix4.rotation((float) Math.toRadians(35), 0.25f, 1f, 0.1f);
                device.draw(model, material, m);
            }

            public void onDispose(GraphicsDevice device) {
            }
        });
        if (view.isSupported()) {
            form.add(BorderLayout.CENTER, view);
        } else {
            form.add(BorderLayout.CENTER, new Label("3D unsupported"));
        }
        form.show();
        return true;
    }

    /// Force a fresh GPU frame before the screenshot so a cold GL surface cannot
    /// produce a blank capture. See Gpu3DCubeScreenshotTest.
    @Override
    protected void registerReadyCallback(Form parent, final Runnable run) {
        UITimer.timer(1000, false, parent, new Runnable() {
            public void run() {
                if (view != null) {
                    view.requestRender();
                }
                UITimer.timer(500, false, parent, run);
            }
        });
    }

    private static int[] checker() {
        int[] px = new int[64 * 64];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                boolean c = ((x / 8) + (y / 8)) % 2 == 0;
                px[y * 64 + x] = c ? 0xffffcc44 : 0xff2266cc;
            }
        }
        return px;
    }

    // A binary glTF (.glb) torus: 320 triangles, smooth normals, tiled UVs.
    // Decoding this base64 yields a spec-valid .glb openable in any glTF viewer.
    private static final String TORUS_GLB_BASE64 =
              "Z2xURgIAAABgIgAAZAMAAEpTT057ImFzc2V0Ijp7InZlcnNpb24iOiIyLjAiLCJnZW5lcmF0b3IiOiJjbjEtdG9ydXMtZ2VuIn0s"
            + "InNjZW5lIjowLCJzY2VuZXMiOlt7Im5vZGVzIjpbMF19XSwibm9kZXMiOlt7Im1lc2giOjB9XSwibWVzaGVzIjpbeyJwcmltaXRp"
            + "dmVzIjpbeyJhdHRyaWJ1dGVzIjp7IlBPU0lUSU9OIjowLCJOT1JNQUwiOjEsIlRFWENPT1JEXzAiOjJ9LCJpbmRpY2VzIjozLCJt"
            + "b2RlIjo0fV19XSwiYnVmZmVycyI6W3siYnl0ZUxlbmd0aCI6NzkwNH1dLCJidWZmZXJWaWV3cyI6W3siYnVmZmVyIjowLCJieXRl"
            + "T2Zmc2V0IjowLCJieXRlTGVuZ3RoIjoyMjQ0LCJ0YXJnZXQiOjM0OTYyfSx7ImJ1ZmZlciI6MCwiYnl0ZU9mZnNldCI6MjI0NCwi"
            + "Ynl0ZUxlbmd0aCI6MjI0NCwidGFyZ2V0IjozNDk2Mn0seyJidWZmZXIiOjAsImJ5dGVPZmZzZXQiOjQ0ODgsImJ5dGVMZW5ndGgi"
            + "OjE0OTYsInRhcmdldCI6MzQ5NjJ9LHsiYnVmZmVyIjowLCJieXRlT2Zmc2V0Ijo1OTg0LCJieXRlTGVuZ3RoIjoxOTIwLCJ0YXJn"
            + "ZXQiOjM0OTYzfV0sImFjY2Vzc29ycyI6W3siYnVmZmVyVmlldyI6MCwiY29tcG9uZW50VHlwZSI6NTEyNiwiY291bnQiOjE4Nywi"
            + "dHlwZSI6IlZFQzMiLCJtaW4iOlstMS40MiwtMC4zOTk0NDM3MzY4NDM5NjQ1LC0xLjQyXSwibWF4IjpbMS40MiwwLjM5OTQ0Mzcz"
            + "Njg0Mzk2NDUsMS40Ml19LHsiYnVmZmVyVmlldyI6MSwiY29tcG9uZW50VHlwZSI6NTEyNiwiY291bnQiOjE4NywidHlwZSI6IlZF"
            + "QzMifSx7ImJ1ZmZlclZpZXciOjIsImNvbXBvbmVudFR5cGUiOjUxMjYsImNvdW50IjoxODcsInR5cGUiOiJWRUMyIn0seyJidWZm"
            + "ZXJWaWV3IjozLCJjb21wb25lbnRUeXBlIjo1MTIzLCJjb3VudCI6OTYwLCJ0eXBlIjoiU0NBTEFSIn1dfSAg4B4AAEJJTgCPwrU/"
            + "AAAAAAAAAAAlfqs/cMt8PgAAAADdnJA/5IPMPgAAAABFxl4/5IPMPgAAAAC2Ayk/cMt8PgAAAADhehQ/7TNtJAAAAAC2Ayk/cMt8"
            + "vgAAAABFxl4/5IPMvgAAAADdnJA/5IPMvgAAAAAlfqs/cMt8vgAAAACPwrU/7TPtpAAAAACi7Kc/AAAAAPMcCz9LcJ4/cMt8Pj9B"
            + "Az/SmoU/5IPMPhZd3T4Z0U0/5IPMPhSBqj4nJhw/cMt8PqxbgT55LQk/7TNtJIlIYz4nJhw/cMt8vqxbgT4Z0U0/5IPMvhSBqj7S"
            + "moU/5IPMvhZd3T5LcJ4/cMt8vj9BAz+i7Kc/7TPtpPMcCz8ThoA/AAAAABOGgD8Ch3I/cMt8PgKHcj9pg0w/5IPMPmmDTD9+hh0/"
            + "5IPMPn6GHT/IBe8+cMt8PsgF7z6B+9E+7TNtJIH70T7IBe8+cMt8vsgF7z5+hh0/5IPMvn6GHT9pg0w/5IPMvmmDTD8Ch3I/cMt8"
            + "vgKHcj8ThoA/7TPtpBOGgD/zHAs/AAAAAKLspz8/QQM/cMt8Pktwnj8WXd0+5IPMPtKahT8Ugao+5IPMPhnRTT+sW4E+cMt8Picm"
            + "HD+JSGM+7TNtJHktCT+sW4E+cMt8vicmHD8Ugao+5IPMvhnRTT8WXd0+5IPMvtKahT8/QQM/cMt8vktwnj/zHAs/7TPtpKLspz8t"
            + "fsgkAAAAAI/CtT/eKr0kcMt8PiV+qz9ghJ8k5IPMPt2ckD8GvHUk5IPMPkXGXj8LbzokcMt8PrYDKT9tyCMk7TNtJOF6FD8Lbzok"
            + "cMt8vrYDKT8GvHUk5IPMvkXGXj9ghJ8k5IPMvt2ckD/eKr0kcMt8viV+qz8tfsgk7TPtpI/CtT/zHAu/AAAAAKLspz8/QQO/cMt8"
            + "Pktwnj8WXd2+5IPMPtKahT8Ugaq+5IPMPhnRTT+sW4G+cMt8PicmHD+JSGO+7TNtJHktCT+sW4G+cMt8vicmHD8Ugaq+5IPMvhnR"
            + "TT8WXd2+5IPMvtKahT8/QQO/cMt8vktwnj/zHAu/7TPtpKLspz8ThoC/AAAAABOGgD8Ch3K/cMt8PgKHcj9pg0y/5IPMPmmDTD9+"
            + "hh2/5IPMPn6GHT/IBe++cMt8PsgF7z6B+9G+7TNtJIH70T7IBe++cMt8vsgF7z5+hh2/5IPMvn6GHT9pg0y/5IPMvmmDTD8Ch3K/"
            + "cMt8vgKHcj8ThoC/7TPtpBOGgD+i7Ke/AAAAAPMcCz9LcJ6/cMt8Pj9BAz/SmoW/5IPMPhZd3T4Z0U2/5IPMPhSBqj4nJhy/cMt8"
            + "PqxbgT55LQm/7TNtJIlIYz4nJhy/cMt8vqxbgT4Z0U2/5IPMvhSBqj7SmoW/5IPMvhZd3T5LcJ6/cMt8vj9BAz+i7Ke/7TPtpPMc"
            + "Cz+PwrW/AAAAAC1+SCUlfqu/cMt8Pt4qPSXdnJC/5IPMPmCEHyVFxl6/5IPMPga89SS2Aym/cMt8PgtvuiThehS/7TNtJG3IoyS2"
            + "Aym/cMt8vgtvuiRFxl6/5IPMvga89STdnJC/5IPMvmCEHyUlfqu/cMt8vt4qPSWPwrW/7TPtpC1+SCWi7Ke/AAAAAPMcC79LcJ6/"
            + "cMt8Pj9BA7/SmoW/5IPMPhZd3b4Z0U2/5IPMPhSBqr4nJhy/cMt8Pqxbgb55LQm/7TNtJIlIY74nJhy/cMt8vqxbgb4Z0U2/5IPM"
            + "vhSBqr7SmoW/5IPMvhZd3b5LcJ6/cMt8vj9BA7+i7Ke/7TPtpPMcC78ThoC/AAAAABOGgL8Ch3K/cMt8PgKHcr9pg0y/5IPMPmmD"
            + "TL9+hh2/5IPMPn6GHb/IBe++cMt8PsgF776B+9G+7TNtJIH70b7IBe++cMt8vsgF775+hh2/5IPMvn6GHb9pg0y/5IPMvmmDTL8C"
            + "h3K/cMt8vgKHcr8ThoC/7TPtpBOGgL/zHAu/AAAAAKLsp78/QQO/cMt8Pktwnr8WXd2+5IPMPtKahb8Ugaq+5IPMPhnRTb+sW4G+"
            + "cMt8PicmHL+JSGO+7TNtJHktCb+sW4G+cMt8vicmHL8Ugaq+5IPMvhnRTb8WXd2+5IPMvtKahb8/QQO/cMt8vktwnr/zHAu/7TPt"
            + "pKLsp7+iXpalAAAAAI/Ctb8m4I2lcMt8PiV+q7+QRm+l5IPMPt2ckL8FTTil5IPMPkXGXr9I0wulcMt8PrYDKb+jrPWk7TNtJOF6"
            + "FL9I0wulcMt8vrYDKb8FTTil5IPMvkXGXr+QRm+l5IPMvt2ckL8m4I2lcMt8viV+q7+iXpal7TPtpI/Ctb/zHAs/AAAAAKLsp78/"
            + "QQM/cMt8Pktwnr8WXd0+5IPMPtKahb8Ugao+5IPMPhnRTb+sW4E+cMt8PicmHL+JSGM+7TNtJHktCb+sW4E+cMt8vicmHL8Ugao+"
            + "5IPMvhnRTb8WXd0+5IPMvtKahb8/QQM/cMt8vktwnr/zHAs/7TPtpKLsp78ThoA/AAAAABOGgL8Ch3I/cMt8PgKHcr9pg0w/5IPM"
            + "PmmDTL9+hh0/5IPMPn6GHb/IBe8+cMt8PsgF776B+9E+7TNtJIH70b7IBe8+cMt8vsgF775+hh0/5IPMvn6GHb9pg0w/5IPMvmmD"
            + "TL8Ch3I/cMt8vgKHcr8ThoA/7TPtpBOGgL+i7Kc/AAAAAPMcC79LcJ4/cMt8Pj9BA7/SmoU/5IPMPhZd3b4Z0U0/5IPMPhSBqr4n"
            + "Jhw/cMt8Pqxbgb55LQk/7TNtJIlIY74nJhw/cMt8vqxbgb4Z0U0/5IPMvhSBqr7SmoU/5IPMvhZd3b5LcJ4/cMt8vj9BA7+i7Kc/"
            + "7TPtpPMcC7+PwrU/AAAAAC1+yKUlfqs/cMt8Pt4qvaXdnJA/5IPMPmCEn6VFxl4/5IPMPga8daW2Ayk/cMt8PgtvOqXhehQ/7TNt"
            + "JG3II6W2Ayk/cMt8vgtvOqVFxl4/5IPMvga8daXdnJA/5IPMvmCEn6Ulfqs/cMt8vt4qvaWPwrU/7TPtpC1+yKUAAIA/AAAAAAAA"
            + "AAC9G08/GHkWPwAAAAB6N54+cXhzPwAAAAB6N56+cXhzPwAAAIC9G0+/GHkWPwAAAIAAAIC/MjENJQAAAIC9G0+/GHkWvwAAAIB6"
            + "N56+cXhzvwAAAIB6N54+cXhzvwAAAAC9G08/GHkWvwAAAAAAAIA/MjGNpQAAAABeg2w/AAAAABXvwz7aVz8/GHkWP42Dnj5VLJI+"
            + "cXhzPwkw8j1VLJK+cXhzPwkw8r3aVz+/GHkWP42Dnr5eg2y/MjENJRXvw77aVz+/GHkWv42Dnr5VLJK+cXhzvwkw8r1VLJI+cXhz"
            + "vwkw8j3aVz8/GHkWv42Dnj5eg2w/MjGNpRXvwz7zBDU/AAAAAPMENT+echI/GHkWP55yEj+QwF8+cXhzP5DAXz6QwF++cXhzP5DA"
            + "X76echK/GHkWP55yEr/zBDW/MjENJfMENb+echK/GHkWv55yEr+QwF++cXhzv5DAX76QwF8+cXhzv5DAXz6echI/GHkWv55yEj/z"
            + "BDU/MjGNpfMENT8V78M+AAAAAF6DbD+Ng54+GHkWP9pXPz8JMPI9cXhzP1Uskj4JMPK9cXhzP1Uskr6Ng56+GHkWP9pXP78V78O+"
            + "MjENJV6DbL+Ng56+GHkWv9pXP78JMPK9cXhzv1Uskr4JMPI9cXhzv1Uskj6Ng54+GHkWv9pXPz8V78M+MjGNpV6DbD8yMY0kAAAA"
            + "AAAAgD8ndGQkGHkWP70bTz/rha4jcXhzP3o3nj7rha6jcXhzP3o3nr4ndGSkGHkWP70bT78yMY2kMjENJQAAgL8ndGSkGHkWv70b"
            + "T7/rha6jcXhzv3o3nr7rha4jcXhzv3o3nj4ndGQkGHkWv70bTz8yMY0kMjGNpQAAgD8V78O+AAAAAF6DbD+Ng56+GHkWP9pXPz8J"
            + "MPK9cXhzP1Uskj4JMPI9cXhzP1Uskr6Ng54+GHkWP9pXP78V78M+MjENJV6DbL+Ng54+GHkWv9pXP78JMPI9cXhzv1Uskr4JMPK9"
            + "cXhzv1Uskj6Ng56+GHkWv9pXPz8V78O+MjGNpV6DbD/zBDW/AAAAAPMENT+echK/GHkWP55yEj+QwF++cXhzP5DAXz6QwF8+cXhz"
            + "P5DAX76echI/GHkWP55yEr/zBDU/MjENJfMENb+echI/GHkWv55yEr+QwF8+cXhzv5DAX76QwF++cXhzv5DAXz6echK/GHkWv55y"
            + "Ej/zBDW/MjGNpfMENT9eg2y/AAAAABXvwz7aVz+/GHkWP42Dnj5VLJK+cXhzPwkw8j1VLJI+cXhzPwkw8r3aVz8/GHkWP42Dnr5e"
            + "g2w/MjENJRXvw77aVz8/GHkWv42Dnr5VLJI+cXhzvwkw8r1VLJK+cXhzvwkw8j3aVz+/GHkWv42Dnj5eg2y/MjGNpRXvwz4AAIC/"
            + "AAAAADIxDSW9G0+/GHkWPyd05CR6N56+cXhzP+uFLiR6N54+cXhzP+uFLqS9G08/GHkWPyd05KQAAIA/MjENJTIxDaW9G08/GHkW"
            + "vyd05KR6N54+cXhzv+uFLqR6N56+cXhzv+uFLiS9G0+/GHkWvyd05CQAAIC/MjGNpTIxDSVeg2y/AAAAABXvw77aVz+/GHkWP42D"
            + "nr5VLJK+cXhzPwkw8r1VLJI+cXhzPwkw8j3aVz8/GHkWP42Dnj5eg2w/MjENJRXvwz7aVz8/GHkWv42Dnj5VLJI+cXhzvwkw8j1V"
            + "LJK+cXhzvwkw8r3aVz+/GHkWv42Dnr5eg2y/MjGNpRXvw77zBDW/AAAAAPMENb+echK/GHkWP55yEr+QwF++cXhzP5DAX76QwF8+"
            + "cXhzP5DAXz6echI/GHkWP55yEj/zBDU/MjENJfMENT+echI/GHkWv55yEj+QwF8+cXhzv5DAXz6QwF++cXhzv5DAX76echK/GHkW"
            + "v55yEr/zBDW/MjGNpfMENb8V78O+AAAAAF6DbL+Ng56+GHkWP9pXP78JMPK9cXhzP1Uskr4JMPI9cXhzP1Uskj6Ng54+GHkWP9pX"
            + "Pz8V78M+MjENJV6DbD+Ng54+GHkWv9pXPz8JMPI9cXhzv1Uskj4JMPK9cXhzv1Uskr6Ng56+GHkWv9pXP78V78O+MjGNpV6DbL/K"
            + "yVOlAAAAAAAAgL8dVyulGHkWP70bT79w5IKkcXhzP3o3nr5w5IIkcXhzP3o3nj4dVyslGHkWP70bTz/KyVMlMjENJQAAgD8dVysl"
            + "GHkWv70bTz9w5IIkcXhzv3o3nj5w5IKkcXhzv3o3nr4dVyulGHkWv70bT7/KyVOlMjGNpQAAgL8V78M+AAAAAF6DbL+Ng54+GHkW"
            + "P9pXP78JMPI9cXhzP1Uskr4JMPK9cXhzP1Uskj6Ng56+GHkWP9pXPz8V78O+MjENJV6DbD+Ng56+GHkWv9pXPz8JMPK9cXhzv1Us"
            + "kj4JMPI9cXhzv1Uskr6Ng54+GHkWv9pXP78V78M+MjGNpV6DbL/zBDU/AAAAAPMENb+echI/GHkWP55yEr+QwF8+cXhzP5DAX76Q"
            + "wF++cXhzP5DAXz6echK/GHkWP55yEj/zBDW/MjENJfMENT+echK/GHkWv55yEj+QwF++cXhzv5DAXz6QwF8+cXhzv5DAX76echI/"
            + "GHkWv55yEr/zBDU/MjGNpfMENb9eg2w/AAAAABXvw77aVz8/GHkWP42Dnr5VLJI+cXhzPwkw8r1VLJK+cXhzPwkw8j3aVz+/GHkW"
            + "P42Dnj5eg2y/MjENJRXvwz7aVz+/GHkWv42Dnj5VLJK+cXhzvwkw8j1VLJI+cXhzvwkw8r3aVz8/GHkWv42Dnr5eg2w/MjGNpRXv"
            + "w74AAIA/AAAAADIxjaW9G08/GHkWPyd0ZKV6N54+cXhzP+uFrqR6N56+cXhzP+uFriS9G0+/GHkWPyd0ZCUAAIC/MjENJTIxjSW9"
            + "G0+/GHkWvyd0ZCV6N56+cXhzv+uFriR6N54+cXhzv+uFrqS9G08/GHkWvyd0ZKUAAIA/MjGNpTIxjaUAAAAAAAAAAAAAAADNzEw+"
            + "AAAAAM3MzD4AAAAAmpkZPwAAAADNzEw/AAAAAAAAgD8AAAAAmpmZPwAAAAAzM7M/AAAAAM3MzD8AAAAAZmbmPwAAAAAAAABAAADA"
            + "PgAAAAAAAMA+zcxMPgAAwD7NzMw+AADAPpqZGT8AAMA+zcxMPwAAwD4AAIA/AADAPpqZmT8AAMA+MzOzPwAAwD7NzMw/AADAPmZm"
            + "5j8AAMA+AAAAQAAAQD8AAAAAAABAP83MTD4AAEA/zczMPgAAQD+amRk/AABAP83MTD8AAEA/AACAPwAAQD+amZk/AABAPzMzsz8A"
            + "AEA/zczMPwAAQD9mZuY/AABAPwAAAEAAAJA/AAAAAAAAkD/NzEw+AACQP83MzD4AAJA/mpkZPwAAkD/NzEw/AACQPwAAgD8AAJA/"
            + "mpmZPwAAkD8zM7M/AACQP83MzD8AAJA/ZmbmPwAAkD8AAABAAADAPwAAAAAAAMA/zcxMPgAAwD/NzMw+AADAP5qZGT8AAMA/zcxM"
            + "PwAAwD8AAIA/AADAP5qZmT8AAMA/MzOzPwAAwD/NzMw/AADAP2Zm5j8AAMA/AAAAQAAA8D8AAAAAAADwP83MTD4AAPA/zczMPgAA"
            + "8D+amRk/AADwP83MTD8AAPA/AACAPwAA8D+amZk/AADwPzMzsz8AAPA/zczMPwAA8D9mZuY/AADwPwAAAEAAABBAAAAAAAAAEEDN"
            + "zEw+AAAQQM3MzD4AABBAmpkZPwAAEEDNzEw/AAAQQAAAgD8AABBAmpmZPwAAEEAzM7M/AAAQQM3MzD8AABBAZmbmPwAAEEAAAABA"
            + "AAAoQAAAAAAAAChAzcxMPgAAKEDNzMw+AAAoQJqZGT8AAChAzcxMPwAAKEAAAIA/AAAoQJqZmT8AAChAMzOzPwAAKEDNzMw/AAAo"
            + "QGZm5j8AAChAAAAAQAAAQEAAAAAAAABAQM3MTD4AAEBAzczMPgAAQECamRk/AABAQM3MTD8AAEBAAACAPwAAQECamZk/AABAQDMz"
            + "sz8AAEBAzczMPwAAQEBmZuY/AABAQAAAAEAAAFhAAAAAAAAAWEDNzEw+AABYQM3MzD4AAFhAmpkZPwAAWEDNzEw/AABYQAAAgD8A"
            + "AFhAmpmZPwAAWEAzM7M/AABYQM3MzD8AAFhAZmbmPwAAWEAAAABAAABwQAAAAAAAAHBAzcxMPgAAcEDNzMw+AABwQJqZGT8AAHBA"
            + "zcxMPwAAcEAAAIA/AABwQJqZmT8AAHBAMzOzPwAAcEDNzMw/AABwQGZm5j8AAHBAAAAAQAAAhEAAAAAAAACEQM3MTD4AAIRAzczM"
            + "PgAAhECamRk/AACEQM3MTD8AAIRAAACAPwAAhECamZk/AACEQDMzsz8AAIRAzczMPwAAhEBmZuY/AACEQAAAAEAAAJBAAAAAAAAA"
            + "kEDNzEw+AACQQM3MzD4AAJBAmpkZPwAAkEDNzEw/AACQQAAAgD8AAJBAmpmZPwAAkEAzM7M/AACQQM3MzD8AAJBAZmbmPwAAkEAA"
            + "AABAAACcQAAAAAAAAJxAzcxMPgAAnEDNzMw+AACcQJqZGT8AAJxAzcxMPwAAnEAAAIA/AACcQJqZmT8AAJxAMzOzPwAAnEDNzMw/"
            + "AACcQGZm5j8AAJxAAAAAQAAAqEAAAAAAAACoQM3MTD4AAKhAzczMPgAAqECamRk/AACoQM3MTD8AAKhAAACAPwAAqECamZk/AACo"
            + "QDMzsz8AAKhAzczMPwAAqEBmZuY/AACoQAAAAEAAALRAAAAAAAAAtEDNzEw+AAC0QM3MzD4AALRAmpkZPwAAtEDNzEw/AAC0QAAA"
            + "gD8AALRAmpmZPwAAtEAzM7M/AAC0QM3MzD8AALRAZmbmPwAAtEAAAABAAADAQAAAAAAAAMBAzcxMPgAAwEDNzMw+AADAQJqZGT8A"
            + "AMBAzcxMPwAAwEAAAIA/AADAQJqZmT8AAMBAMzOzPwAAwEDNzMw/AADAQGZm5j8AAMBAAAAAQAAACwAMAAAADAABAAEADAANAAEA"
            + "DQACAAIADQAOAAIADgADAAMADgAPAAMADwAEAAQADwAQAAQAEAAFAAUAEAARAAUAEQAGAAYAEQASAAYAEgAHAAcAEgATAAcAEwAI"
            + "AAgAEwAUAAgAFAAJAAkAFAAVAAkAFQAKAAsAFgAXAAsAFwAMAAwAFwAYAAwAGAANAA0AGAAZAA0AGQAOAA4AGQAaAA4AGgAPAA8A"
            + "GgAbAA8AGwAQABAAGwAcABAAHAARABEAHAAdABEAHQASABIAHQAeABIAHgATABMAHgAfABMAHwAUABQAHwAgABQAIAAVABYAIQAi"
            + "ABYAIgAXABcAIgAjABcAIwAYABgAIwAkABgAJAAZABkAJAAlABkAJQAaABoAJQAmABoAJgAbABsAJgAnABsAJwAcABwAJwAoABwA"
            + "KAAdAB0AKAApAB0AKQAeAB4AKQAqAB4AKgAfAB8AKgArAB8AKwAgACEALAAtACEALQAiACIALQAuACIALgAjACMALgAvACMALwAk"
            + "ACQALwAwACQAMAAlACUAMAAxACUAMQAmACYAMQAyACYAMgAnACcAMgAzACcAMwAoACgAMwA0ACgANAApACkANAA1ACkANQAqACoA"
            + "NQA2ACoANgArACwANwA4ACwAOAAtAC0AOAA5AC0AOQAuAC4AOQA6AC4AOgAvAC8AOgA7AC8AOwAwADAAOwA8ADAAPAAxADEAPAA9"
            + "ADEAPQAyADIAPQA+ADIAPgAzADMAPgA/ADMAPwA0ADQAPwBAADQAQAA1ADUAQABBADUAQQA2ADcAQgBDADcAQwA4ADgAQwBEADgA"
            + "RAA5ADkARABFADkARQA6ADoARQBGADoARgA7ADsARgBHADsARwA8ADwARwBIADwASAA9AD0ASABJAD0ASQA+AD4ASQBKAD4ASgA/"
            + "AD8ASgBLAD8ASwBAAEAASwBMAEAATABBAEIATQBOAEIATgBDAEMATgBPAEMATwBEAEQATwBQAEQAUABFAEUAUABRAEUAUQBGAEYA"
            + "UQBSAEYAUgBHAEcAUgBTAEcAUwBIAEgAUwBUAEgAVABJAEkAVABVAEkAVQBKAEoAVQBWAEoAVgBLAEsAVgBXAEsAVwBMAE0AWABZ"
            + "AE0AWQBOAE4AWQBaAE4AWgBPAE8AWgBbAE8AWwBQAFAAWwBcAFAAXABRAFEAXABdAFEAXQBSAFIAXQBeAFIAXgBTAFMAXgBfAFMA"
            + "XwBUAFQAXwBgAFQAYABVAFUAYABhAFUAYQBWAFYAYQBiAFYAYgBXAFgAYwBkAFgAZABZAFkAZABlAFkAZQBaAFoAZQBmAFoAZgBb"
            + "AFsAZgBnAFsAZwBcAFwAZwBoAFwAaABdAF0AaABpAF0AaQBeAF4AaQBqAF4AagBfAF8AagBrAF8AawBgAGAAawBsAGAAbABhAGEA"
            + "bABtAGEAbQBiAGMAbgBvAGMAbwBkAGQAbwBwAGQAcABlAGUAcABxAGUAcQBmAGYAcQByAGYAcgBnAGcAcgBzAGcAcwBoAGgAcwB0"
            + "AGgAdABpAGkAdAB1AGkAdQBqAGoAdQB2AGoAdgBrAGsAdgB3AGsAdwBsAGwAdwB4AGwAeABtAG4AeQB6AG4AegBvAG8AegB7AG8A"
            + "ewBwAHAAewB8AHAAfABxAHEAfAB9AHEAfQByAHIAfQB+AHIAfgBzAHMAfgB/AHMAfwB0AHQAfwCAAHQAgAB1AHUAgACBAHUAgQB2"
            + "AHYAgQCCAHYAggB3AHcAggCDAHcAgwB4AHkAhACFAHkAhQB6AHoAhQCGAHoAhgB7AHsAhgCHAHsAhwB8AHwAhwCIAHwAiAB9AH0A"
            + "iACJAH0AiQB+AH4AiQCKAH4AigB/AH8AigCLAH8AiwCAAIAAiwCMAIAAjACBAIEAjACNAIEAjQCCAIIAjQCOAIIAjgCDAIQAjwCQ"
            + "AIQAkACFAIUAkACRAIUAkQCGAIYAkQCSAIYAkgCHAIcAkgCTAIcAkwCIAIgAkwCUAIgAlACJAIkAlACVAIkAlQCKAIoAlQCWAIoA"
            + "lgCLAIsAlgCXAIsAlwCMAIwAlwCYAIwAmACNAI0AmACZAI0AmQCOAI8AmgCbAI8AmwCQAJAAmwCcAJAAnACRAJEAnACdAJEAnQCS"
            + "AJIAnQCeAJIAngCTAJMAngCfAJMAnwCUAJQAnwCgAJQAoACVAJUAoAChAJUAoQCWAJYAoQCiAJYAogCXAJcAogCjAJcAowCYAJgA"
            + "owCkAJgApACZAJoApQCmAJoApgCbAJsApgCnAJsApwCcAJwApwCoAJwAqACdAJ0AqACpAJ0AqQCeAJ4AqQCqAJ4AqgCfAJ8AqgCr"
            + "AJ8AqwCgAKAAqwCsAKAArAChAKEArACtAKEArQCiAKIArQCuAKIArgCjAKMArgCvAKMArwCkAKUAsACxAKUAsQCmAKYAsQCyAKYA"
            + "sgCnAKcAsgCzAKcAswCoAKgAswC0AKgAtACpAKkAtAC1AKkAtQCqAKoAtQC2AKoAtgCrAKsAtgC3AKsAtwCsAKwAtwC4AKwAuACt"
            + "AK0AuAC5AK0AuQCuAK4AuQC6AK4AugCvAA==";
}
