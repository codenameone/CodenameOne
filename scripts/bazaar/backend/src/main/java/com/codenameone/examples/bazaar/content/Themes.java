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
package com.codenameone.examples.bazaar.content;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.ThemeDto;
import com.codenameone.examples.bazaar.domain.Photo;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreTheme;
import com.codenameone.examples.bazaar.marketing.Tokens;
import com.codenameone.examples.bazaar.repository.PhotoRepository;
import com.codenameone.examples.bazaar.repository.StoreThemeRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// A store's storefront theme: a draft the merchant edits and previews, and
/// the live one shoppers see, which publishing replaces with the draft.
///
/// Every value comes from a safe list or is checked to the character -- the
/// accent is `#rrggbb`, the palette, font and corner radius are keys the
/// storefront's own stylesheet maps to CSS custom properties
/// (`th-*`, `font-*`, `radius-*` classes in `web-src/shop.css`), the logo a
/// photo of this store -- because the compiled templates allow no inline
/// style and the public pages' policy allows none. Nothing a merchant types
/// reaches CSS.
///
/// A preview is a signed address ([#previewToken]): the store and an expiry
/// signed with a key only this process holds, so it can be shared with a
/// colleague for an hour and cannot be forged for another store. The page
/// it opens reads the stored draft, never values from the address.
@Component
public class Themes {
    public static final String LIVE = "LIVE";
    public static final String DRAFT = "DRAFT";
    public static final String[] PALETTES = {"light", "warm", "dark", "contrast"};
    public static final String[] FONTS = {"system", "serif", "mono", "humanist", "rounded"};
    public static final String[] RADII = {"soft", "square", "round"};
    /// The home page sections, in their default order.
    public static final String[] SECTIONS = {"hero", "collections", "featured", "blog", "about"};
    static final long PREVIEW_MILLIS = 3600L * 1000L;

    private final StoreThemeRepository rows;
    private final PhotoRepository photos;
    private final StoreAccess access;
    private final String key;

    public Themes(StoreThemeRepository rows, PhotoRepository photos, StoreAccess access)
            throws IOException {
        this.rows = rows;
        this.photos = photos;
        this.access = access;
        this.key = Tokens.token();
    }

