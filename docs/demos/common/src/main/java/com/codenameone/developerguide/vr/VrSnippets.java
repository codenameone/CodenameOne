package com.codenameone.developerguide.vr;

import com.codename1.gpu.Camera;
import com.codename1.gpu.GraphicsDevice;
import com.codename1.gpu.Material;
import com.codename1.gpu.Matrix4;
import com.codename1.gpu.Mesh;
import com.codename1.gpu.Primitives;
import com.codename1.gpu.Texture;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.vr.Media360View;
import com.codename1.vr.TextureSource;
import com.codename1.vr.VREye;
import com.codename1.vr.VRRenderer;
import com.codename1.vr.VRView;
import java.io.IOException;

/**
 * Snippets that accompany the Virtual Reality and 360 Media guide chapter.
 */
public class VrSnippets {

    public void showStereoScene() {
        // tag::stereoScene[]
        VRView vr = new VRView(new VRRenderer() {
            Mesh cube;
            Material material;

            public void onInit(GraphicsDevice device) {
                cube = Primitives.cube(device, 0.5f);
                material = new Material(Material.Type.PHONG).setColor(0xff3366ff);
            }

            public void onEyeFrame(GraphicsDevice device, VREye eye, Camera camera) {
                device.draw(cube, material, Matrix4.translation(0, 0, -2));
            }

            public void onDispose(GraphicsDevice device) { }
        });
        vr.setContinuous(true); // head tracked scenes want continuous rendering

        Form f = new Form("VR", new BorderLayout());
        f.add(BorderLayout.CENTER, vr);
        f.show();
        // end::stereoScene[]
    }

    public void showPanorama(Form f) throws IOException {
        // tag::photoViewer[]
        Media360View pano = new Media360View();
        pano.setImage(EncodedImage.create("/panorama.jpg"));
        f.add(BorderLayout.CENTER, pano);
        // end::photoViewer[]
    }

    public void dynamicTexture(Media360View pano, final int[] initialPixels) {
        // tag::textureSource[]
        pano.setTextureSource(new TextureSource() {
            public Texture createTexture(GraphicsDevice device) {
                return device.createTexture(1024, 512, initialPixels);
            }
            public boolean updateTexture(GraphicsDevice device, Texture texture) {
                // mutate pixels and re-upload as needed; return true when changed
                return false;
            }
            public void dispose(GraphicsDevice device) { }
        });
        // end::textureSource[]
    }
}
