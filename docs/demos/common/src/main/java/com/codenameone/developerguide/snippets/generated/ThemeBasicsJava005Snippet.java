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


class ThemeBasicsJava005Snippet {

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
    // tag::theme-basics-java-005[]
    private Label createForFont(Font fnt, String s) {
      Label l = new Label(s);
      l.getUnselectedStyle().setFont(fnt);
      return l;
    }

    public void showForm() {
      GridLayout gr = new GridLayout(5);
      gr.setAutoFit(true);
      Form hi = new Form("Fonts", gr);

      int fontSize = Display.getInstance().convertToPixels(3);

      // requires Handlee-Regular.ttf in the src folder root!
      Font ttfFont = Font.createTrueTypeFont("Handlee", "Handlee-Regular.ttf").
                          derive(fontSize, Font.STYLE_PLAIN);

      Font smallPlainSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
      Font mediumPlainSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
      Font largePlainSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
      Font smallBoldSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
      Font mediumBoldSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
      Font largeBoldSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
      Font smallItalicSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_SMALL);
      Font mediumItalicSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
      Font largeItalicSystemFont = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_LARGE);

      Font smallPlainMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
      Font mediumPlainMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
      Font largePlainMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_LARGE);
      Font smallBoldMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_SMALL);
      Font mediumBoldMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
      Font largeBoldMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_LARGE);
      Font smallItalicMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_ITALIC, Font.SIZE_SMALL);
      Font mediumItalicMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
      Font largeItalicMonospaceFont = Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_ITALIC, Font.SIZE_LARGE);

      Font smallPlainProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
      Font mediumPlainProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
      Font largePlainProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_LARGE);
      Font smallBoldProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
      Font mediumBoldProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
      Font largeBoldProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
      Font smallItalicProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_SMALL);
      Font mediumItalicProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_MEDIUM);
      Font largeItalicProportionalFont = Font.createSystemFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_LARGE);

      String[] nativeFontTypes = {
          "native:MainThin", "native:MainLight",
          "native:MainRegular", "native:MainBold",
          "native:MainBlack", "native:ItalicThin",
          "native:ItalicLight", "native:ItalicRegular",
          "native:ItalicBold", "native:ItalicBlack"};

      for(String s : nativeFontTypes) {
          Font tt  = Font.createTrueTypeFont(s, s).derive(fontSize, Font.STYLE_PLAIN);
          hi.add(createForFont(tt, s));
      }

      hi.add(createForFont(ttfFont, "Handlee TTF Font")).
              add(createForFont(smallPlainSystemFont, "smallPlainSystemFont")).
              add(createForFont(mediumPlainSystemFont, "mediumPlainSystemFont")).
              add(createForFont(largePlainSystemFont, "largePlainSystemFont")).
              add(createForFont(smallBoldSystemFont, "smallBoldSystemFont")).
              add(createForFont(mediumBoldSystemFont, "mediumBoldSystemFont")).
              add(createForFont(largeBoldSystemFont, "largeBoldSystemFont")).
              add(createForFont(smallPlainSystemFont, "smallItalicSystemFont")).
              add(createForFont(mediumItalicSystemFont, "mediumItalicSystemFont")).
              add(createForFont(largeItalicSystemFont, "largeItalicSystemFont")).

              add(createForFont(smallPlainMonospaceFont, "smallPlainMonospaceFont")).
              add(createForFont(mediumPlainMonospaceFont, "mediumPlainMonospaceFont")).
              add(createForFont(largePlainMonospaceFont, "largePlainMonospaceFont")).
              add(createForFont(smallBoldMonospaceFont, "smallBoldMonospaceFont")).
              add(createForFont(mediumBoldMonospaceFont, "mediumBoldMonospaceFont")).
              add(createForFont(largeBoldMonospaceFont, "largeBoldMonospaceFont")).
              add(createForFont(smallItalicMonospaceFont, "smallItalicMonospaceFont")).
              add(createForFont(mediumItalicMonospaceFont, "mediumItalicMonospaceFont")).
              add(createForFont(largeItalicMonospaceFont, "largeItalicMonospaceFont")).

              add(createForFont(smallPlainProportionalFont, "smallPlainProportionalFont")).
              add(createForFont(mediumPlainProportionalFont, "mediumPlainProportionalFont")).
              add(createForFont(largePlainProportionalFont, "largePlainProportionalFont")).
              add(createForFont(smallBoldProportionalFont, "smallBoldProportionalFont")).
              add(createForFont(mediumBoldProportionalFont, "mediumBoldProportionalFont")).
              add(createForFont(largeBoldProportionalFont, "largeBoldProportionalFont")).
              add(createForFont(smallItalicProportionalFont, "smallItalicProportionalFont")).
              add(createForFont(mediumItalicProportionalFont, "mediumItalicProportionalFont")).
              add(createForFont(largeItalicProportionalFont, "largeItalicProportionalFont"));

      hi.show();
    }
    // end::theme-basics-java-005[]
}