    /// The theme `status` (LIVE, or DRAFT -- which starts as the live one).
    @Transactional(readOnly = true)
    public ThemeDto get(String username, String storeId, String status) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(DRAFT.equals(status) ? draftOf(storeId) : liveOf(storeId));
    }

    /// Saves the draft; 400 for a value off the safe lists.
    @Transactional(rollbackFor = IOException.class)
    public ThemeDto saveDraft(String username, String storeId, ThemeDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say what the theme is");
        }
        StoreTheme t = rows.findByStoreIdAndStatus(storeId, DRAFT);
        boolean fresh = t == null;
        if (fresh) {
            t = copy(liveOf(storeId), storeId, DRAFT);
        }
        t.setAccent(accent(in.accent()));
        t.setPalette(pick(in.palette(), PALETTES, "palette"));
        t.setFont(pick(in.font(), FONTS, "font"));
        t.setRadius(pick(in.radius(), RADII, "corner style"));
        String logo = in.logoPhotoId() == null ? "" : in.logoPhotoId().trim();
        if (logo.length() > 0) {
            Photo p = logo.length() > 32 ? null : photos.findById(logo);
            if (p == null || !storeId.equals(p.getOwnerId())) {
                throw new ResponseStatusException(400, "The logo must be one of this store's photos");
            }
        }
        t.setLogoPhotoId(logo);
        t.setSections(sections(in.sections()));
        t.setUpdatedAt(Instant.now());
        if (fresh) {
            rows.insert(t);
        } else {
            rows.save(t);
        }
        return dto(t);
    }

    /// Makes the draft live.
    @Transactional(rollbackFor = IOException.class)
    public ThemeDto publish(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        StoreTheme draft = draftOf(storeId);
        StoreTheme live = rows.findByStoreIdAndStatus(storeId, LIVE);
        boolean fresh = live == null;
        StoreTheme out = copy(draft, storeId, LIVE);
        if (!fresh) {
            out.setId(live.getId());
        }
        out.setUpdatedAt(Instant.now());
        if (fresh) {
            rows.insert(out);
        } else {
            rows.save(out);
        }
        return dto(out);
    }

    /// The theme shoppers see: the live one, or the defaults.
    @Transactional(readOnly = true)
    public StoreTheme liveOf(String storeId) throws IOException {
        StoreTheme t = rows.findByStoreIdAndStatus(storeId, LIVE);
        return t == null ? defaults(storeId) : t;
    }

    @Transactional(readOnly = true)
    StoreTheme draftOf(String storeId) throws IOException {
        StoreTheme t = rows.findByStoreIdAndStatus(storeId, DRAFT);
        return t == null ? copy(liveOf(storeId), storeId, DRAFT) : t;
    }

    /// A preview token for `storeId`, good for an hour.
    public String previewToken(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String body = storeId + "." + (System.currentTimeMillis() + PREVIEW_MILLIS);
        return body + "." + Tokens.sign(key, body);
    }

    /// The draft `token` previews at `store`, or null when it is forged,
    /// expired or another store's.
    @Transactional(readOnly = true)
    public StoreTheme preview(Store store, String token) throws IOException {
        if (token == null || token.length() > 200) {
            return null;
        }
        int last = token.lastIndexOf('.');
        int first = token.indexOf('.');
        if (first <= 0 || last <= first) {
            return null;
        }
        String body = token.substring(0, last);
        if (!store.getId().equals(token.substring(0, first))
                || !Tokens.verify(key, body, token.substring(last + 1))) {
            return null;
        }
        long expires = digits(token.substring(first + 1, last));
        if (expires < System.currentTimeMillis()) {
            return null;
        }
        return draftOf(store.getId());
    }

    private static long digits(String s) {
        if (s.length() == 0 || s.length() > 15) {
            return -1;
        }
        long out = 0;
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            if (c < '0' || c > '9') {
                return -1;
            }
            out = out * 10 + (c - '0');
        }
        return out;
    }

    /// The classes a storefront page's body carries for `t`.
    public static String classes(StoreTheme t) {
        return "th-" + t.getPalette() + " font-" + t.getFont() + " radius-" + t.getRadius();
    }

    /// The sections of `t` in order.
    public static List<String> sectionList(StoreTheme t) {
        return Lists.split(t.getSections());
    }

    static StoreTheme defaults(String storeId) throws IOException {
        StoreTheme t = new StoreTheme();
        t.setId(Ids.next());
        t.setStoreId(storeId);
        t.setStatus(LIVE);
        t.setAccent("");
        t.setPalette(PALETTES[0]);
        t.setFont(FONTS[0]);
        t.setRadius(RADII[0]);
        t.setLogoPhotoId("");
        t.setSections("hero,collections,featured,about");
        return t;
    }

    private static StoreTheme copy(StoreTheme from, String storeId, String status)
            throws IOException {
        StoreTheme t = new StoreTheme();
        t.setId(Ids.next());
        t.setStoreId(storeId);
        t.setStatus(status);
        t.setAccent(from.getAccent());
        t.setPalette(from.getPalette());
        t.setFont(from.getFont());
        t.setRadius(from.getRadius());
        t.setLogoPhotoId(from.getLogoPhotoId());
        t.setSections(from.getSections());
        t.setUpdatedAt(Instant.now());
        return t;
    }

    static ThemeDto dto(StoreTheme t) {
        return new ThemeDto(t.getStatus(), t.getAccent(), t.getPalette(), t.getFont(),
                t.getRadius(), t.getLogoPhotoId(), sectionList(t));
    }

    /// `#rrggbb` (lower case), empty for the store's own colour, or 400.
    static String accent(String in) {
        String a = in == null ? "" : in.trim();
        if (a.length() == 0) {
            return "";
        }
        if (a.length() != 7 || a.charAt(0) != '#') {
            throw new ResponseStatusException(400, "The accent is a colour like #4f46e5");
        }
        StringBuilder out = new StringBuilder("#");
        for (int iter = 1; iter < 7; iter++) {
            char c = a.charAt(iter);
            if (c >= 'A' && c <= 'F') {
                c = (char) (c + 32);
            }
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) {
                throw new ResponseStatusException(400, "The accent is a colour like #4f46e5");
            }
            out.append(c);
        }
        return out.toString();
    }

    static String pick(String in, String[] allowed, String what) {
        if (in == null || in.length() == 0) {
            return allowed[0];
        }
        for (int iter = 0; iter < allowed.length; iter++) {
            if (allowed[iter].equals(in)) {
                return in;
            }
        }
        throw new ResponseStatusException(400, "That " + what + " is not one of the choices");
    }

    /// The sections in the order given, each once, unknown ones refused.
    static String sections(List<String> in) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; in != null && iter < in.size(); iter++) {
            String s = in.get(iter) == null ? "" : in.get(iter).trim();
            if (s.length() == 0) {
                continue;
            }
            pick(s, SECTIONS, "section");
            if (!out.contains(s)) {
                out.add(s);
            }
        }
        if (out.isEmpty()) {
            return "hero,collections,featured,about";
        }
        return Lists.join(out);
    }
}
