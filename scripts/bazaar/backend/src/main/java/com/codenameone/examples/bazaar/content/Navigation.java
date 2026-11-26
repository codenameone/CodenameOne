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
import com.codenameone.examples.bazaar.api.MenuLinkDto;
import com.codenameone.examples.bazaar.api.RedirectDto;
import com.codenameone.examples.bazaar.domain.MenuLink;
import com.codenameone.examples.bazaar.domain.UrlRedirect;
import com.codenameone.examples.bazaar.repository.MenuLinkRepository;
import com.codenameone.examples.bazaar.repository.UrlRedirectRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// A store's navigation menus and its table of permanent redirects.
///
/// **Menus.** Two menus, `main` (the header) and `footer`, each a tree two
/// levels deep: a link under a top-level link is in its drop-down. Order is
/// a position among siblings, moved one step at a time ([#move]) so the
/// back office needs no script to reorder. A link goes to a path on this
/// site or an `https://` address, never a script.
///
/// **Redirects.** An old address of the store (`/s/{handle}/...`, so one
/// store can never redirect another's pages) to a new one on the site. They
/// are followed only where the storefront would otherwise answer 404 (see
/// `web.StorefrontRedirects`), so a redirect never hides a live page; a
/// redirect to itself, or one whose target is itself redirected, is refused,
/// which keeps chains and loops out of the table.
@Component
public class Navigation {
    public static final String MAIN = "main";
    public static final String FOOTER = "footer";
    static final int MAX_LINKS = 200;

    private final MenuLinkRepository links;
    private final UrlRedirectRepository redirects;
    private final StoreAccess access;
    private final StoreRepository stores;
    private final com.codenameone.examples.bazaar.market.Translations translations;

    public Navigation(MenuLinkRepository links, UrlRedirectRepository redirects,
            StoreAccess access, StoreRepository stores, com.codenameone.examples.bazaar.market.Translations translations) {
        this.translations = translations;
        this.stores = stores;
        this.links = links;
        this.redirects = redirects;
        this.access = access;
    }

    // ================================================================= menus

    @Transactional(readOnly = true)
    public List<MenuLinkDto> links(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dtos(ordered(storeId));
    }

    /// Every link of a store: each menu's top level in order, each followed
    /// by its children in order.
    @Transactional(readOnly = true)
    public List<MenuLink> ordered(String storeId) throws IOException {
        List<MenuLink> all = links.findByStoreIdOrderByPositionAsc(storeId);
        List<MenuLink> out = new ArrayList<MenuLink>();
        String[] menus = {MAIN, FOOTER};
        for (int m = 0; m < menus.length; m++) {
            for (int iter = 0; iter < all.size(); iter++) {
                MenuLink top = all.get(iter);
                if (!menus[m].equals(top.getMenu()) || top.getParentId().length() > 0) {
                    continue;
                }
                out.add(top);
                for (int c = 0; c < all.size(); c++) {
                    if (top.getId().equals(all.get(c).getParentId())) {
                        out.add(all.get(c));
                    }
                }
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<MenuLinkDto> add(String username, String storeId, MenuLinkDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say what the link is");
        }
        List<MenuLink> all = links.findByStoreIdOrderByPositionAsc(storeId);
        if (all.size() >= MAX_LINKS) {
            throw new ResponseStatusException(400, "A store has at most " + MAX_LINKS + " links");
        }
        String menu = FOOTER.equals(in.menu()) ? FOOTER : MAIN;
        String parent = in.parentId() == null ? "" : in.parentId().trim();
        if (parent.length() > 0) {
            MenuLink p = link(storeId, parent);
            if (p.getParentId().length() > 0) {
                throw new ResponseStatusException(400, "Menus are two levels deep");
            }
            menu = p.getMenu();
        }
        String title = Lists.cut(in.title(), 120);
        if (title.length() == 0) {
            throw new ResponseStatusException(400, "Give the link a title");
        }
        MenuLink row = new MenuLink();
        row.setId(Ids.next());
        row.setStoreId(storeId);
        row.setMenu(menu);
        row.setParentId(parent);
        row.setTitle(title);
        row.setUrl(url(in.url()));
        int last = -1;
        for (int iter = 0; iter < all.size(); iter++) {
            last = Math.max(last, all.get(iter).getPosition());
        }
        row.setPosition(last + 1);
        links.insert(row);
        return dtos(ordered(storeId));
    }

    /// Swaps a link with its previous (`up`) or next sibling.
    @Transactional(rollbackFor = IOException.class)
    public List<MenuLinkDto> move(String username, String storeId, String linkId, boolean up)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        MenuLink row = link(storeId, linkId);
        List<MenuLink> all = links.findByStoreIdOrderByPositionAsc(storeId);
        List<MenuLink> siblings = new ArrayList<MenuLink>();
        for (int iter = 0; iter < all.size(); iter++) {
            MenuLink l = all.get(iter);
            if (l.getMenu().equals(row.getMenu()) && l.getParentId().equals(row.getParentId())) {
                siblings.add(l);
            }
        }
        int at = -1;
        for (int iter = 0; iter < siblings.size(); iter++) {
            if (siblings.get(iter).getId().equals(row.getId())) {
                at = iter;
            }
        }
        int other = up ? at - 1 : at + 1;
        if (at >= 0 && other >= 0 && other < siblings.size()) {
            MenuLink o = siblings.get(other);
            int p = o.getPosition();
            o.setPosition(row.getPosition());
            row.setPosition(p);
            links.save(o);
            links.save(row);
        }
        return dtos(ordered(storeId));
    }

    /// Removes a link and the links under it.
    @Transactional(rollbackFor = IOException.class)
    public List<MenuLinkDto> remove(String username, String storeId, String linkId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        MenuLink row = link(storeId, linkId);
        List<MenuLink> all = links.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (row.getId().equals(all.get(iter).getParentId())) {
                links.delete(all.get(iter));
                translations.forget(storeId, com.codenameone.examples.bazaar.market.Translations.MENU_LINK, all.get(iter).getId());
            }
        }
        links.delete(row);
        translations.forget(storeId, com.codenameone.examples.bazaar.market.Translations.MENU_LINK, row.getId());
        return dtos(ordered(storeId));
    }

