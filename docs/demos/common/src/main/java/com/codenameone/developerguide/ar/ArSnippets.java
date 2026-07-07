package com.codenameone.developerguide.ar;

import com.codename1.ar.AR;
import com.codename1.ar.ARAnchor;
import com.codename1.ar.ARAnchorEvent;
import com.codename1.ar.ARImageAnchor;
import com.codename1.ar.ARModel;
import com.codename1.ar.ARNode;
import com.codename1.ar.ARPlane;
import com.codename1.ar.ARReferenceImage;
import com.codename1.ar.ARSession;
import com.codename1.ar.ARSessionOptions;
import com.codename1.ar.ARView;
import com.codename1.components.ToastBar;
import com.codename1.gpu.Primitives;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Snippets that accompany the Augmented Reality guide chapter. Each method
 * body between the tag markers is included verbatim into the AsciiDoc.
 */
public class ArSnippets {

    public void placeModelOnFloor(final byte[] modelBytes) {
        // tag::placeModel[]
        if (!AR.isSupported()) {
            ToastBar.showInfoMessage("AR is not supported on this device");
            return;
        }
        final ARSession session = AR.open(new ARSessionOptions());
        final ARView view = session.createView();

        Form f = new Form("AR", new BorderLayout());
        f.add(BorderLayout.CENTER, view);
        f.show();

        // modelBytes is a .glb asset, e.g. loaded from getResourceAsStream
        view.addPointerReleasedListener(e -> {
            float xn = (e.getX() - view.getAbsoluteX()) / (float) view.getWidth();
            float yn = (e.getY() - view.getAbsoluteY()) / (float) view.getHeight();
            session.hitTest(xn, yn).ready(hits -> {
                if (hits.length > 0) {
                    ARAnchor anchor = hits[0].createAnchor();
                    anchor.setNode(new ARNode(ARModel.fromGltf(modelBytes)));
                }
            });
        });
        // end::placeModel[]
    }

    public void logPlanes(ARSession session) {
        // tag::planeListener[]
        session.addPlaneListener(ev -> {
            ARPlane p = ev.getPlane();
            System.out.println(ev.getKind() + " " + p.getType()
                    + " " + p.getExtentX() + "x" + p.getExtentZ() + "m");
        });
        // end::planeListener[]
    }

    public void anchorContent(ARAnchor anchor) {
        // tag::anchorContent[]
        ARNode root = new ARNode(ARModel.fromMesh(
                Primitives.sphere(0.15f, 24, 32, false), 0xffdd4433));
        root.setLocalPosition(0, 0.15f, 0); // rest on the surface, not in it
        anchor.setNode(root);
        // end::anchorContent[]
    }

    public void trackImage(final byte[] posterPngBytes, final byte[] overlayModel) {
        // tag::imageTracking[]
        ARSessionOptions opts = new ARSessionOptions().referenceImages(new ARReferenceImage[]{
            new ARReferenceImage("poster", posterPngBytes, 0.42f) // physical width in meters
        });
        ARSession session = AR.open(opts);
        session.addAnchorListener(ev -> {
            if (ev.getKind() == ARAnchorEvent.Kind.ADDED
                    && ev.getAnchor() instanceof ARImageAnchor) {
                ARImageAnchor img = (ARImageAnchor) ev.getAnchor();
                System.out.println("Recognized " + img.getReferenceImageName());
                img.setNode(new ARNode(ARModel.fromGltf(overlayModel)));
            }
        });
        // end::imageTracking[]
    }
}
