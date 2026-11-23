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

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.BlogPostDto;
import com.codenameone.examples.bazaar.api.ContentApiServer;
import com.codenameone.examples.bazaar.api.ContentPageDto;
import com.codenameone.examples.bazaar.api.MenuLinkDto;
import com.codenameone.examples.bazaar.api.MetafieldDefDto;
import com.codenameone.examples.bazaar.api.MetafieldValueDto;
import com.codenameone.examples.bazaar.api.MetaobjectEntryDto;
import com.codenameone.examples.bazaar.api.MetaobjectTypeDto;
import com.codenameone.examples.bazaar.api.RedirectDto;
import com.codenameone.examples.bazaar.api.ThemeDto;
import java.util.List;

/// The server's half of `ContentApi`.
@Component
public class ContentEndpoint implements ContentApiServer {
    private final Themes themes;
    private final Pages pages;
    private final Navigation navigation;
    private final Metafields metafields;

    public ContentEndpoint(Themes themes, Pages pages, Navigation navigation,
            Metafields metafields) {
        this.themes = themes;
        this.pages = pages;
        this.navigation = navigation;
        this.metafields = metafields;
    }

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.store.Stores stores;

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.repository.ProductRepository products;

    /// The live home page blocks of an active store, each product block carrying the
    /// product's id; a product that is gone or not active drops its block.
    @Override
    public List<com.codenameone.examples.bazaar.api.BlockDto> blocks(String handle)
            throws Exception {
        com.codenameone.examples.bazaar.domain.Store store = stores.activeStore(handle);
        List<com.codenameone.examples.bazaar.api.BlockDto> in =
                Themes.blockList(themes.liveOf(store.getId()));
        List<com.codenameone.examples.bazaar.api.BlockDto> out =
                new java.util.ArrayList<com.codenameone.examples.bazaar.api.BlockDto>();
        for (int iter = 0; iter < in.size(); iter++) {
            com.codenameone.examples.bazaar.api.BlockDto b = in.get(iter);
            if ("product".equals(b.kind())) {
                com.codenameone.examples.bazaar.domain.Product p =
                        products.findByStoreIdAndHandle(store.getId(), b.ref());
                if (p == null || !"ACTIVE".equals(p.getStatus())) {
                    continue;
                }
                b = new com.codenameone.examples.bazaar.api.BlockDto(b.kind(),
                        b.text().length() > 0 ? b.text() : p.getTitle(), b.ref(), p.getId());
            }
            out.add(b);
        }
        return out;
    }

    @Override
    public ThemeDto theme(String storeId, String status) throws Exception {
        return themes.get(Caller.name(), storeId, status);
    }

    @Override
    public ThemeDto saveTheme(String storeId, ThemeDto body) throws Exception {
        return themes.saveDraft(Caller.name(), storeId, body);
    }

    @Override
    public ThemeDto publishTheme(String storeId) throws Exception {
        return themes.publish(Caller.name(), storeId);
    }

    @Override
    public List<ContentPageDto> contentPages(String storeId) throws Exception {
        return pages.list(Caller.name(), storeId);
    }

    @Override
    public ContentPageDto createContentPage(String storeId, ContentPageDto body) throws Exception {
        return pages.save(Caller.name(), storeId, null, body);
    }

    @Override
    public ContentPageDto saveContentPage(String storeId, String pageId, ContentPageDto body)
            throws Exception {
        return pages.save(Caller.name(), storeId, pageId, body);
    }

    @Override
    public List<ContentPageDto> removeContentPage(String storeId, String pageId) throws Exception {
        pages.remove(Caller.name(), storeId, pageId);
        return pages.list(Caller.name(), storeId);
    }

    @Override
    public List<BlogPostDto> blogPosts(String storeId) throws Exception {
        return pages.posts(Caller.name(), storeId);
    }

    @Override
    public BlogPostDto createBlogPost(String storeId, BlogPostDto body) throws Exception {
        return pages.savePost(Caller.name(), storeId, null, body);
    }