    private MenuLink link(String storeId, String linkId) throws IOException {
        MenuLink l = linkId == null || linkId.length() > 32 ? null : links.findById(linkId);
        if (l == null || !storeId.equals(l.getStoreId())) {
            throw new ResponseStatusException(404, "No such link");
        }
        return l;
    }

    /// A link's address: a path on this site, or an `https://` or `http://` one.
    static String url(String in) {
        String u = in == null ? "" : in.trim();
        String path = Lists.sitePath(u);
        if (path != null) {
            return path;
        }
        String safe = RichText.safeHref(u);
        if (safe != null && safe.equals(u) && (u.regionMatches(true, 0, "https://", 0, 8)
                || u.regionMatches(true, 0, "http://", 0, 7)) && u.length() <= 500) {
            return u;
        }
        throw new ResponseStatusException(400, "A link goes to a path like /s/shop/pages/about "
                + "or an https:// address");
    }

    private static List<MenuLinkDto> dtos(List<MenuLink> rows) {
        List<MenuLinkDto> out = new ArrayList<MenuLinkDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            MenuLink l = rows.get(iter);
            out.add(new MenuLinkDto(l.getId(), l.getMenu(), l.getParentId(), l.getTitle(),
                    l.getUrl(), l.getPosition()));
        }
        return out;
    }

    // ============================================================= redirects

    @Transactional(readOnly = true)
    public List<RedirectDto> redirects(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return redirectDtos(storeId);
    }

    /// Adds a redirect from `fromPath` -- under `/s/{handle}/` of this store
    /// -- to a path on the site.
    @Transactional(rollbackFor = IOException.class)
    public List<RedirectDto> addRedirect(String username, String storeId,
            RedirectDto in) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say what to redirect");
        }
        String from = Lists.sitePath(in.fromPath());
        String prefix = "/s/" + stores.findById(storeId).getHandle() + "/";
        if (from == null || !from.startsWith(prefix) || from.length() == prefix.length()
                || from.length() > 190 || from.indexOf('?') >= 0 || from.indexOf('#') >= 0) {
            throw new ResponseStatusException(400, "Redirect an address of this store, like "
                    + prefix + "old-page");
        }
        String to = Lists.sitePath(in.toPath());
        if (to == null) {
            throw new ResponseStatusException(400, "Redirect to a path on this site");
        }
        if (to.equals(from) || redirects.findByStoreIdAndFromPath(storeId, path(to)) != null) {
            throw new ResponseStatusException(400, "That would redirect in a loop or a chain");
        }
        List<UrlRedirect> all = redirects.findByStoreIdOrderByFromPathAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (path(all.get(iter).getToPath()).equals(from)) {
                throw new ResponseStatusException(400, "Another redirect leads here; point it at "
                        + "the new address instead");
            }
        }
        UrlRedirect row = redirects.findByStoreIdAndFromPath(storeId, from);
        boolean fresh = row == null;
        if (fresh) {
            row = new UrlRedirect();
            row.setId(Ids.next());
            row.setStoreId(storeId);
            row.setFromPath(from);
            row.setCreatedAt(Instant.now());
        }
        row.setToPath(to);
        if (fresh) {
            redirects.insert(row);
        } else {
            redirects.save(row);
        }
        return redirectDtos(storeId);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<RedirectDto> removeRedirect(String username, String storeId, String redirectId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        UrlRedirect r = redirectId == null || redirectId.length() > 32 ? null
                : redirects.findById(redirectId);
        if (r == null || !storeId.equals(r.getStoreId())) {
            throw new ResponseStatusException(404, "No such redirect");
        }
        redirects.delete(r);
        return redirectDtos(storeId);
    }

    /// Where `path` (no query) of `storeId` moved, counting the visit, or null.
    @Transactional(rollbackFor = IOException.class)
    public String follow(String storeId, String path) throws IOException {
        if (path == null || path.length() > 190) {
            return null;
        }
        UrlRedirect r = redirects.findByStoreIdAndFromPath(storeId, path);
        if (r == null) {
            return null;
        }
        r.setHits(r.getHits() + 1);
        redirects.save(r);
        return r.getToPath();
    }

    private static String path(String to) {
        int q = to.indexOf('?');
        int h = to.indexOf('#');
        int end = q < 0 ? h : h < 0 ? q : Math.min(q, h);
        return end < 0 ? to : to.substring(0, end);
    }

    private List<RedirectDto> redirectDtos(String storeId) throws IOException {
        List<UrlRedirect> rows = redirects.findByStoreIdOrderByFromPathAsc(storeId);
        List<RedirectDto> out = new ArrayList<RedirectDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            UrlRedirect r = rows.get(iter);
            out.add(new RedirectDto(r.getId(), r.getFromPath(), r.getToPath(), r.getHits()));
        }
        return out;
    }
}
