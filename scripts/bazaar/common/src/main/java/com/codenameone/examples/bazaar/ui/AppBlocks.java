/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.ui;

import com.codename1.appkit.Net;
import com.codename1.components.SpanLabel;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Component;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.URLImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.BlockValueDto;
import com.codenameone.examples.bazaar.api.ExtensionActionDto;
import com.codenameone.examples.bazaar.api.ExtensionBlockDto;
import com.codenameone.examples.bazaar.api.FieldAnswerDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ops.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Installed apps' blocks drawn with ordinary components -- no web view.
/// Text is the app's own, so nothing here is localized; links open in the
/// system browser, actions post through Bazaar ([Api#extensions]), and the
/// app's own page opens in a [BrowserComponent] with a short-lived session.
public final class AppBlocks {
    private AppBlocks() {
    }

    /// A container that fills with the blocks of `area` (`product`, `order`
    /// or `customer`) for `resourceId` once the server answers; empty when no
    /// app adds any.
    public static Container admin(final String storeId, final String area,
            final String resourceId) {
        final Container box = new Container(BoxLayout.y());
        box.setName("app-blocks-" + area);
        Api.extensions().blocks(storeId, area, resourceId, Net.to(list -> {
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                box.add(card(storeId, resourceId, AppBlockModel.of(list.get(iter)), null));
            }
            box.revalidateLater();
        }, AppBlocks::quiet));
        return box;
    }

    /// Blocks are extras: a failure to load them leaves the page as it is.
    static void quiet(int status, String message) {
    }

    /// Checkout's app fields: the blocks of `checkout.block`, with their
    /// inputs read back by [#answers].
    public static final class Checkout {
        public final Container view = new Container(BoxLayout.y());
        final List<AppBlockModel> models = new ArrayList<AppBlockModel>();
        final Map<String, Component> inputs = new HashMap<String, Component>();

        /// The answers as checkout sends them; throws naming a field that is
        /// wrong.
        public List<FieldAnswerDto> answers() throws Blocks.Invalid {
            List<FieldAnswerDto> out = new ArrayList<FieldAnswerDto>();
            for (int iter = 0; iter < models.size(); iter++) {
                AppBlockModel m = models.get(iter);
                List<BlockValueDto> a = m.answers(read(inputs, m.extensionId));
                for (int f = 0; f < a.size(); f++) {
                    out.add(new FieldAnswerDto("app:" + m.extensionId + ":" + a.get(f).key(),
                            a.get(f).value()));
                }
            }
            return out;
        }
    }

    /// The checkout blocks of the store `handle`.
    public static Checkout checkout(String handle) {
        final Checkout c = new Checkout();
        c.view.setName("app-checkout-blocks");
        Api.extensions().shopBlocks(handle, Blocks.CHECKOUT_BLOCK, Net.to(list -> {
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                AppBlockModel m = AppBlockModel.of(list.get(iter));
                if (m.error.length() == 0) {
                    c.models.add(m);
                    c.view.add(card(null, null, m, c.inputs));
                }
            }
            c.view.revalidateLater();
        }, AppBlocks::quiet));
        return c;
    }

    /// The storefront blocks of the store `handle` at `place`.
    public static Container storefront(String handle, final String place) {
        final Container box = new Container(BoxLayout.y());
        Api.extensions().shopBlocks(handle, Blocks.STOREFRONT_BLOCK, Net.to(list -> {
            for (int iter = 0; list != null && iter < list.size(); iter++) {
                ExtensionBlockDto d = list.get(iter);
                if (place.equals(d.place())) {
                    box.add(card(null, null, AppBlockModel.of(d), null));
                }
            }
            box.revalidateLater();
        }, AppBlocks::quiet));
        return box;
    }

    private static Label plain(String text, String uiid) {
        Label l = new Label("", uiid);
        l.setShouldLocalize(false);
        l.setText(text);
        return l;
    }

    private static Component paragraph(String text, String uiid) {
        SpanLabel s = new SpanLabel();
        s.setUIID("Container");
        s.setTextUIID(uiid);
        s.setText(text);
        return s;
    }

    /// One block; `inputs` collects its fields under `extension:key` when
    /// given, else the block keeps its own for its actions.
    static Container card(final String storeId, final String resourceId, final AppBlockModel m,
            Map<String, Component> inputs) {
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        card.setName("app-block-" + m.extensionId);
        card.add(plain(m.appName, "SvMuted"));
        if (m.error.length() > 0) {
            card.add(paragraph(m.error, "SvError"));
        }
        for (int iter = 0; iter < m.rows.size(); iter++) {
            Component c = row(m.rows.get(iter));
            if (c != null) {
                card.add(c);
            }
        }
        final Map<String, Component> own = inputs == null ? new HashMap<String, Component>()
                : inputs;
        for (int iter = 0; iter < m.fields.size(); iter++) {
            Blocks.Node f = m.fields.get(iter);
            Component in = input(f);
            own.put(m.extensionId + ":" + f.key, in);
            card.add(plain(f.text + (f.required ? " *" : ""), "SvMuted"));
            card.add(in);
        }
        if (storeId != null) {
            Container buttons = new Container(BoxLayout.x());
            for (int iter = 0; iter < m.actions.size(); iter++) {
                final Blocks.Node a = m.actions.get(iter);
                Button b = new Button("", "SvSecondary");
                b.setShouldLocalize(false);
                b.setText(a.text);
                b.setName("app-action-" + a.action);
                b.addActionListener(e -> press(storeId, resourceId, m, a.action, own));
                buttons.add(b);
            }
            Button open = Ui.link(Lang.tr("Open"), "app-open-" + m.extensionId,
                    e -> open(storeId, m));
            buttons.add(open);
            card.add(buttons);
        }
        return card;
    }

    private static Component row(final Blocks.Row r) {
        int indent = r.depth * 8;
        Component c;
        if ("heading".equals(r.kind)) {
            c = plain(r.text, "SvHeading");
        } else if ("badge".equals(r.kind)) {
            Container wrap = new Container(BoxLayout.x());
            wrap.add(plain(r.text, "SvBadge"));
            c = wrap;
        } else if ("link".equals(r.kind)) {
            Button b = new Button("", "SvLink");
            b.setShouldLocalize(false);
            b.setText(r.text);
            b.addActionListener(e -> Display.getInstance().execute(r.url));
            c = b;
        } else if ("image".equals(r.kind)) {
            int w = Display.getInstance().convertToPixels(30);
            EncodedImage holder = EncodedImage.createFromImage(Image.createImage(w, w / 2, 0),
                    false);
            Label l = new Label(URLImage.createToStorage(holder, "appimg-" + r.url.hashCode(),
                    r.url));
            l.setShouldLocalize(false);
            c = l;
        } else if ("divider".equals(r.kind)) {
            Label hr = new Label("", "SvDivider");
            c = hr;
        } else if ("item".equals(r.kind)) {
            c = paragraph("\u2022 " + r.text, "SvDeskText");
        } else {
            c = paragraph(r.text, "SvDeskText");
        }
        if (indent > 0) {
            c.getAllStyles().setMarginLeft(indent);
        }
        return c;
    }

    private static Component input(Blocks.Node f) {
        if (Blocks.KIND_CHECKBOX.equals(f.kind)) {
            CheckBox c = new CheckBox();
            c.setName("app-field-" + f.key);
            return c;
        }
        if (Blocks.KIND_SELECT.equals(f.kind)) {
            ComboBox<String> c = new ComboBox<String>();
            c.addItem("");
            for (int iter = 0; iter < f.items.size(); iter++) {
                c.addItem(f.items.get(iter));
            }
            c.setName("app-field-" + f.key);
            return c;
        }
        TextField t = new TextField("", "", 20, Blocks.KIND_NUMBER.equals(f.kind)
                ? TextField.DECIMAL : TextField.ANY);
        t.setName("app-field-" + f.key);
        return t;
    }

    /// The values typed into the inputs of extension `extensionId`.
    static Map<String, String> read(Map<String, Component> inputs, String extensionId) {
        Map<String, String> out = new HashMap<String, String>();
        String prefix = extensionId + ":";
        for (Map.Entry<String, Component> e : inputs.entrySet()) {
            if (!e.getKey().startsWith(prefix)) {
                continue;
            }
            Component c = e.getValue();
            String v;
            if (c instanceof CheckBox) {
                v = ((CheckBox) c).isSelected() ? "yes" : "";
            } else if (c instanceof ComboBox) {
                Object s = ((ComboBox) c).getSelectedItem();
                v = s == null ? "" : s.toString();
            } else if (c instanceof TextField) {
                v = ((TextField) c).getText();
            } else {
                v = "";
            }
            out.put(e.getKey().substring(prefix.length()), v);
        }
        return out;
    }

    private static void press(String storeId, String resourceId, AppBlockModel m, String action,
            Map<String, Component> inputs) {
        List<BlockValueDto> values;
        try {
            values = m.answers(read(inputs, m.extensionId));
        } catch (Blocks.Invalid bad) {
            Ui.fail(400, bad.getMessage());
            return;
        }
        Api.extensions().action(storeId, m.extensionId, new ExtensionActionDto(action,
                resourceId, values), Net.to(r -> {
                    String message = r == null || r.message() == null ? "" : r.message();
                    if (r != null && r.ok()) {
                        if (message.length() > 0) {
                            Ui.sayPlain(message);
                        }
                    } else {
                        Ui.fail(502, message.length() > 0 ? message : "The app did not answer");
                    }
                }, Ui::fail));
    }

    /// Opens the app's own page in a web view, signed in by a five-minute
    /// session token in its address.
    static void open(String storeId, final AppBlockModel m) {
        Api.extensions().session(storeId, m.installationId, Net.to(s -> {
            if (s == null || s.url() == null) {
                return;
            }
            final Form previous = Display.getInstance().getCurrent();
            Form f = new Form(new BorderLayout());
            f.setTitle(m.appName);
            f.getTitleComponent().setShouldLocalize(false);
            BrowserComponent web = new BrowserComponent();
            web.setName("app-page");
            web.setURL(s.url());
            f.add(BorderLayout.CENTER, web);
            Ui.back(f, previous);
            f.show();
        }, Ui::fail));
    }
}
