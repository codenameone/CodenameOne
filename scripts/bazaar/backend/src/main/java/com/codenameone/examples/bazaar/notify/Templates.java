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
package com.codenameone.examples.bazaar.notify;

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.Rows;
import com.codenameone.examples.bazaar.api.TemplateDto;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.web.StoreText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A store's own wording of the texts shoppers receive, per language.
///
/// Every shopper mail is built from texts of [StoreText] (`mail.*`, the
/// staff-only `mail.low.*` aside), each with numbered placeholders `{0}`,
/// `{1}`... the mail fills in. A store may replace any of them in any of the
/// storefront's languages; [Emails] reads the replacements as it builds a
/// mail, so an override reaches every mail that uses the text, and a store
/// with none sends exactly the built-in text. The one SMS a shopper gets, the
/// register's receipt, has an opening line [#SMS_RECEIPT] that is empty
/// unless the store writes one.
///
/// Texts go out as text: a mail escapes them like the built-in ones, so a
/// template cannot add markup.
@Component
public class Templates {
    /// The opening line of the register's SMS receipt.
    public static final String SMS_RECEIPT = "sms.receipt.intro";
    public static final String MAIL = "MAIL";
    public static final String SMS = "SMS";
    static final int MAX_BODY = 2000;

    private final DataSource db;
    private final StoreAccess access;

    public Templates(DataSource db, StoreAccess access) {
        this.db = db;
        this.access = access;
    }

