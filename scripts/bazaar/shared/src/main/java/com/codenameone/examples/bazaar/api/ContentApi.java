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
package com.codenameone.examples.bazaar.api;

import com.codename1.annotations.rest.Body;
import com.codename1.annotations.rest.DELETE;
import com.codename1.annotations.rest.GET;
import com.codename1.annotations.rest.POST;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import java.util.List;

/// The online store's content: the theme, pages and policies, the blog, navigation menus,
/// URL redirects, and merchant-defined metafields and metaobjects.
@RestClient
public interface ContentApi {
    @GET("/api/merchant/stores/{storeId}/theme")
    void theme(@Path("storeId") String storeId,
            @Query("status") String status,
            OnComplete<Response<ThemeDto>> callback);

    @POST("/api/merchant/stores/{storeId}/theme")
    void saveTheme(@Path("storeId") String storeId,
            @Body ThemeDto body,
            OnComplete<Response<ThemeDto>> callback);

    @POST("/api/merchant/stores/{storeId}/theme/publish")
    void publishTheme(@Path("storeId") String storeId,
            OnComplete<Response<ThemeDto>> callback);

    /// The live theme's home page blocks, for the app's store home.
    @GET("/api/shop/stores/{handle}/blocks")
    void blocks(@Path("handle") String handle,
            OnComplete<Response<List<BlockDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/pages")
    void contentPages(@Path("storeId") String storeId,
            OnComplete<Response<List<ContentPageDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/pages")
    void createContentPage(@Path("storeId") String storeId,
            @Body ContentPageDto body,
            OnComplete<Response<ContentPageDto>> callback);

    @POST("/api/merchant/stores/{storeId}/pages/{pageId}")
    void saveContentPage(@Path("storeId") String storeId,
            @Path("pageId") String pageId,
            @Body ContentPageDto body,
            OnComplete<Response<ContentPageDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/pages/{pageId}")
    void removeContentPage(@Path("storeId") String storeId,
            @Path("pageId") String pageId,
            OnComplete<Response<List<ContentPageDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/blog-posts")
    void blogPosts(@Path("storeId") String storeId,
            OnComplete<Response<List<BlogPostDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/blog-posts")
    void createBlogPost(@Path("storeId") String storeId,
            @Body BlogPostDto body,
            OnComplete<Response<BlogPostDto>> callback);

    @POST("/api/merchant/stores/{storeId}/blog-posts/{postId}")
    void saveBlogPost(@Path("storeId") String storeId,
            @Path("postId") String postId,
            @Body BlogPostDto body,
            OnComplete<Response<BlogPostDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/blog-posts/{postId}")
    void removeBlogPost(@Path("storeId") String storeId,
            @Path("postId") String postId,
            OnComplete<Response<List<BlogPostDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/menu-links")
    void menuLinks(@Path("storeId") String storeId,
            OnComplete<Response<List<MenuLinkDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/menu-links")
    void addMenuLink(@Path("storeId") String storeId,
            @Body MenuLinkDto body,
            OnComplete<Response<List<MenuLinkDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/menu-links/{linkId}/move")
    void moveMenuLink(@Path("storeId") String storeId,
            @Path("linkId") String linkId,
            @Query("direction") String direction,
            OnComplete<Response<List<MenuLinkDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/menu-links/{linkId}")
    void removeMenuLink(@Path("storeId") String storeId,
            @Path("linkId") String linkId,
            OnComplete<Response<List<MenuLinkDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/redirects")
    void redirects(@Path("storeId") String storeId,
            OnComplete<Response<List<RedirectDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/redirects")
    void addRedirect(@Path("storeId") String storeId,
            @Body RedirectDto body,
            OnComplete<Response<List<RedirectDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/redirects/{redirectId}")
    void removeRedirect(@Path("storeId") String storeId,
            @Path("redirectId") String redirectId,
            OnComplete<Response<List<RedirectDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/metafields")
    void metafields(@Path("storeId") String storeId,
            OnComplete<Response<List<MetafieldDefDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/metafields")
    void addMetafield(@Path("storeId") String storeId,
            @Body MetafieldDefDto body,
            OnComplete<Response<List<MetafieldDefDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/metafields/{fieldId}")
    void removeMetafield(@Path("storeId") String storeId,
            @Path("fieldId") String fieldId,
            OnComplete<Response<List<MetafieldDefDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/metafield-values")
    void metafieldValues(@Path("storeId") String storeId,
            @Query("ownerKind") String ownerKind,
            @Query("ownerId") String ownerId,
            OnComplete<Response<List<MetafieldValueDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/metafield-values")
    void putMetafieldValue(@Path("storeId") String storeId,
            @Body MetafieldValueDto body,
            OnComplete<Response<List<MetafieldValueDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/metaobject-types")
    void metaobjectTypes(@Path("storeId") String storeId,
            OnComplete<Response<List<MetaobjectTypeDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/metaobject-types")
    void addMetaobjectType(@Path("storeId") String storeId,
            @Body MetaobjectTypeDto body,
            OnComplete<Response<List<MetaobjectTypeDto>>> callback);

    @GET("/api/merchant/stores/{storeId}/metaobject-types/{typeId}/objects")
    void metaobjects(@Path("storeId") String storeId,
            @Path("typeId") String typeId,
            OnComplete<Response<List<MetaobjectEntryDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/metaobject-types/{typeId}/objects")
    void addMetaobject(@Path("storeId") String storeId,
            @Path("typeId") String typeId,
            @Body MetaobjectEntryDto body,
            OnComplete<Response<List<MetaobjectEntryDto>>> callback);

    @DELETE("/api/merchant/stores/{storeId}/metaobjects/{objectId}")
    void removeMetaobject(@Path("storeId") String storeId,
            @Path("objectId") String objectId,
            OnComplete<Response<List<MetaobjectEntryDto>>> callback);
}
