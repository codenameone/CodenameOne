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
import com.codename1.annotations.rest.PUT;
import com.codename1.annotations.rest.Path;
import com.codename1.annotations.rest.Query;
import com.codename1.annotations.rest.RestClient;
import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

import java.util.List;

/// The catalog: products, their options, variants and pictures, and
/// collections.
///
/// `/api/shop` needs no sign-in and shows only ACTIVE products of ACTIVE
/// stores. `/api/merchant` shows every product of a store to its staff, and
/// lets an OWNER or MANAGER change them.
///
/// Search parameters, all optional: `q` (words in the title, vendor, type or
/// tags), `collection` (handle), `tag`, `type`, `vendor`, `min` and `max`
/// (cents), `sort` (`relevance`, `newest`, `price`, `-price`, `title`,
/// `rating`), `page` (from 0), `size` (1 to 100, 24 when absent) and
/// `category` (a taxonomy id; a top-level one takes those under it).
@RestClient
public interface CatalogApi {
    @GET("/api/shop/stores/{handle}/products")
    void products(@Path("handle") String handle, @Query("q") String q,
            @Query("collection") String collection, @Query("tag") String tag,
            @Query("type") String type, @Query("vendor") String vendor,
            @Query("min") String min, @Query("max") String max, @Query("sort") String sort,
            @Query("page") String page, @Query("size") String size,
            @Query("category") String category,
            OnComplete<Response<ProductPageDto>> callback);

    /// The counts behind the filters, for the same search less its own facet.
    @GET("/api/shop/stores/{handle}/facets")
    void facets(@Path("handle") String handle, @Query("q") String q,
            @Query("collection") String collection, @Query("tag") String tag,
            @Query("type") String type, @Query("vendor") String vendor,
            @Query("min") String min, @Query("max") String max,
            @Query("category") String category,
            OnComplete<Response<FacetsDto>> callback);

    /// Published collections, each with the number of active products in it.
    @GET("/api/shop/stores/{handle}/collections")
    void collections(@Path("handle") String handle,
            OnComplete<Response<List<CollectionDto>>> callback);

    /// Predictive search: up to eight products and collections whose titles
    /// start with the words typed, best first; empty for a store behind its
    /// pre-launch password. The same answers as the web's `/s/{handle}/suggest`.
    @GET("/api/shop/stores/{handle}/suggest")
    void suggest(@Path("handle") String handle, @Query("q") String q,
            OnComplete<Response<List<SearchSuggestionDto>>> callback);

    /// A product by id.
    @GET("/api/shop/products/{id}")
    void product(@Path("id") String id, OnComplete<Response<ProductDto>> callback);

    /// "Frequently bought together": up to four products bought in the same
    /// paid orders as this one, then others from its collections.
    @GET("/api/shop/products/{id}/together")
    void together(@Path("id") String id, OnComplete<Response<List<ProductCardDto>>> callback);

    @GET("/api/shop/stores/{handle}/products/{productHandle}")
    void productByHandle(@Path("handle") String handle,
            @Path("productHandle") String productHandle,
            OnComplete<Response<ProductDto>> callback);

    /// Every product of the store, drafts and archived included; `status`
    /// narrows it. The same search parameters as the public list.
    @GET("/api/merchant/stores/{storeId}/products")
    void merchantProducts(@Path("storeId") String storeId, @Query("status") String status,
            @Query("q") String q, @Query("collection") String collection,
            @Query("tag") String tag, @Query("type") String type,
            @Query("vendor") String vendor, @Query("sort") String sort,
            @Query("page") String page, @Query("size") String size,
            OnComplete<Response<ProductPageDto>> callback);

    @GET("/api/merchant/stores/{storeId}/products/{productId}")
    void merchantProduct(@Path("storeId") String storeId, @Path("productId") String productId,
            OnComplete<Response<ProductDto>> callback);

    @POST("/api/merchant/stores/{storeId}/products")
    void createProduct(@Path("storeId") String storeId, @Body ProductDto product,
            OnComplete<Response<ProductDto>> callback);

    @PUT("/api/merchant/stores/{storeId}/products/{productId}")
    void updateProduct(@Path("storeId") String storeId, @Path("productId") String productId,
            @Body ProductDto product, OnComplete<Response<ProductDto>> callback);

