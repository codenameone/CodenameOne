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


class MiscellaneousFeaturesJava028Snippet {

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
        // tag::miscellaneous-features-java-028[]
        Form hi = new Form("Capture", new BorderLayout());
        hi.setToolbar(new Toolbar());
        Style s = UIManager.getInstance().getComponentStyle("Title");
        FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);

        ImageViewer iv = new ImageViewer(icon);

        hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
            Display.getInstance().openGallery((e) -> {
                if(e != null && e.getSource() != null) {
                    try {
                        DefaultListModel<Image> m = (DefaultListModel<Image>)iv.getImageList();
                        Image img = Image.createImage((String)e.getSource());
                        if(m == null) {
                            m = new DefaultListModel<>(img);
                            iv.setImageList(m);
                            iv.setImage(img);
                        } else {
                            m.addItem(img);
                        }
                        m.setSelectedIndex(m.getSize() - 1);
                    } catch(IOException err) {
                        Log.e(err);
                    }
                }
            }, Display.GALLERY_IMAGE);
        });

        hi.add(BorderLayout.CENTER, iv);
        // end::miscellaneous-features-java-028[]
    }
}
