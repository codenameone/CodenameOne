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


class TheComponentsOfCodenameOneJava121Snippet {

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
        // tag::the-components-of-codename-one-java-121[]
        final String[] characters = { "Tyrion Lannister", "Jaime Lannister", "Cersei Lannister", "Daenerys Targaryen",
            "Jon Snow", "Petyr Baelish", "Jorah Mormont", "Sansa Stark", "Arya Stark", "Theon Greyjoy"
            // snipped the rest for clarity
        };

        Form current = new Form("AutoComplete", BoxLayout.y());

        AutoCompleteTextField ac = new AutoCompleteTextField(characters);

        final int size = Display.getInstance().convertToPixels(7);
        final EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(size, size, 0xffcccccc), true);

        final String[] actors = { "Peter Dinklage", "Nikolaj Coster-Waldau", "Lena Headey"}; // <1>
        final Image[] pictures = {
            URLImage.createToStorage(placeholder, "tyrion","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/tyrion-lannister-512x512.jpg"),
            URLImage.createToStorage(placeholder, "jaime","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/jamie-lannister-512x512.jpg"),
            URLImage.createToStorage(placeholder, "cersei","http://i.lv3.hbo.com/assets/images/series/game-of-thrones/character/s5/cersei-lannister-512x512.jpg")
        };

        ac.setCompletionRenderer(new ListCellRenderer() {
            private final Label focus = new Label(); // <2>
            private final Label line1 = new Label(characters[0]);
            private final Label line2 = new Label(actors[0]);
            private final Label icon = new Label(pictures[0]);
            private final Container selection = BorderLayout.center(
                    BoxLayout.encloseY(line1, line2)).add(BorderLayout.EAST, icon);

            @Override
            public Component getListCellRendererComponent(com.codename1.ui.List list, Object value, int index, boolean isSelected) {
                for(int iter = 0 ; iter < characters.length ; iter++) {
                    if(characters[iter].equals(value)) {
                        line1.setText(characters[iter]);
                        if(actors.length > iter) {
                            line2.setText(actors[iter]);
                            icon.setIcon(pictures[iter]);
                        } else {
                            line2.setText(""); // <3>
                            icon.setIcon(placeholder);
                        }
                        break;
                    }
                }
                return selection;
            }

            @Override
            public Component getListFocusComponent(com.codename1.ui.List list) {
                return focus;
            }
        });
        current.add(ac);

        current.show();
        // end::the-components-of-codename-one-java-121[]
    }
}