    @Override
    public BlogPostDto saveBlogPost(String storeId, String postId, BlogPostDto body)
            throws Exception {
        return pages.savePost(Caller.name(), storeId, postId, body);
    }

    @Override
    public List<BlogPostDto> removeBlogPost(String storeId, String postId) throws Exception {
        pages.removePost(Caller.name(), storeId, postId);
        return pages.posts(Caller.name(), storeId);
    }

    @Override
    public List<MenuLinkDto> menuLinks(String storeId) throws Exception {
        return navigation.links(Caller.name(), storeId);
    }

    @Override
    public List<MenuLinkDto> addMenuLink(String storeId, MenuLinkDto body) throws Exception {
        return navigation.add(Caller.name(), storeId, body);
    }

    @Override
    public List<MenuLinkDto> moveMenuLink(String storeId, String linkId, String direction)
            throws Exception {
        return navigation.move(Caller.name(), storeId, linkId, "up".equals(direction));
    }

    @Override
    public List<MenuLinkDto> removeMenuLink(String storeId, String linkId) throws Exception {
        return navigation.remove(Caller.name(), storeId, linkId);
    }

    @Override
    public List<RedirectDto> redirects(String storeId) throws Exception {
        return navigation.redirects(Caller.name(), storeId);
    }

    @Override
    public List<RedirectDto> addRedirect(String storeId, RedirectDto body) throws Exception {
        return navigation.addRedirect(Caller.name(), storeId, body);
    }

    @Override
    public List<RedirectDto> removeRedirect(String storeId, String redirectId) throws Exception {
        return navigation.removeRedirect(Caller.name(), storeId, redirectId);
    }

    @Override
    public List<MetafieldDefDto> metafields(String storeId) throws Exception {
        return metafields.definitions(Caller.name(), storeId);
    }

    @Override
    public List<MetafieldDefDto> addMetafield(String storeId, MetafieldDefDto body)
            throws Exception {
        metafields.define(Caller.name(), storeId, body);
        return metafields.definitions(Caller.name(), storeId);
    }

    @Override
    public List<MetafieldDefDto> removeMetafield(String storeId, String fieldId) throws Exception {
        metafields.undefine(Caller.name(), storeId, fieldId);
        return metafields.definitions(Caller.name(), storeId);
    }

    @Override
    public List<MetafieldValueDto> metafieldValues(String storeId, String ownerKind,
            String ownerId) throws Exception {
        return metafields.valuesOf(Caller.name(), storeId, ownerKind, ownerId);
    }

    @Override
    public List<MetafieldValueDto> putMetafieldValue(String storeId, MetafieldValueDto body)
            throws Exception {
        return metafields.put(Caller.name(), storeId, body);
    }

    @Override
    public List<MetaobjectTypeDto> metaobjectTypes(String storeId) throws Exception {
        return metafields.types(Caller.name(), storeId);
    }

    @Override
    public List<MetaobjectTypeDto> addMetaobjectType(String storeId, MetaobjectTypeDto body)
            throws Exception {
        metafields.addType(Caller.name(), storeId, body);
        return metafields.types(Caller.name(), storeId);
    }

    @Override
    public List<MetaobjectEntryDto> metaobjects(String storeId, String typeId) throws Exception {
        return metafields.entries(Caller.name(), storeId, typeId);
    }

    @Override
    public List<MetaobjectEntryDto> addMetaobject(String storeId, String typeId,
            MetaobjectEntryDto body) throws Exception {
        return metafields.addEntry(Caller.name(), storeId, typeId, body);
    }

    @Override
    public List<MetaobjectEntryDto> removeMetaobject(String storeId, String objectId)
            throws Exception {
        String typeId = metafields.removeEntry(Caller.name(), storeId, objectId);
        return metafields.entries(Caller.name(), storeId, typeId);
    }
}
