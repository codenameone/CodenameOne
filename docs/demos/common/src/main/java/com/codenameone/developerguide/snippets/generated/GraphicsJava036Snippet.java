package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import java.util.*;


class GraphicsJava036Snippet {

    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    void snippet() throws Exception {
        // tag::graphics-java-036[]
        Toolbar.setGlobalToolbar(true);
        Form hi = new Form("Rounder", new BorderLayout());
        Label picture = new Label("", "Container");
        hi.add(BorderLayout.CENTER, picture);
        hi.getUnselectedStyle().setBgColor(0xff0000);
        hi.getUnselectedStyle().setBgTransparency(255);
        Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
        Image camera = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);
        hi.getToolbar().addCommandToRightBar("", camera, (ev) -> {
            try {
                int width = Display.getInstance().getDisplayWidth();
                Image capturedImage = Image.createImage(Capture.capturePhoto(width, -1));
                Image roundMask = Image.createImage(width, capturedImage.getHeight(), 0xff000000);
                Graphics gr = roundMask.getGraphics();
                gr.setColor(0xffffff);
                gr.fillArc(0, 0, width, width, 0, 360);
                Object mask = roundMask.createMask();
                capturedImage = capturedImage.applyMask(mask);
                picture.setIcon(capturedImage);
                hi.revalidate();
            } catch(IOException err) {
                Log.e(err);
            }
        });
        // end::graphics-java-036[]
    }
}