    @POST("/api/merchant/stores/{storeId}/products/{productId}/archive")
    void archiveProduct(@Path("storeId") String storeId, @Path("productId") String productId,
            OnComplete<Response<ProductDto>> callback);

    /// A DRAFT copy, with new SKUs and no barcodes or stock.
    @POST("/api/merchant/stores/{storeId}/products/{productId}/duplicate")
    void duplicateProduct(@Path("storeId") String storeId, @Path("productId") String productId,
            OnComplete<Response<ProductDto>> callback);

    @POST("/api/merchant/stores/{storeId}/products/bulk")
    void bulkEdit(@Path("storeId") String storeId, @Body BulkEditDto edit,
            OnComplete<Response<CountDto>> callback);

    /// Stores a picture and adds it to the end of the product's gallery; the
    /// first one becomes the product's main photo.
    @POST("/api/merchant/stores/{storeId}/products/{productId}/photos")
    void addPhoto(@Path("storeId") String storeId, @Path("productId") String productId,
            @Body PhotoDto photo, OnComplete<Response<ProductDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/products/{productId}/photos/{photoId}")
    void removePhoto(@Path("storeId") String storeId, @Path("productId") String productId,
            @Path("photoId") String photoId, OnComplete<Response<ProductDto>> callback);

    /// A stored picture by id: product, collection or store logo.
    @GET("/api/shop/photos/{photoId}")
    void photo(@Path("photoId") String photoId, OnComplete<Response<PhotoDto>> callback);

    /// Every collection, published or not.
    @GET("/api/merchant/stores/{storeId}/collections")
    void merchantCollections(@Path("storeId") String storeId,
            OnComplete<Response<List<CollectionDto>>> callback);

    @POST("/api/merchant/stores/{storeId}/collections")
    void createCollection(@Path("storeId") String storeId, @Body CollectionDto collection,
            OnComplete<Response<CollectionDto>> callback);

    @PUT("/api/merchant/stores/{storeId}/collections/{collectionId}")
    void updateCollection(@Path("storeId") String storeId,
            @Path("collectionId") String collectionId, @Body CollectionDto collection,
            OnComplete<Response<CollectionDto>> callback);

    @DELETE("/api/merchant/stores/{storeId}/collections/{collectionId}")
    void deleteCollection(@Path("storeId") String storeId,
            @Path("collectionId") String collectionId,
            OnComplete<Response<CollectionDto>> callback);

    /// Whether a product is a bundle, and its components.
    @GET("/api/merchant/stores/{storeId}/products/{productId}/bundle")
    void bundle(@Path("storeId") String storeId, @Path("productId") String productId,
            OnComplete<Response<BundleDto>> callback);

    /// Makes a product a bundle of the components sent, or a standard product
    /// again. Refused while an open order holds a bundle not yet shipped.
    @PUT("/api/merchant/stores/{storeId}/products/{productId}/bundle")
    void setBundle(@Path("storeId") String storeId, @Path("productId") String productId,
            @Body BundleDto bundle, OnComplete<Response<BundleDto>> callback);

    /// Categories of the product taxonomy whose path has every word of `q`
    /// (or the one category whose id `q` is), each as its path and id
    /// ([NameCountDto] `name` and `count`); at most 40.
    @GET("/api/shop/taxonomy")
    void taxonomy(@Query("q") String q, OnComplete<Response<List<NameCountDto>>> callback);

    /// A product's publishing window on every channel the store sells on; a
    /// channel with no window answers 0 for both bounds.
    @GET("/api/merchant/stores/{storeId}/products/{productId}/schedule")
    void schedule(@Path("storeId") String storeId, @Path("productId") String productId,
            OnComplete<Response<List<ChannelScheduleDto>>> callback);

    /// Sets (or, with both bounds 0, removes) the window on one channel.
    @PUT("/api/merchant/stores/{storeId}/products/{productId}/schedule")
    void setSchedule(@Path("storeId") String storeId, @Path("productId") String productId,
            @Body ChannelScheduleDto window,
            OnComplete<Response<List<ChannelScheduleDto>>> callback);
}
