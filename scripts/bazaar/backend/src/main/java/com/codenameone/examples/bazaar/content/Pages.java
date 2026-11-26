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
import com.codenameone.examples.bazaar.api.BlogPostDto;
import com.codenameone.examples.bazaar.api.ContentPageDto;
import com.codenameone.examples.bazaar.domain.BlogPost;
import com.codenameone.examples.bazaar.domain.ContentPage;
import com.codenameone.examples.bazaar.repository.BlogPostRepository;
import com.codenameone.examples.bazaar.repository.ContentPageRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// A store's pages, policies and blog.
///
/// A page (`PAGE`) has a handle of the merchant's; a policy (`POLICY`) is one
/// of [#POLICIES] -- refund, privacy, terms, shipping -- and the storefront's
/// footer links each one published. Bodies are rich text, sanitized when they
/// are saved ([RichText]), so what is stored is already clean; their titles
/// and bodies are translated through `Translations` (`CONTENT` and `POST`).
/// Managers write; anyone on the staff reads. A draft is never on the
/// storefront, in the sitemap or in the feed.
@Component
public class Pages {
    public static final String PAGE = "PAGE";
    public static final String POLICY = "POLICY";
    /// The policies a store can publish, in footer order.
    public static final String[] POLICIES = {"refund", "privacy", "terms", "shipping"};
    public static final String[] POLICY_TITLES = {"Refund policy", "Privacy policy",
        "Terms of service", "Shipping policy"};

    private final ContentPageRepository pages;
    private final BlogPostRepository posts;
    private final StoreAccess access;

    public Pages(ContentPageRepository pages, BlogPostRepository posts, StoreAccess access) {
        this.pages = pages;
        this.posts = posts;
        this.access = access;
    }

    // ================================================================= pages

    @Transactional(readOnly = true)
    public List<ContentPageDto> list(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<ContentPage> rows = pages.findByStoreIdOrderByTitleAsc(storeId);
        List<ContentPageDto> out = new ArrayList<ContentPageDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ContentPageDto detail(String username, String storeId, String pageId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(page(storeId, pageId));
    }

    /// Creates (`pageId` null) or replaces a page or policy.
    @Transactional(rollbackFor = IOException.class)
    public ContentPageDto save(String username, String storeId, String pageId, ContentPageDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say what the page is");
        }
        ContentPage row = pageId == null ? null : page(storeId, pageId);
        String kind = row != null ? row.getKind() : POLICY.equals(in.kind()) ? POLICY : PAGE;
        String title = Lists.cut(in.title(), 200);
        String handle;
        if (POLICY.equals(kind)) {
            handle = row != null ? row.getHandle() : policy(in.handle());
            if (title.length() == 0) {
                title = POLICY_TITLES[indexOf(POLICIES, handle)];
            }
        } else {
            if (title.length() == 0) {
                throw new ResponseStatusException(400, "Give the page a title");
            }
            handle = Lists.handle(in.handle(), title, 80);
        }
        ContentPage clash = pages.findByStoreIdAndKindAndHandle(storeId, kind, handle);
        if (clash != null && (row == null || !clash.getId().equals(row.getId()))) {
            throw new ResponseStatusException(409, POLICY.equals(kind)
                    ? "That policy exists already" : "Another page has that handle");
        }
        boolean fresh = row == null;
        if (fresh) {
            row = new ContentPage();
            row.setId(Ids.next());
            row.setStoreId(storeId);
            row.setKind(kind);
        }
        row.setHandle(handle);
        row.setTitle(title);
        row.setBody(RichText.sanitize(in.body()));
        row.setPublished(in.published());
        row.setUpdatedAt(Instant.now());
        if (fresh) {
            pages.insert(row);
        } else {
            pages.save(row);
        }
        return dto(row);
    }

    @Transactional(rollbackFor = IOException.class)
    public void remove(String username, String storeId, String pageId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        pages.delete(page(storeId, pageId));
    }

    /// A published page or policy of the storefront, or 404.
    @Transactional(readOnly = true)
    public ContentPage published(String storeId, String kind, String handle) throws IOException {
        ContentPage p = handle == null || handle.length() > 80 ? null
                : pages.findByStoreIdAndKindAndHandle(storeId, kind, handle);
        if (p == null || !p.isPublished()) {
            throw new ResponseStatusException(404, "No such page");
        }
        return p;
    }

