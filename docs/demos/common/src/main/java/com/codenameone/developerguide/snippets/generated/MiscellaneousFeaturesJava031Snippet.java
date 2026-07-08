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


class MiscellaneousFeaturesJava031Snippet {

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
Form f = new Form("Accordion", new BorderLayout());
Accordion accr = new Accordion();

void addEntry(Accordion accr) {
    TextArea t = new TextArea("New Entry");
    Button delete = new Button();
    FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
    Label title = new Label(t.getText());
    t.addActionListener(ee -> title.setText(t.getText()));
    delete.addActionListener(ee -> {
        accr.removeContent(t);
        accr.animateLayout(200);
    });
    delete.setBlockLead(true);
    delete.setUIID("Label");
    Container header = BorderLayout.center(title).
            add(BorderLayout.EAST, delete);
    accr.addContent(header, t);
    accr.animateLayout(200);
}
    void snippet() throws Exception {
        // tag::miscellaneous-features-java-031[]
        f.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_ADD, e -> addEntry(accr));
        addEntry(accr);
        f.add(BorderLayout.CENTER, accr);
        f.show();
        // end::miscellaneous-features-java-031[]
    }
}
