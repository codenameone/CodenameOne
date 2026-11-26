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
import com.codenameone.examples.bazaar.api.BlockDto;
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
/// signed with a key from `web.security.FormKeys`, so it can be shared with a
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
    public static final String[] SECTIONS = {"hero", "collections", "featured", "blog", "about",
        "blocks"};
    /// The kinds of block the `blocks` section holds.
    public static final String[] BLOCK_KINDS = {"heading", "text", "image", "product",
        "collection", "button"};
    /// At most this many blocks, each with at most these many characters of text and of
    /// reference, so the encoded list fits its VARCHAR(4000) column.
    static final int MAX_BLOCKS = 10;
    static final int MAX_BLOCK_TEXT = 240;
    static final int MAX_BLOCK_REF = 120;
    static final long PREVIEW_MILLIS = 3600L * 1000L;

    private final StoreThemeRepository rows;
    private final PhotoRepository photos;
    private final StoreAccess access;
    private final String key;

    public Themes(StoreThemeRepository rows, PhotoRepository photos, StoreAccess access,
            com.codenameone.examples.bazaar.web.security.FormKeys keys) throws IOException {
        this.rows = rows;
        this.photos = photos;
        this.access = access;
        this.key = keys.key(com.codenameone.examples.bazaar.web.security.FormKeys.THEME_PREVIEW);
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
        t.setBlocks(encodeBlocks(storeId, in.blocks()));
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
        t.setBlocks(from.getBlocks());
        t.setUpdatedAt(Instant.now());
        return t;
    }

    static ThemeDto dto(StoreTheme t) {
        return new ThemeDto(t.getStatus(), t.getAccent(), t.getPalette(), t.getFont(),
                t.getRadius(), t.getLogoPhotoId(), sectionList(t), blockList(t));
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

    /// The blocks of `t` in order; the stored form is one block a line, its kind, text and
    /// reference split by tabs (input never carries either character).
    public static List<BlockDto> blockList(StoreTheme t) {
        List<BlockDto> out = new ArrayList<BlockDto>();
        String all = t.getBlocks() == null ? "" : t.getBlocks();
        int start = 0;
        while (start < all.length()) {
            int end = all.indexOf('\n', start);
            if (end < 0) {
                end = all.length();
            }
            String line = all.substring(start, end);
            start = end + 1;
            int a = line.indexOf('\t');
            int b = a < 0 ? -1 : line.indexOf('\t', a + 1);
            if (b > 0) {
                out.add(new BlockDto(line.substring(0, a), line.substring(a + 1, b),
                        line.substring(b + 1), ""));
            }
        }
        return out;
    }

    /// Checks and encodes the blocks: a known kind, text where the kind shows text, a
    /// reference where it needs one (one of this store's photos for an image, a handle for
    /// a product or collection, a site path or https link for a button), or 400.
    String encodeBlocks(String storeId, List<BlockDto> in) throws IOException {
        if (in == null || in.isEmpty()) {
            return "";
        }
        if (in.size() > MAX_BLOCKS) {
            throw new ResponseStatusException(400, "At most " + MAX_BLOCKS + " blocks");
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < in.size(); iter++) {
            BlockDto b = in.get(iter);
            String kind = b == null || b.kind() == null ? "" : b.kind().trim();
            pick(kind, BLOCK_KINDS, "block kind");
            String text = flat(b.text(), MAX_BLOCK_TEXT);
            String ref = flat(b.ref(), MAX_BLOCK_REF);
            if (("heading".equals(kind) || "text".equals(kind) || "button".equals(kind))
                    && text.length() == 0) {
                throw new ResponseStatusException(400, "A " + kind + " block needs text");
            }
            if ("heading".equals(kind) || "text".equals(kind)) {
                ref = "";
            } else if (ref.length() == 0) {
                throw new ResponseStatusException(400, "A " + kind + " block needs a reference");
            }
            if ("image".equals(kind)) {
                Photo p = ref.length() > 32 ? null : photos.findById(ref);
                if (p == null || !storeId.equals(p.getOwnerId())) {
                    throw new ResponseStatusException(400,
                            "An image block must use one of this store's photos");
                }
            }
            if ("button".equals(kind) && !safeLink(ref)) {
                throw new ResponseStatusException(400,
                        "A button links to a path on this site or an https address");
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(kind).append('\t').append(text).append('\t').append(ref);
        }
        return out.toString();
    }

    /// A path on this site (one leading slash) or an https address; never a script URL.
    public static boolean safeLink(String ref) {
        if (ref == null) {
            return false;
        }
        if (ref.startsWith("/")) {
            return !ref.startsWith("//") && ref.indexOf('\\') < 0;
        }
        return ref.regionMatches(true, 0, "https://", 0, 8) && ref.length() > 8;
    }

    /// `s` trimmed, with tabs and line breaks turned into spaces, at most `max` long.
    private static String flat(String s, int max) {
        if (s == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            b.append(c == '\t' || c == '\n' || c == '\r' ? ' ' : c);
        }
        String out = b.toString().trim();
        return out.length() > max ? out.substring(0, max).trim() : out;
    }
}