    /// Every editable text in `language`, built-in and the store's own.
    @Transactional(readOnly = true)
    public List<TemplateDto> list(String username, String storeId, String language)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, false, StoreAccess.ANYONE);
        String lang = lang(language);
        Map<String, String> own = overrides(storeId, lang);
        String[] keys = keys();
        List<TemplateDto> out = new ArrayList<TemplateDto>();
        for (int iter = 0; iter < keys.length; iter++) {
            out.add(dto(keys[iter], lang, own.get(keys[iter])));
        }
        return out;
    }

    /// Saves the store's wording of one text. It may use the placeholders
    /// the built-in text has, and no others.
    @Transactional(rollbackFor = IOException.class)
    public TemplateDto save(String username, String storeId, TemplateDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        String key = known(wanted);
        String lang = lang(wanted.language());
        String body = wanted.body() == null ? "" : wanted.body().trim();
        if (body.length() == 0 && !SMS_RECEIPT.equals(key)) {
            throw new ResponseStatusException(400, "Write the text, or reset it to the "
                    + "built-in one");
        }
        if (body.length() > MAX_BODY) {
            throw new ResponseStatusException(400, "A text is at most " + MAX_BODY
                    + " characters");
        }
        int allowed = placeholders(builtIn(key, lang));
        int used = highest(body);
        if (used >= allowed) {
            throw new ResponseStatusException(400, allowed == 0 ? "This text has no "
                    + "placeholders" : "This text's placeholders are {0} to {" + (allowed - 1)
                    + "}");
        }
        db.execute("DELETE FROM notification_template WHERE store_id = ? AND language = ? AND "
                + "text_key = ?", new Object[] {storeId, lang, key});
        db.execute("INSERT INTO notification_template (id, store_id, language, text_key, body, "
                + "updated_at) VALUES (?, ?, ?, ?, ?, ?)", new Object[] {Ids.next(), storeId,
                    lang, key, body, Long.valueOf(System.currentTimeMillis())});
        return dto(key, lang, body);
    }

    /// Goes back to the built-in wording.
    @Transactional(rollbackFor = IOException.class)
    public TemplateDto reset(String username, String storeId, TemplateDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, true, StoreAccess.MANAGERS);
        String key = known(wanted);
        String lang = lang(wanted.language());
        db.execute("DELETE FROM notification_template WHERE store_id = ? AND language = ? AND "
                + "text_key = ?", new Object[] {storeId, lang, key});
        return dto(key, lang, null);
    }

    /// The text -- the one sent, or the saved one, or the built-in one --
    /// with example values in its placeholders, answered in `body`.
    @Transactional(readOnly = true)
    public TemplateDto preview(String username, String storeId, TemplateDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.SETTINGS, false, StoreAccess.ANYONE);
        String key = known(wanted);
        String lang = lang(wanted.language());
        String own = overrides(storeId, lang).get(key);
        String text = wanted.body() != null && wanted.body().trim().length() > 0
                ? wanted.body().trim() : own != null ? own : builtIn(key, lang);
        if (text.length() > MAX_BODY) {
            throw new ResponseStatusException(400, "A text is at most " + MAX_BODY
                    + " characters");
        }
        TemplateDto base = dto(key, lang, own);
        return new TemplateDto(key, base.template(), base.channel(), lang,
                StoreText.fill(text, examples()), base.defaultBody(), base.placeholders(),
                base.custom());
    }

    /// The store's texts in `lang`, by key; empty when it has none.
    @Transactional(readOnly = true)
    public Map<String, String> overrides(String storeId, String lang) {
        Map<String, String> out = new HashMap<String, String>();
        try {
            List rows = db.query("SELECT text_key, body FROM notification_template WHERE "
                    + "store_id = ? AND language = ?", new Object[] {storeId, lang(lang)});
            for (int iter = 0; iter < rows.size(); iter++) {
                Map r = (Map) rows.get(iter);
                out.put(Rows.text(r.get("text_key")), Rows.text(r.get("body")));
            }
        } catch (IOException unreadable) {
            // A mail is never held up by its wording: the built-in text goes.
            out.clear();
        }
        return out;
    }

    /// Every editable key: the shopper mail texts, then the SMS receipt line.
    public static String[] keys() {
        String[] all = StoreText.keys();
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter].startsWith("mail.") && !all[iter].startsWith("mail.low.")) {
                out.add(all[iter]);
            }
        }
        out.add(SMS_RECEIPT);
        return out.toArray(new String[out.size()]);
    }

    private static TemplateDto dto(String key, String lang, String own) {
        String builtIn = builtIn(key, lang);
        int n = placeholders(builtIn);
        StringBuilder names = new StringBuilder();
        for (int iter = 0; iter < n; iter++) {
            names.append(iter == 0 ? "" : " ").append('{').append(iter).append('}');
        }
        return new TemplateDto(key, group(key), SMS_RECEIPT.equals(key) ? SMS : MAIL, lang,
                own == null ? builtIn : own, builtIn, names.toString(), own != null);
    }

    /// The built-in text of `key` in `lang`, unfilled.
    static String builtIn(String key, String lang) {
        if (SMS_RECEIPT.equals(key)) {
            return "";
        }
        String s = StoreText.table(lang).get(key);
        return s == null ? StoreText.table("en").get(key) : s;
    }

    /// The mail a key belongs to: `mail.order.title` is in `order`.
    static String group(String key) {
        if (SMS_RECEIPT.equals(key)) {
            return "sms";
        }
        int a = key.indexOf('.');
        int b = key.indexOf('.', a + 1);
        return b < 0 ? key.substring(a + 1) : key.substring(a + 1, b);
    }

    /// How many placeholders a text has: one more than the highest `{n}`.
    static int placeholders(String s) {
        return highest(s) + 1;
    }

    /// The highest `{n}` in `s`, or -1 for none.
    static int highest(String s) {
        int out = -1;
        for (int iter = 0; s != null && iter + 2 < s.length(); iter++) {
            char d = s.charAt(iter + 1);
            if (s.charAt(iter) == '{' && d >= '0' && d <= '9' && s.charAt(iter + 2) == '}') {
                out = Math.max(out, d - '0');
            }
        }
        return out;
    }

    private static Object[] examples() {
        Object[] out = new Object[10];
        for (int iter = 0; iter < out.length; iter++) {
            out[iter] = "[example " + (iter + 1) + "]";
        }
        return out;
    }

    private static String known(TemplateDto wanted) {
        String key = wanted == null ? null : wanted.key();
        String[] keys = keys();
        for (int iter = 0; key != null && iter < keys.length; iter++) {
            if (keys[iter].equals(key)) {
                return key;
            }
        }
        throw new ResponseStatusException(400, "No such text");
    }

    /// A storefront language; English for anything else.
    static String lang(String language) {
        return language != null && StoreText.supported(language) ? language : "en";
    }
}
