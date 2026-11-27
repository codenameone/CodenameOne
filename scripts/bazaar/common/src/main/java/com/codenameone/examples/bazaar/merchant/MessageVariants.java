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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.UITimer;
import com.codename1.graph.GraphNode;
import com.codenameone.examples.bazaar.api.TranslationDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The translations of a message the merchant writes -- a campaign, a
/// journey's or an automation's message step -- in its editor: a language
/// and one field per text of the message in that language.
///
/// A reader gets the translation for their language when every text of the
/// message is translated, else the original; an empty field removes that
/// translation. A step keeps its translations in its own data, as `body@es`
/// beside `body`, so they save with the graph. A campaign's go to the
/// server's translations, kind `CAMPAIGN`, as they are typed.
final class MessageVariants {
    /// The languages a message is translated into, as codes and names.
    static final String[] LANGUAGES = {"es", "fr", "de", "he", "en"};
    static final String[] NAMES = {"Spanish", "French", "German", "Hebrew", "English"};
    /// What joins a text's name to a language in a step's data.
    static final String VARIANT = "@";

    private MessageVariants() {
    }

    /// The texts of a message step of `type` that are translated, or none.
    static String[] texts(String type) {
        if ("journey.send.email".equals(type) || "email.customer".equals(type)) {
            return "email.customer".equals(type) ? new String[] {"subject", "message"}
                    : new String[] {"subject", "body"};
        }
        if ("journey.send.sms".equals(type)) {
            return new String[] {"body"};
        }
        if ("journey.send.push".equals(type)) {
            return new String[] {"title", "body"};
        }
        if ("notify.staff".equals(type)) {
            return new String[] {"message"};
        }
        return new String[0];
    }

    private static String caption(String text) {
        return "subject".equals(text) ? "Subject" : "subjectB".equals(text)
                ? "Second subject" : "title".equals(text) ? "Title" : "Message";
    }

    private static Picker languages(Container to) {
        Picker p = new Picker();
        p.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        p.setUIID("SvField");
        p.setName("message-language");
        String[] labels = new String[NAMES.length];
        for (int iter = 0; iter < NAMES.length; iter++) {
            labels[iter] = Lang.tr(NAMES[iter]);
        }
        p.setStrings(labels);
        p.setSelectedStringIndex(0);
        to.add(Ui.label(Lang.tr("Translate into"), "SvLabel"));
        to.add(p);
        return p;
    }

    private static String chosen(Picker p) {
        int at = p.getSelectedStringIndex();
        return at >= 0 && at < LANGUAGES.length ? LANGUAGES[at] : LANGUAGES[0];
    }

    /// The translation fields of a graph step, added to `to` when the step
    /// is a message; `changed` runs after each edit.
    static void step(Container to, final GraphNode node, String type, final Runnable changed) {
        final String[] texts = texts(type);
        if (texts.length == 0) {
            return;
        }
        final Picker picker = languages(to);
        final Container fields = new Container(BoxLayout.y());
        to.add(fields);
        final Runnable paint = () -> {
            fields.removeAll();
            final String lang = chosen(picker);
            for (int iter = 0; iter < texts.length; iter++) {
                final String key = texts[iter] + VARIANT + lang;
                final TextField f = MUi.text(fields, caption(texts[iter]), "variant-" + texts[iter],
                        AutomationCodec.param(node, key));
                f.addDataChangedListener((t, index) -> {
                    AutomationCodec.setParam(node, key, f.getText().trim());
                    if (changed != null) {
                        changed.run();
                    }
                });
            }
            Ui.relayout(fields);
        };
        picker.addActionListener(e -> paint.run());
        paint.run();
    }

    /// The translation fields of a saved campaign: each field is written to
    /// the server a moment after typing stops in it.
    static void campaign(Container to, final String campaignId, boolean ab) {
        final String[] texts = ab ? new String[] {"subjectA", "subjectB", "body"}
                : new String[] {"subjectA", "body"};
        to.add(Ui.text(Lang.tr("A subscriber gets the message in their language when it is "
                + "translated, else the original."), "SvMuted"));
        final Picker picker = languages(to);
        final Container fields = new Container(BoxLayout.y());
        to.add(fields);
        final Runnable paint = () -> {
            final String lang = chosen(picker);
            Api.markets().translations(MerchantState.storeId(), "CAMPAIGN", campaignId,
                    Net.to(found -> {
                        fields.removeAll();
                        for (int iter = 0; iter < texts.length; iter++) {
                            final String field = texts[iter];
                            final TextField f = MUi.text(fields, "subjectA".equals(field)
                                    ? "Subject" : caption(field), "campaign-tr-" + field,
                                    value(found, lang, field));
                            final UITimer[] pending = new UITimer[1];
                            f.addDataChangedListener((t, index) -> {
                                if (pending[0] != null) {
                                    pending[0].cancel();
                                }
                                pending[0] = UITimer.timer(800, false, fields.getComponentForm(),
                                        () -> Api.markets().putTranslation(MerchantState.storeId(),
                                                new TranslationDto("CAMPAIGN", campaignId, lang,
                                                        field, f.getText().trim()),
                                                Net.to(saved -> { }, Ui::fail)));
                            });
                        }
                        Ui.relayout(fields);
                    }, Ui::fail));
        };
        picker.addActionListener(e -> paint.run());
        paint.run();
    }

    private static String value(List<TranslationDto> all, String lang, String field) {
        for (int iter = 0; all != null && iter < all.size(); iter++) {
            TranslationDto t = all.get(iter);
            if (lang.equals(t.locale()) && field.equals(t.field())) {
                return t.value();
            }
        }
        return "";
    }
}