    /// Every published page and policy of a store.
    @Transactional(readOnly = true)
    public List<ContentPage> publishedAll(String storeId) throws IOException {
        List<ContentPage> rows = pages.findByStoreIdOrderByTitleAsc(storeId);
        List<ContentPage> out = new ArrayList<ContentPage>();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).isPublished()) {
                out.add(rows.get(iter));
            }
        }
        return out;
    }

    /// The published policies in [#POLICIES] order.
    @Transactional(readOnly = true)
    public List<ContentPage> publishedPolicies(String storeId) throws IOException {
        List<ContentPage> all = publishedAll(storeId);
        List<ContentPage> out = new ArrayList<ContentPage>();
        for (int p = 0; p < POLICIES.length; p++) {
            for (int iter = 0; iter < all.size(); iter++) {
                ContentPage c = all.get(iter);
                if (POLICY.equals(c.getKind()) && POLICIES[p].equals(c.getHandle())) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    /// A page of `storeId` by id, or 404 -- another store's id included.
    @Transactional(readOnly = true)
    public ContentPage page(String storeId, String pageId) throws IOException {
        ContentPage p = pageId == null || pageId.length() > 32 ? null : pages.findById(pageId);
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such page");
        }
        return p;
    }

    static String policy(String handle) {
        String h = handle == null ? "" : handle.trim();
        if (indexOf(POLICIES, h) < 0) {
            throw new ResponseStatusException(400, "A policy is refund, privacy, terms or shipping");
        }
        return h;
    }

    static int indexOf(String[] all, String s) {
        for (int iter = 0; iter < all.length; iter++) {
            if (all[iter].equals(s)) {
                return iter;
            }
        }
        return -1;
    }

    static ContentPageDto dto(ContentPage p) {
        return new ContentPageDto(p.getId(), p.getKind(), p.getHandle(), p.getTitle(), p.getBody(),
                p.isPublished(), p.getUpdatedAt() == null ? 0 : p.getUpdatedAt().toEpochMilli());
    }

    // ================================================================== blog

    @Transactional(readOnly = true)
    public List<BlogPostDto> posts(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<BlogPost> rows = posts.findByStoreIdOrderByPublishedAtDesc(storeId);
        List<BlogPostDto> out = new ArrayList<BlogPostDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public BlogPostDto postDetail(String username, String storeId, String postId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return dto(post(storeId, postId));
    }

    /// Creates (`postId` null) or replaces a post; publishing it the first
    /// time stamps when.
    @Transactional(rollbackFor = IOException.class)
    public BlogPostDto savePost(String username, String storeId, String postId, BlogPostDto in)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (in == null) {
            throw new ResponseStatusException(400, "Say what the post is");
        }
        String title = Lists.cut(in.title(), 200);
        if (title.length() == 0) {
            throw new ResponseStatusException(400, "Give the post a title");
        }
        BlogPost row = postId == null ? null : post(storeId, postId);
        String handle = Lists.handle(in.handle(), title, 80);
        BlogPost clash = posts.findByStoreIdAndHandle(storeId, handle);
        if (clash != null && (row == null || !clash.getId().equals(row.getId()))) {
            throw new ResponseStatusException(409, "Another post has that handle");
        }
        boolean fresh = row == null;
        if (fresh) {
            row = new BlogPost();
            row.setId(Ids.next());
            row.setStoreId(storeId);
        }
        row.setHandle(handle);
        row.setTitle(title);
        row.setAuthor(Lists.cut(in.author(), 120));
        List<String> tags = new ArrayList<String>();
        for (int iter = 0; in.tags() != null && iter < in.tags().size() && iter < 20; iter++) {
            String t = Lists.cut(in.tags().get(iter), 40);
            StringBuilder clean = new StringBuilder();
            for (int c = 0; c < t.length(); c++) {
                char ch = t.charAt(c);
                if (ch != ',' && ch >= ' ') {
                    clean.append(ch);
                }
            }
            String tag = clean.toString().trim();
            if (tag.length() > 0 && !tags.contains(tag)) {
                tags.add(tag);
            }
        }
        row.setTags(Lists.cut(Lists.join(tags), 400));
        String body = RichText.sanitize(in.body());
        row.setBody(body);
        String excerpt = Lists.cut(in.excerpt(), 500);
        if (excerpt.length() == 0) {
            excerpt = Lists.cut(RichText.plainText(body), 300);
        }
        row.setExcerpt(RichText.collapse(excerpt));
        if (in.published() && !row.isPublished()) {
            row.setPublishedAt(Instant.now());
        }
        row.setPublished(in.published());
        row.setUpdatedAt(Instant.now());
        if (fresh) {
            posts.insert(row);
        } else {
            posts.save(row);
        }
        return dto(row);
    }

    @Transactional(rollbackFor = IOException.class)
    public void removePost(String username, String storeId, String postId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        posts.delete(post(storeId, postId));
    }

    /// The published posts, newest first, optionally only those tagged `tag`.
    @Transactional(readOnly = true)
    public List<BlogPost> publishedPosts(String storeId, String tag) throws IOException {
        List<BlogPost> rows = posts.findByStoreIdOrderByPublishedAtDesc(storeId);
        List<BlogPost> out = new ArrayList<BlogPost>();
        for (int iter = 0; iter < rows.size(); iter++) {
            BlogPost p = rows.get(iter);
            if (p.isPublished() && (tag == null || tag.length() == 0
                    || Lists.split(p.getTags()).contains(tag))) {
                out.add(p);
            }
        }
        return out;
    }

    /// A published post by handle, or 404.
    @Transactional(readOnly = true)
    public BlogPost publishedPost(String storeId, String handle) throws IOException {
        BlogPost p = handle == null || handle.length() > 80 ? null
                : posts.findByStoreIdAndHandle(storeId, handle);
        if (p == null || !p.isPublished()) {
            throw new ResponseStatusException(404, "No such post");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public BlogPost post(String storeId, String postId) throws IOException {
        BlogPost p = postId == null || postId.length() > 32 ? null : posts.findById(postId);
        if (p == null || !storeId.equals(p.getStoreId())) {
            throw new ResponseStatusException(404, "No such post");
        }
        return p;
    }

    static BlogPostDto dto(BlogPost p) {
        return new BlogPostDto(p.getId(), p.getHandle(), p.getTitle(), p.getAuthor(),
                Lists.split(p.getTags()), p.getExcerpt(), p.getBody(), p.isPublished(),
                p.getPublishedAt() == null ? 0 : p.getPublishedAt().toEpochMilli());
    }
}
