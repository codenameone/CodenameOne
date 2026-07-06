package com.codenameone.developerguide.snippets.generated;

import com.codename1.ui.*;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.layouts.mig.MigLayout;

public class BasicsJava035Snippet {
    // tag::basics-java-035[]
    public static Form createForm() {
        Form hi = new Form("MigLayout",
                new MigLayout("wrap 2, insets 4mm", "[right]3mm[32mm]", "[]12[]12[]12[]"));
        hi.add(new Label("First name"));
        hi.add("growx, w 32mm", new TextField("", "First name"));
        hi.add(new Label("Last name"));
        hi.add("growx, w 32mm", new TextField("", "Last name"));
        hi.add(new Label("Phone"));
        hi.add("growx, w 32mm", new TextField("", "Phone"));
        Button ok = new Button("OK");
        ok.setCapsText(false);
        Style okStyle = ok.getAllStyles();
        okStyle.setBgColor(0xf4f8ff);
        okStyle.setBgTransparency(255);
        okStyle.setFgColor(0x0d47a1);
        okStyle.setBorder(Border.createLineBorder(1, 0x2b5c9e));
        okStyle.setPaddingUnit(Style.UNIT_TYPE_DIPS);
        okStyle.setPadding(2, 2, 2, 2);
        hi.add("span 2, growx", FlowLayout.encloseCenter(ok));
        return hi;
    }
    // end::basics-java-035[]

    void snippet() {
        createForm().show();
    }
}
